package com.grindrplus.modules

import com.grindrplus.Hooker
import com.grindrplus.core.Command
import com.grindrplus.core.CommandModule
import com.grindrplus.core.Utils.toggleSetting
import com.grindrplus.core.Utils.logChatMessage

class Settings(recipient: String) : CommandModule(recipient) {
    @Command(name = "redesign", aliases = ["rd"], help = "Toggle the new Grindr design.")
    private fun redesign(args: List<String>) {
        logChatMessage(toggleSetting("profile_redesign", "Profile redesign"),
            this.recipient, this.recipient)
    }

    @Command(name = "views", aliases = ["vw"], help = "Control whether you want to be hidden from the view list.")
    private fun views(args: List<String>) {
        logChatMessage(toggleSetting("dont_record_views", "Hiding from views"),
            this.recipient, this.recipient)
    }

    @Command(name = "details", aliases = ["dt"], help = "Toggle the profile details feature (BMI, etc).")
    private fun details(args: List<String>) {
        logChatMessage(toggleSetting("show_profile_details", "Profile details"),
            this.recipient, this.recipient)
    }

    @Command(name = "clear", aliases = ["cl"], help = "Clear the album cache.")
    private fun clear(args: List<String>) {
        Hooker.globalCache.clearCache()
        logChatMessage("Album cache cleared.", this.recipient, this.recipient)
    }
}