package com.grindrplus.decorated.persistence.model;

import com.grindrplus.Hooker;
import com.grindrplus.Obfuscation;

import de.robv.android.xposed.XposedHelpers

class ChatMessage {
    var instance: Any = XposedHelpers.newInstance(
        CLAZZ
    )

    companion object {
        val CLAZZ: Class<*> by lazy {
            XposedHelpers.findClass(Obfuscation.GApp.persistence.model.ChatMessage, Hooker.pkgParam.classLoader)
        }

        val TABLE_NAME: String? by lazy {
            XposedHelpers.getStaticObjectField(
                CLAZZ,
                Obfuscation.GApp.persistence.model.ChatMessage_.TABLE_NAME
            ) as String?
        }

        val TAP_TYPE_FRIENDLY: String? by lazy {
            XposedHelpers.getStaticObjectField(
                CLAZZ,
                Obfuscation.GApp.persistence.model.ChatMessage_.TAP_TYPE_FRIENDLY
            ) as String?
        }

        val TAP_TYPE_HOT: String? by lazy {
            XposedHelpers.getStaticObjectField(
                CLAZZ,
                Obfuscation.GApp.persistence.model.ChatMessage_.TAP_TYPE_HOT
            ) as String?
        }

        val TAP_TYPE_LOOKING: String? by lazy {
            XposedHelpers.getStaticObjectField(
                CLAZZ,
                Obfuscation.GApp.persistence.model.ChatMessage_.TAP_TYPE_LOOKING
            ) as String?
        }

        val TAP_TYPE_NONE: String? by lazy {
            XposedHelpers.getStaticObjectField(
                CLAZZ,
                Obfuscation.GApp.persistence.model.ChatMessage_.TAP_TYPE_NONE
            ) as String?
        }

        val tapTypes: java.util.List<java.lang.String>? by lazy {
            XposedHelpers.getStaticObjectField(
                CLAZZ,
                Obfuscation.GApp.persistence.model.ChatMessage_.tapTypes
            ) as java.util.List<java.lang.String>?
        }
    }


    var album: Any? //FIXME: Use a decorator for album
        get() = XposedHelpers.getObjectField(instance, "album")
        set(value) = XposedHelpers.setObjectField(instance, "album", value)

    var body: String?
        get() = XposedHelpers.getObjectField(instance, "body") as String?
        set(value) = XposedHelpers.setObjectField(instance, "body", value)

    var conversationId: String?
        get() = XposedHelpers.getObjectField(instance, "conversationId") as String?
        set(value) = XposedHelpers.setObjectField(instance, "conversationId", value)

    var countryCode: String?
        get() = XposedHelpers.getObjectField(instance, "countryCode") as String?
        set(value) = XposedHelpers.setObjectField(instance, "countryCode", value)

    var countryCodes: List<String>?
        get() = XposedHelpers.getObjectField(instance, "countryCodes") as List<String>?
        set(value) = XposedHelpers.setObjectField(instance, "countryCodes", value)

    var dateHeader: String?
        get() = XposedHelpers.getObjectField(instance, "dateHeader") as String?
        set(value) = XposedHelpers.setObjectField(instance, "dateHeader", value)

    var foundYouViaType: String?
        get() = XposedHelpers.getObjectField(instance, "foundYouViaType") as String?
        set(value) = XposedHelpers.setObjectField(instance, "foundYouViaType", value)

    var foundYouViaValue: String?
        get() = XposedHelpers.getObjectField(instance, "foundYouViaValue") as String?
        set(value) = XposedHelpers.setObjectField(instance, "foundYouViaValue", value)

    var groupChatTips: String?
        get() = XposedHelpers.getObjectField(instance, "groupChatTips") as String?
        set(value) = XposedHelpers.setObjectField(instance, "groupChatTips", value)

    var groupNewName: String?
        get() = XposedHelpers.getObjectField(instance, "groupNewName") as String?
        set(value) = XposedHelpers.setObjectField(instance, "groupNewName", value)

    var inviteesList: List<String>?
        get() = XposedHelpers.getObjectField(instance, "inviteesList") as List<String>?
        set(value) = XposedHelpers.setObjectField(instance, "inviteesList", value)

    var isGroupOwnerLeave: java.lang.Boolean?
        get() = XposedHelpers.getObjectField(instance, "isGroupOwnerLeave") as java.lang.Boolean?
        set(value) = XposedHelpers.setObjectField(instance, "isGroupOwnerLeave", value)

    var isTimestampShown: java.lang.Boolean?
        get() = XposedHelpers.getObjectField(instance, "isTimestampShown") as java.lang.Boolean?
        set(value) = XposedHelpers.setObjectField(instance, "isTimestampShown", value)

