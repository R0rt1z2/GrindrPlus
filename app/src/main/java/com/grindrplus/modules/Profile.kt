package com.grindrplus.modules

import com.grindrplus.core.Command
import com.grindrplus.core.CommandModule
import com.grindrplus.core.Hooks.ownProfileId
import com.grindrplus.core.Utils.logChatMessage
import com.grindrplus.core.Utils.openProfile

class Profile(recipient: String) : CommandModule(recipient) {
    @Command(name = "id", help = "Get the profile ID of the current profile.")
    private fun id(args: List<String>) {
        logChatMessage("This person's profile ID is: ${this.recipient}",
            this.recipient, this.recipient)
    }

    @Command(name = "myId", help = "Get your own profile ID.")
    private fun myId(args: List<String>) {
        logChatMessage("Your profile ID is: $ownProfileId",
            this.recipient, this.recipient)
    }

    @Command(name = "open", help = "Open a profile by its ID.")
    private fun open(args: List<String>) {
        if (args.isNotEmpty()) {
            openProfile(args[0])
        } else {
            logChatMessage("Please specify a profile ID.",
                this.recipient, this.recipient)
        }
    }
}