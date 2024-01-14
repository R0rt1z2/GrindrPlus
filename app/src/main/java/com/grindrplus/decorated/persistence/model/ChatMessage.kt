package com.grindrplus.decorated.persistence.model

import com.grindrplus.Hooker
import com.grindrplus.core.Obfuscation

import de.robv.android.xposed.XposedHelpers

@Suppress(
    "UNUSED", "UNCHECKED_CAST",
    "REDUNDANTVISIBILITYMODIFIER",
    "PLATFORM_CLASS_MAPPED_TO_KOTLIN",
)
class ChatMessage(public var instance: Any = XposedHelpers.newInstance(CLAZZ)) {
    companion object {
        private val CLAZZ: Class<*> by lazy {
            XposedHelpers.findClass(
                Obfuscation.GApp.persistence.model.ChatMessage,
                Hooker.pkgParam.classLoader)
        }

        val TAP_TYPE_NONE: String? by lazy {
            getObjectFieldStatic<String>("TAP_TYPE_NONE")
        }

        private inline fun <reified T> getObjectFieldStatic(fieldName: String): T? =
            XposedHelpers.getStaticObjectField(CLAZZ, fieldName) as? T
    }

    private fun <T> getField(fieldName: String): T? = XposedHelpers.getObjectField(instance, fieldName) as? T
    private fun setField(fieldName: String, value: Any?) = XposedHelpers.setObjectField(instance, fieldName, value)

    var album: Any?
        get() = getField("album")
        set(value) = setField("album", value)

    var body: String?
        get() = getField("body")
        set(value) = setField("body", value)

    var conversationId: String?
        get() = getField("conversationId")
        set(value) = setField("conversationId", value)

    var countryCode: String?
        get() = getField("countryCode")
        set(value) = setField("countryCode", value)

    var countryCodes: List<String>?
        get() = getField("countryCodes")
        set(value) = setField("countryCodes", value)

    var dateHeader: String?
        get() = getField("dateHeader")
        set(value) = setField("dateHeader", value)

    var foundYouViaType: String?
        get() = getField("foundYouViaType")
        set(value) = setField("foundYouViaType", value)

    var foundYouViaValue: String?
        get() = getField("foundYouViaValue")
        set(value) = setField("foundYouViaValue", value)

    var groupChatTips: String?
        get() = getField("groupChatTips")
        set(value) = setField("groupChatTips", value)

    var groupNewName: String?
        get() = getField("groupNewName")
        set(value) = setField("groupNewName", value)

    var inviteesList: List<String>?
        get() = getField("inviteesList")
        set(value) = setField("inviteesList", value)

    var isGroupOwnerLeave: java.lang.Boolean?
        get() = getField("isGroupOwnerLeave")
        set(value) = setField("isGroupOwnerLeave", value)

    var isTimestampShown: java.lang.Boolean?
        get() = getField("isTimestampShown")
        set(value) = setField("isTimestampShown", value)

    var latitude: Double
        get() = getField("latitude") ?: 0.0
        set(value) = setField("latitude", value)

    var longitude: Double
        get() = getField("longitude") ?: 0.0
        set(value) = setField("longitude", value)

    var mediaHash: String?
        get() = getField("mediaHash")
        set(value) = setField("mediaHash", value)

    var mediaUrl: String?
        get() = getField("mediaUrl")
        set(value) = setField("mediaUrl", value)

    var messageContext: String?
        get() = getField("messageContext")
        set(value) = setField("messageContext", value)

    var messageId: String?
        get() = getField("messageId")
        set(value) = setField("messageId", value)

    var pushMetaData: Any?
        get() = getField("pushMetaData")
        set(value) = setField("pushMetaData", value)

    var reactions: List<Any>?
        get() = getField("reactions")
        set(value) = setField("reactions", value)

    var recipient: String?
        get() = getField("recipient")
        set(value) = setField("recipient", value)

    var repliedMessage: Any?
        get() = getField("repliedMessage")
        set(value) = setField("repliedMessage", value)

    var reply: String?
        get() = getField("reply")
        set(value) = setField("reply", value)

    var replyMessageBody: String?
        get() = getField("replyMessageBody")
        set(value) = setField("replyMessageBody", value)

    var replyMessageEntry: String?
        get() = getField("replyMessageEntry")
        set(value) = setField("replyMessageEntry", value)

    var replyMessageId: String?
        get() = getField("replyMessageId")
        set(value) = setField("replyMessageId", value)

    var replyMessageName: String?
        get() = getField("replyMessageName")
        set(value) = setField("replyMessageName", value)

    var replyMessageType: String?
        get() = getField("replyMessageType")
        set(value) = setField("replyMessageType", value)

    var sender: String?
        get() = getField("sender")
        set(value) = setField("sender", value)

    var senderPushProfile: Any?
        get() = getField("senderPushProfile")
        set(value) = setField("senderPushProfile", value)

    var sourceOverride: String?
        get() = getField("sourceOverride")
        set(value) = setField("sourceOverride", value)

    var stanzaId: String?
        get() = getField("stanzaId")
        set(value) = setField("stanzaId", value)

    var status: Int
        get() = getField("status") ?: 0
        set(value) = setField("status", value)

    var tapType: String?
        get() = getField("tapType")
        set(value) = setField("tapType", value)

    var timestamp: Long
        get() = getField("timestamp") ?: 0L
        set(value) = setField("timestamp", value)

    var translation: String?
        get() = getField("translation")
        set(value) = setField("translation", value)

    var type: String?
        get() = getField("type")
        set(value) = setField("type", value)

    var unread: java.lang.Boolean?
        get() = getField("unread")
        set(value) = setField("unread", value)

    fun clone(): ChatMessage =
        ChatMessage(XposedHelpers.callMethod(instance, "clone"))
}