    var latitude: Double
        get() = XposedHelpers.getDoubleField(instance, "latitude")
        set(value) = XposedHelpers.setDoubleField(instance, "latitude", value)

    var longitude: Double
        get() = XposedHelpers.getDoubleField(instance, "longitude")
        set(value) = XposedHelpers.setDoubleField(instance, "longitude", value)

    var mediaHash: String?
        get() = XposedHelpers.getObjectField(instance, "mediaHash") as String?
        set(value) = XposedHelpers.setObjectField(instance, "mediaHash", value)

    var mediaUrl: String?
        get() = XposedHelpers.getObjectField(instance, "mediaUrl") as String?
        set(value) = XposedHelpers.setObjectField(instance, "mediaUrl", value)

    var messageContext: String?
        get() = XposedHelpers.getObjectField(instance, "messageContext") as String?
        set(value) = XposedHelpers.setObjectField(instance, "messageContext", value)

    var messageId: String?
        get() = XposedHelpers.getObjectField(instance, "messageId") as String?
        set(value) = XposedHelpers.setObjectField(instance, "messageId", value)

    var pushMetaData: Any?
        get() = XposedHelpers.getObjectField(instance, "pushMetaData")
        set(value) = XposedHelpers.setObjectField(instance, "pushMetaData", value)

    var reactions: List<Any>?
        get() = XposedHelpers.getObjectField(instance, "reactions") as List<Any>?
        set(value) = XposedHelpers.setObjectField(instance, "reactions", value)

    var recipient: String?
        get() = XposedHelpers.getObjectField(instance, "recipient") as String?
        set(value) = XposedHelpers.setObjectField(instance, "recipient", value)

    var repliedMessage: Any?
        get() = XposedHelpers.getObjectField(instance, "repliedMessage")
        set(value) = XposedHelpers.setObjectField(instance, "repliedMessage", value)

    var reply: String?
        get() = XposedHelpers.getObjectField(instance, "reply") as String?
        set(value) = XposedHelpers.setObjectField(instance, "reply", value)

    var replyMessageBody: String?
        get() = XposedHelpers.getObjectField(instance, "replyMessageBody") as String?
        set(value) = XposedHelpers.setObjectField(instance, "replyMessageBody", value)

    var replyMessageEntry: String?
        get() = XposedHelpers.getObjectField(instance, "replyMessageEntry") as String?
        set(value) = XposedHelpers.setObjectField(instance, "replyMessageEntry", value)

    var replyMessageId: String?
        get() = XposedHelpers.getObjectField(instance, "replyMessageId") as String?
        set(value) = XposedHelpers.setObjectField(instance, "replyMessageId", value)

    var replyMessageName: String?
        get() = XposedHelpers.getObjectField(instance, "replyMessageName") as String?
        set(value) = XposedHelpers.setObjectField(instance, "replyMessageName", value)

    var replyMessageType: String?
        get() = XposedHelpers.getObjectField(instance, "replyMessageType") as String?
        set(value) = XposedHelpers.setObjectField(instance, "replyMessageType", value)

    var sender: String?
        get() = XposedHelpers.getObjectField(instance, "sender") as String?
        set(value) = XposedHelpers.setObjectField(instance, "sender", value)

    var senderPushProfile: Any?
        get() = XposedHelpers.getObjectField(instance, "senderPushProfile")
        set(value) = XposedHelpers.setObjectField(instance, "senderPushProfile", value)

    var sourceOverride: String?
        get() = XposedHelpers.getObjectField(instance, "sourceOverride") as String?
        set(value) = XposedHelpers.setObjectField(instance, "sourceOverride", value)

    var stanzaId: String?
        get() = XposedHelpers.getObjectField(instance, "stanzaId") as String?
        set(value) = XposedHelpers.setObjectField(instance, "stanzaId", value)

    var status: Int
        get() = XposedHelpers.getIntField(instance, "status")
        set(value) = XposedHelpers.setIntField(instance, "status", value)

    var tapType: String?
        get() = XposedHelpers.getObjectField(instance, "tapType") as String?
        set(value) = XposedHelpers.setObjectField(instance, "tapType", value)

    var timestamp: Long
        get() = XposedHelpers.getLongField(instance, "timestamp")
        set(value) = XposedHelpers.setLongField(instance, "timestamp", value)

    var translation: String?
        get() = XposedHelpers.getObjectField(instance, "translation") as String?
        set(value) = XposedHelpers.setObjectField(instance, "translation", value)

    var type: String?
        get() = XposedHelpers.getObjectField(instance, "type") as String?
        set(value) = XposedHelpers.setObjectField(instance, "type", value)

    var unread: java.lang.Boolean?
        get() = XposedHelpers.getObjectField(instance, "unread") as java.lang.Boolean?
        set(value) = XposedHelpers.setObjectField(instance, "unread", value)
}

