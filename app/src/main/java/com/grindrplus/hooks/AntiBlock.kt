package com.grindrplus.hooks

import android.os.Build
import androidx.annotation.RequiresApi
import com.grindrplus.GrindrPlus
import com.grindrplus.GrindrPlus.instanceManager
import com.grindrplus.core.DatabaseHelper
import com.grindrplus.core.Utils.sendNotification
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.getObjectField
import org.json.JSONObject

class AntiBlock : Hook(
    "Anti Block",
    "Alerts you when people block you (prevents the chat from clearing)"
) {
    private var myProfileId: Long = 0

    override fun init() {
        findClass("com.grindrapp.android.chat.model.ConversationDeleteNotification")
            .hookConstructor(HookStage.AFTER) { param ->
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

                if (DatabaseHelper.query(
                        "SELECT * FROM blocks WHERE profileId = ?",
                        arrayOf(otherProfileId.toString())
                    ).isNotEmpty()
                ) {
                    return@hookConstructor
                }

                val response = fetchProfileData(otherProfileId.toString())
                handleProfileResponse(otherProfileId, response)
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

    private fun handleProfileResponse(profileId: Long, response: String) {
        try {
            val jsonResponse = JSONObject(response)
            val profilesArray = jsonResponse.optJSONArray("profiles")

            if (profilesArray == null || profilesArray.length() == 0) {
                val name = (DatabaseHelper.query(
                    "SELECT name FROM chat_conversations WHERE conversation_id = ?",
                    arrayOf(profileId.toString())
                ).firstOrNull()?.get("name") as? String)?.takeIf {
                    name -> name.isNotEmpty() } ?: profileId.toString()
                GrindrPlus.logger.log("User $name has blocked you")
                sendNotification(
                    GrindrPlus.context,
                    "Blocked by User",
                    "You have been blocked by user $name",
                    profileId.toInt()
                )
            } else {
                val profile = profilesArray.getJSONObject(0)
                val displayName = profile.optString("displayName", profileId.toString())
                GrindrPlus.logger.log("User $profileId (Name: $displayName) unblocked you")
                sendNotification(
                    GrindrPlus.context,
                    "Unblocked by $displayName",
                    "$displayName has unblocked you.",
                    profileId.toInt(),
                )
            }
        } catch (e: Exception) {
            GrindrPlus.logger.log("Error handling profile response: ${e.message}")
        }
    }
}
