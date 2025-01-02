package com.grindrplus.hooks

import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.GrindrPlus.instanceManager
import com.grindrplus.core.Config
import com.grindrplus.core.DatabaseHelper
import com.grindrplus.core.Utils.sendNotification
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.getObjectField
import org.json.JSONObject

class AntiBlock : Hook(
    "Anti Block",
    "Notifies you when someone blocks or unblocks you"
) {
    private var myProfileId: Long = 0
    private val inboxFragmentV2DeleteConversations = "na.b0\$a"
    override fun init() {
        findClass(inboxFragmentV2DeleteConversations)
            .hook("invokeSuspend", HookStage.BEFORE) { param ->
                GrindrPlus.logger.log(Thread.currentThread().stackTrace.joinToString("\n"))
                GrindrPlus.shouldTriggerAntiblock = false
        }

        findClass(inboxFragmentV2DeleteConversations)
            .hook("invokeSuspend", HookStage.AFTER) { param ->
            val numberOfChatsToDelete = (getObjectField(param.thisObject(), "j") as List<*>).size
            Thread.sleep((300 * numberOfChatsToDelete).toLong()) // FIXME
            GrindrPlus.shouldTriggerAntiblock = true
        }

        findClass("com.grindrapp.android.chat.model.ConversationDeleteNotification")
            .hookConstructor(HookStage.BEFORE) { param ->
                if (!GrindrPlus.shouldTriggerAntiblock) {
                    param.setArg(0, emptyList<String>())
                    return@hookConstructor
                }
            }

        findClass("com.grindrapp.android.chat.model.ConversationDeleteNotification")
            .hookConstructor(HookStage.AFTER) { param ->
                if (!GrindrPlus.shouldTriggerAntiblock) {
                    return@hookConstructor
                }

                if (myProfileId == 0L) {
                    myProfileId = (getObjectField(instanceManager
                        .getInstance("com.grindrapp.android.storage.b"),
                        "p") as String).toLong()
                }
                val profiles = param.args().firstOrNull() as? List<String> ?: emptyList()
                val conversationId = profiles.joinToString(",")
                val conversationIds = conversationId.split(":").mapNotNull { it.toLongOrNull() }
                val otherProfileId = conversationIds.firstOrNull { it != myProfileId } ?: return@hookConstructor

                // FIXME: Get rid of this ugly shit
                if (otherProfileId == myProfileId) return@hookConstructor
                Thread.sleep(300)

                try {
                    if (DatabaseHelper.query(
                            "SELECT * FROM blocks WHERE profileId = ?",
                            arrayOf(otherProfileId.toString())
                        ).isNotEmpty()
                    ) {
                        return@hookConstructor
                    }
                } catch(e: Exception) {
                    GrindrPlus.logger.log("Error checking if user is blocked: ${e.message}")
                }

                try {
                    val response = fetchProfileData(otherProfileId.toString())
                    handleProfileResponse(otherProfileId, conversationId, response)

                } catch (e: Exception) {
                    GrindrPlus.logger.log("Error handling block/unblock request!")
                }
            }
    }

    private fun fetchProfileData(profileId: String): String {
        return try {
            val response = GrindrPlus.httpClient.sendRequest(
                url = "https://grindr.mobi/v4/profiles/$profileId",
                method = "GET"
            )

            if (response.isSuccessful) {
                response.body?.string() ?: "Empty response"
            } else {
                "Error: ${response.code} - ${response.message}"
            }
        } catch (e: Exception) {
            "Error fetching profile: ${e.message}"
        }
    }

    private fun handleProfileResponse(profileId: Long, conversationIds: String, response: String) {
        try {
            val jsonResponse = JSONObject(response)
            val profilesArray = jsonResponse.optJSONArray("profiles")

            if (profilesArray == null || profilesArray.length() == 0) {
                var name = (DatabaseHelper.query(
                    "SELECT name FROM chat_conversations WHERE conversation_id = ?",
                    arrayOf(conversationIds)
                ).firstOrNull()?.get("name") as? String)?.takeIf {
                    name -> name.isNotEmpty() } ?: profileId.toString()
                if (name != profileId.toString()) {
                    name += " ($profileId)"
                }
                if (name == "null") {
                    name = profileId.toString()
                }
                GrindrPlus.logger.log("User $name has blocked you")
                if (Config.get("anti_block_use_toasts", false) as Boolean) {
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "Blocked by $name")
                } else {
                    sendNotification(
                        GrindrPlus.context,
                        "Blocked by User",
                        "You have been blocked by user $name",
                        profileId.toInt()
                    )
                }
            } else {
                val profile = profilesArray.getJSONObject(0)
                val displayName = profile.optString("displayName", profileId.toString())
                    .takeIf { it.isNotEmpty() && it != "null" } ?: profileId.toString()
                GrindrPlus.logger.log("User $profileId (Name: $displayName) unblocked you")
                if (Config.get("anti_block_use_toasts", false) as Boolean) {
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "Unblocked by $displayName")
                } else {
                    sendNotification(
                        GrindrPlus.context,
                        "Unblocked by $displayName",
                        "$displayName has unblocked you.",
                        profileId.toInt(),
                    )
                }
            }
        } catch (e: Exception) {
            GrindrPlus.logger.log("Error handling profile response: ${e.message}")
        }
    }
}
