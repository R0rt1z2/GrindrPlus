package com.grindrplus.hooks

import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.bridge.BridgeService
import com.grindrplus.core.Config
import com.grindrplus.core.DatabaseHelper
import com.grindrplus.core.Logger
import com.grindrplus.core.logd
import com.grindrplus.core.loge
import com.grindrplus.core.logi
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.getObjectField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class AntiBlock : Hook(
    "Anti Block",
    "Notifies you when someone blocks or unblocks you"
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var myProfileId: Long = 0
    private val chatDeleteConversationPlugin = "F6.c" // search for 'com.grindrapp.android.chat.ChatDeleteConversationPlugin'
    private val inboxFragmentV2DeleteConversations = "ca.i" // search for '("chat_read_receipt", conversationId, null);'
    private val individualUnblockActivityViewModel = "ng.p" // search for 'SnackbarEvent.i.ERROR, R.string.unblock_individual_sync_blocks_failure, null, new SnackbarEvent'

    override fun init() {
        // search for '.setValue(new DialogMessage(116, null, 2, null));'
        findClass(individualUnblockActivityViewModel).hook("K", HookStage.BEFORE) { param ->
            GrindrPlus.shouldTriggerAntiblock = false
        }

        // search for '.setValue(new DialogMessage(116, null, 2, null));'
        findClass(individualUnblockActivityViewModel).hook("K", HookStage.AFTER) { param ->
            Thread.sleep(700) // Wait for WS to unblock
            GrindrPlus.shouldTriggerAntiblock = true
        }

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
                    logd("Request to delete $numberOfChatsToDelete chats")
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
                }
            }

            findClass("com.grindrapp.android.chat.model.ConversationDeleteNotification")
                .hookConstructor(HookStage.BEFORE) { param ->
                    @Suppress("UNCHECKED_CAST")
                    if (GrindrPlus.shouldTriggerAntiblock) {
                        val profiles = param.args().firstOrNull() as? List<String> ?: emptyList()
                        param.setArg(0, emptyList<String>())
                    }
                }

            scope.launch {
                GrindrPlus.serverNotifications.collect { notification ->
                    if (notification.typeValue != "chat.v1.conversation.delete") return@collect
                    if (!GrindrPlus.shouldTriggerAntiblock) return@collect

                    val conversationIds = notification.payload
                        ?.optJSONArray("conversationIds") ?: return@collect

                    val conversationIdStrings = (0 until conversationIds.length())
                        .map { index -> conversationIds.getString(index) }

                    val myId = GrindrPlus.myProfileId.toLongOrNull() ?: return@collect

                    val otherProfileId = conversationIdStrings
                        .flatMap { conversationId ->
                            conversationId.split(":").mapNotNull { it.toLongOrNull() }
                        }
                        .firstOrNull { id -> id != myId }

                    if (otherProfileId == null || otherProfileId == myId) {
                        logd("Skipping block/unblock handling for my own profile or no valid profile found")
                        return@collect
                    }

                    try {
                        if (DatabaseHelper.query(
                                "SELECT * FROM blocks WHERE profileId = ?",
                                arrayOf(otherProfileId.toString())
                            ).isNotEmpty()
                        ) {
                            return@collect
                        }
                    } catch (e: Exception) {
                        loge("Error checking if user is blocked: ${e.message}")
                        Logger.writeRaw(e.stackTraceToString())
                    }

                    try {
                        val response = fetchProfileData(otherProfileId.toString())
                        if (handleProfileResponse(otherProfileId,
                                conversationIdStrings.joinToString(","), response)) {
                        }
                    } catch (e: Exception) {
                        loge("Error handling block/unblock request: ${e.message ?: "Unknown error"}")
                        Logger.writeRaw(e.stackTraceToString())
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
                    loge("Error fetching display name: ${e.message}")
                    Logger.writeRaw(e.stackTraceToString())
                    displayName = profileId.toString()
                }
                displayName = if (displayName == profileId.toString() || displayName == "null")
                { profileId.toString() } else { "$displayName ($profileId)" }
                GrindrPlus.bridgeClient.logBlockEvent(profileId.toString(), displayName, true,
                    GrindrPlus.packageName)
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
                GrindrPlus.bridgeClient.logBlockEvent(profileId.toString(), displayName, false,
                    GrindrPlus.packageName)
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
            loge("Error handling profile response: ${e.message ?: "Unknown error"}")
            Logger.writeRaw(e.stackTraceToString())
            return false
        }
    }
}
