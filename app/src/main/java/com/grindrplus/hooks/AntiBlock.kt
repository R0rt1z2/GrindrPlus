package com.grindrplus.hooks

import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.bridge.BridgeService
import com.grindrplus.core.Config
import com.grindrplus.core.DatabaseHelper
import com.grindrplus.core.Utils.sendNotification
import com.grindrplus.core.Logger
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
    private val chatDeleteConversationPlugin = "q5.c" // search for 'com.grindrapp.android.chat.ChatDeleteConversationPlugin'
    private val inboxFragmentV2DeleteConversations = "t7.i" // search for '("chat_read_receipt", conversationId, null);'

    override fun init() {
        if (Config.get("force_old_anti_block_behavior", false) as Boolean) {
            findClass("com.grindrapp.android.chat.model.ConversationDeleteNotification")
                .hookConstructor(HookStage.BEFORE) { param ->
                    @Suppress("UNCHECKED_CAST")
                    val profiles = param.args().firstOrNull() as? List<String> ?: emptyList()
                    param.setArg(0, emptyList<String>())
                }
        } else {
            findClass(inboxFragmentV2DeleteConversations)
                .hook("d", HookStage.BEFORE) { param ->
                    GrindrPlus.shouldTriggerAntiblock = false
                    GrindrPlus.blockCaller = "inboxFragmentV2DeleteConversations"
                }

            findClass(inboxFragmentV2DeleteConversations)
                .hook("d", HookStage.AFTER) { param ->
                    val numberOfChatsToDelete = (param.args().firstOrNull() as? List<*>)?.size ?: 0
                    if (numberOfChatsToDelete == 0) return@hook
                    // GrindrPlus.logger.debug("Request to delete $numberOfChatsToDelete chats")
                    Thread.sleep((300 * numberOfChatsToDelete).toLong()) // FIXME
                    GrindrPlus.shouldTriggerAntiblock = true
                    GrindrPlus.blockCaller = ""
                }

            findClass(chatDeleteConversationPlugin).hook("b", HookStage.BEFORE) { param ->
                myProfileId = GrindrPlus.myProfileId.toLong()
                if (!GrindrPlus.shouldTriggerAntiblock) {
                    val whitelist = listOf(
                        "inboxFragmentV2DeleteConversations",
                    )
                    if (GrindrPlus.blockCaller !in whitelist) {
                        param.setResult(null)
                    }
                    return@hook
                } else {
                    val conversationId = (param.args().firstOrNull()?.let { getObjectField(it, "payload") } as? JSONObject)
                        ?.optJSONArray("conversationIds")
                        ?.let { (0 until it.length()).joinToString(",") { index -> it.getString(index) } }
                        ?: return@hook

                    val conversationIds = conversationId.split(":").mapNotNull { it.toLongOrNull() }
                    val otherProfileId = conversationIds.firstOrNull { it != myProfileId } ?: return@hook

                    // FIXME: Get rid of this ugly shit
                    if (otherProfileId == myProfileId) return@hook
                    Thread.sleep(300)

                    try {
                        if (DatabaseHelper.query(
                                "SELECT * FROM blocks WHERE profileId = ?",
                                arrayOf(otherProfileId.toString())
                            ).isNotEmpty()
                        ) {
                            return@hook
                        }
                    } catch(e: Exception) {
                        val message = "Error checking if user is blocked: ${e.message}"
                        Logger.apply {
                            e(message)
                            writeRaw(e.stackTraceToString())
                        }
                    }

                    try {
                        val response = fetchProfileData(otherProfileId.toString())
                        handleProfileResponse(otherProfileId, conversationId, response)
                        param.setResult(null)
                    } catch (e: Exception) {
                        val message = "Error handling block/unblock request: ${e.message ?: "Unknown error"}"
                        Logger.apply {
                            e(message)
                            writeRaw(e.stackTraceToString())
                        }
                    }
                }
            }
        }
    }

    private fun fetchProfileData(profileId: String): String {
        val response = GrindrPlus.httpClient.sendRequest(
            url = "https://grindr.mobi/v4/profiles/$profileId",
            method = "GET"
        )

        if (response.isSuccessful) {
            return response.body?.string() ?: "Empty response"
        } else {
            throw Exception("Failed to fetch profile data: ${response.body?.string()}")
        }
    }

    private fun handleProfileResponse(profileId: Long, conversationIds: String, response: String): Boolean {
        try {
            val jsonResponse = JSONObject(response)
            val profilesArray = jsonResponse.optJSONArray("profiles")

            if (profilesArray == null || profilesArray.length() == 0) {
                var displayName = ""
                try {
                    displayName = (DatabaseHelper.query(
                        "SELECT name FROM chat_conversations WHERE conversation_id = ?",
                        arrayOf(conversationIds)
                    ).firstOrNull()?.get("name") as? String)?.takeIf {
                            name -> name.isNotEmpty() } ?: profileId.toString()
                } catch (e: Exception) {
                    Logger.apply {
                        e("Error fetching display name: ${e.message}")
                        writeRaw(e.stackTraceToString())
                    }
                    displayName = profileId.toString()
                }
                displayName = if (displayName == profileId.toString() || displayName == "null")
                { profileId.toString() } else { "$displayName ($profileId)" }
                GrindrPlus.bridgeClient.logBlockEvent(profileId.toString(), displayName, true)
                if (Config.get("anti_block_use_toasts", false) as Boolean) {
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "Blocked by $displayName")
                } else {
                    GrindrPlus.bridgeClient.sendNotificationWithMultipleActions(
                        "Blocked by User",
                        "You have been blocked by user $displayName",
                        10000000 + (profileId % 10000000).toInt(),
                        listOf("Copy ID"),
                        listOf("COPY"),
                        listOf(profileId.toString(), profileId.toString()),
                        BridgeService.CHANNEL_BLOCKS,
                        "Block Notifications",
                        "Notifications when users block you"
                    )
                }
                return true
            } else {
                val profile = profilesArray.getJSONObject(0)
                var displayName = profile.optString("displayName", profileId.toString())
                    .takeIf { it.isNotEmpty() && it != "null" } ?: profileId.toString()
                displayName = if (displayName != profileId.toString()) "$displayName ($profileId)" else displayName
                GrindrPlus.bridgeClient.logBlockEvent(profileId.toString(), displayName, false)
                if (Config.get("anti_block_use_toasts", false) as Boolean) {
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "Unblocked by $displayName")
                } else {
                    GrindrPlus.bridgeClient.sendNotificationWithMultipleActions(
                        "Unblocked by $displayName",
                        "$displayName has unblocked you.",
                        20000000 + (profileId % 10000000).toInt(),
                        listOf("Copy ID"),
                        listOf("COPY"),
                        listOf(profileId.toString()),
                        BridgeService.CHANNEL_UNBLOCKS,
                        "Unblock Notifications",
                        "Notifications when users unblock you"
                    )
                }
                return false
            }
        } catch (e: Exception) {
            val message = "Error handling profile response: ${e.message ?: "Unknown error"}"
            Logger.apply {
                e(message)
                writeRaw(e.stackTraceToString())
            }
            return false
        }
    }
}
