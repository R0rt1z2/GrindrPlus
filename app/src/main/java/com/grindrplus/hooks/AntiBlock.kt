package com.grindrplus.hooks

import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.bridge.BridgeService
import com.grindrplus.core.Config
import com.grindrplus.core.DatabaseHelper
import com.grindrplus.utils.RetrofitUtils
import com.grindrplus.core.logd
import com.grindrplus.core.loge
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.RetrofitUtils.isDELETE
import com.grindrplus.utils.hookConstructor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class AntiBlock : Hook(
    "Anti Block",
    "Notifies you when someone blocks or unblocks you"
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Track outgoing conversation deletions (chats we deleted ourselves)
    private val recentlyDeletedConversations = mutableSetOf<String>()
    
    // Obfuscated / renamed classes and services
    private val chatRestService = "vn.a" // search for '"v4/chat/conversation/{conversationId}"'
    private val conversationDeleteNotification = "com.grindrapp.android.chat.model.ConversationDeleteNotification"

    override fun init() {
        RetrofitUtils.hookService(findClass(chatRestService)) { originalHandler, originalProxy, method, args ->
            try {
                if (method.isDELETE("v4/chat/conversation/{conversationId}")) {
                    val conversationId = args.firstOrNull() as? String
                    if (conversationId != null) {
                        recentlyDeletedConversations.add(conversationId)
                        logd("Intercepted outgoing conversation deletion for $conversationId")
                    } else {
                        loge("chat rest service hook: DELETE args[0] was not a string: ${args.firstOrNull()}")
                    }
                }
            } catch (e: Exception) {
                loge("Error intercepting chat rest service: ${e.message}, ${e.stackTrace.joinToString("\n")}")
            }

            originalHandler.invoke(originalProxy, method, args)
        }

        // WS Notifications listener for live events
        scope.launch {
            GrindrPlus.serverNotifications.collect { notification ->
                if (notification.typeValue != "chat.v1.conversation.delete") return@collect
                logd("Received WS chat.v1.conversation.delete notification")

                try {
                    val conversationIdsArray = notification.payload?.optJSONArray("conversationIds")
                    if (conversationIdsArray == null) {
                        loge("No conversationIds in WS payload: ${notification.payload}")
                        return@collect
                    }
                    val conversationIds = (0 until conversationIdsArray.length()).map { conversationIdsArray.getString(it) }

                    logd("WS notification loop: processing ${conversationIds.size} ids")
                    conversationIds.forEach { conversationId ->
                        if (isIncomingBlock(conversationId)) {
                            logd("WS: Tracking incoming block for $conversationId")
                            checkProfileAndNotify(conversationId)
                        } else {
                            logd("WS: Skipping $conversationId since it is NOT an incoming block")
                        }
                    }
                } catch (e: Exception) {
                    loge("WS Notification Error: ${e.message}, ${e.stackTrace.joinToString("\n")}")
                }
            }
        }
    }

    private fun isIncomingBlock(conversationId: String): Boolean {
        if (recentlyDeletedConversations.contains(conversationId)) {
            logd("isIncomingBlock: $conversationId was recently deleted locally. Returning false.")
            return false
        }

        val myId = GrindrPlus.myProfileId.toLongOrNull() ?: 0L
        if (myId == 0L) {
            logd("isIncomingBlock: myProfileId is not known yet. Being safe and returning true.")
            return true // Be safe rather than sorry if myProfileId is uninitialized
        }

        val parts = conversationId.split(":")
        val otherProfileIdStr = parts.firstOrNull { it != myId.toString() }
        val otherProfileId = otherProfileIdStr?.toLongOrNull()

        if (otherProfileId == null) {
            logd("isIncomingBlock: Could not extract other user's ID from $conversationId")
            return false
        }

        logd("isIncomingBlock: Yes, $conversationId involves an incoming block from $otherProfileId")
        return true
    }

    private fun checkProfileAndNotify(conversationId: String) {
        val myId = GrindrPlus.myProfileId.toLongOrNull() ?: 0L
        if (myId == 0L) return

        val otherProfileId = conversationId.split(":")
            .mapNotNull { it.toLongOrNull() }
            .firstOrNull { it != myId } ?: return

        try {
            val response = fetchProfileData(otherProfileId.toString())
            handleProfileResponse(otherProfileId, conversationId, response)
        } catch (e: Exception) {
            loge("Check failed: ${e.message}, ${e.stackTrace.map { it.toString() }.toList().joinToString("\n")}")
        }
    }

    private fun fetchProfileData(profileId: String): String {
        // GET v4/profiles/$profileId
        // returns a single profile, only if not blocked (either way) and not deleted
        // GET v7/profiles/$profileId
        // returns a single profile with displayName 3 if deleted, 4 if blocked (either way)
        // POST v3/profiles
        // returns a list of non-deleted profiles, not differentiated by block status
        val response = GrindrPlus.httpClient.sendRequest(
            url = "https://grindr.mobi/v7/profiles/$profileId",
            method = "GET"
        )
        val responseBody = response.body?.string()

        if (response.isSuccessful) {
            logd("fetchProfileData: HTTP ${response.code}: $responseBody")
            if (responseBody.isNullOrBlank()) {
                logd("fetchProfileData: HTTP ${response.code}: response is null or blank")
                throw Exception("HTTP ${response.code}: response is null or blank")
            }
            return responseBody

        } else {
            logd("fetchProfileData: HTTP ${response.code}: $responseBody")
            throw Exception("HTTP ${response.code}: $responseBody")
        }
    }

    private fun handleProfileResponse(profileId: Long, conversationId: String, response: String) {
        try {
            logd("handleProfileResponse: Parsing response for profile $profileId")
            val jsonResponse = JSONObject(response)
            val profilesArray = jsonResponse.optJSONArray("profiles")

            val resolvedName = getNameFromDatabase(conversationId) ?: "User"

            // should not be empty for v7 requests
            if (profilesArray == null || profilesArray.length() == 0) {
                logd("logic: Profile missing fully. Treating as block by $resolvedName ($profileId")
                sendNotification(profileId, resolvedName, EventType.BLOCK)
                return
            }

            val profile = profilesArray.getJSONObject(0)
            val profileDisplayName = profile.optString("displayName", "")

            val displayName = when (profileDisplayName) {
                "3", "4", "" -> resolvedName
                else -> profileDisplayName
            }
            val nameAndId = "$displayName ($profileId)"

            when (profileDisplayName) {
                "4" -> {
                    logd("Detected block by $nameAndId")
                    sendNotification(profileId, displayName, EventType.BLOCK)
                }
                "3" -> {
                    logd("Profile $nameAndId was deleted")
                    sendNotification(profileId, displayName, EventType.PROFILE_DELETE)
                }
                else -> {
                    logd("Detected unblock by $nameAndId")
                    sendNotification(profileId, displayName, EventType.UNBLOCK)
                }
            }
        } catch (e: Exception) {
            loge("response handling error: ${e.message}, ${e.stackTrace.joinToString("\n")}")
        }
    }

    enum class EventType {
        BLOCK,
        UNBLOCK,
        PROFILE_DELETE,
    }

    private fun sendNotification(
        profileId: Long,
        displayName: String,
        eventType: EventType
    ) {
        val nameAndId = "$displayName ($profileId)"

        val useToasts = Config.get("anti_block_use_toasts", false) as Boolean

        GrindrPlus.bridgeClient.logBlockEvent(
            profileId.toString(),
            displayName,
            eventType.name.lowercase(),
            GrindrPlus.packageName
        )

        when (eventType) {
            EventType.BLOCK -> {
                if (useToasts) {
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "Blocked by $displayName")

                } else {
                    GrindrPlus.bridgeClient.sendNotificationWithMultipleActions(
                        "Blocked by $displayName",
                        "Blocked by $nameAndId",
                        10000000 + (profileId % 10000000).toInt(),
                        listOf("Copy ID"),
                        listOf("COPY"),
                        listOf(profileId.toString()),
                        BridgeService.CHANNEL_BLOCKS,
                        "Block Notifications",
                        "Notifications when users block you"
                    )
                }
            }

            EventType.UNBLOCK -> {
                if (useToasts) {
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "Unblocked by $displayName")

                } else {
                    GrindrPlus.bridgeClient.sendNotificationWithMultipleActions(
                        "Unblocked by $displayName",
                        "Unblocked by $nameAndId",
                        20000000 + (profileId % 10000000).toInt(),
                        listOf("Copy ID"),
                        listOf("COPY"),
                        listOf(profileId.toString()),
                        BridgeService.CHANNEL_UNBLOCKS,
                        "Unblock Notifications",
                        "Notifications when users unblock you"
                    )
                }
            }

            EventType.PROFILE_DELETE -> {
                if (useToasts) {
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "$displayName deleted their profile")

                } else {
                    GrindrPlus.bridgeClient.sendNotificationWithMultipleActions(
                        "$displayName deleted their profile",
                        "$nameAndId deleted their profile",
                        30000000 + (profileId % 10000000).toInt(),
                        listOf("Copy ID"),
                        listOf("COPY"),
                        listOf(profileId.toString()),
                        BridgeService.CHANNEL_BLOCKS, // Re-use blocks channel
                        "Block Notifications",
                        "Notifications when users block you"
                    )
                }
            }
        }

    }

    private fun getNameFromDatabase(conversationId: String): String? {
        try {
            val name = (DatabaseHelper.query(
                "SELECT name FROM chat_conversations WHERE conversation_id = ?",
                arrayOf(conversationId)
            ).firstOrNull()
                ?.get("name") as? String)
                ?.takeIf { name -> name.isNotEmpty() }

            logd("DB query for resolvedName: $name")
            return name
        } catch (e: Exception) {
            loge("DB query for resolvedName failed: ${e.message}")
            return null
        }
    }

}
