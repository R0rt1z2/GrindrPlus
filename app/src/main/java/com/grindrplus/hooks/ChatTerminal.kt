package com.grindrplus.hooks

import com.grindrplus.commands.CommandHandler
import com.grindrplus.core.Config
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.getObjectField
import org.json.JSONObject

class ChatTerminal : Hook(
    "Chat terminal",
    "Create a chat terminal to execute commands"
) {
    private val chatMessageHandler = "J6.k" // search for '(chatMessageMetaData, "chatMessageMetaData")'

    override fun init() {
        findClass(chatMessageHandler).hook("l", HookStage.BEFORE) { param ->
            val message = getObjectField(param.arg(0), "chatMessage")
            val content = getObjectField(message, "content")
            val sender = getObjectField(content, "sender") as String
            val recipient = getObjectField(content, "recipient") as String
            val messageBody = JSONObject(getObjectField(content, "body") as String)
            if (!messageBody.has("text")) return@hook // Ignore non-text messages
            val text = messageBody.getString("text")

            val commandPrefix = (Config.get("command_prefix", "/") as String)
            if (text.startsWith(commandPrefix)) {
                param.setResult(null) // Don't send the command to the chat
                CommandHandler(sender, recipient).handle(text.substring(1))
            }
        }
    }
}