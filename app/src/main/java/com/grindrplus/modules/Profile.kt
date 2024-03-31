package com.grindrplus.modules

import android.content.ClipData
import com.grindrplus.core.Command
import com.grindrplus.core.CommandModule
import com.grindrplus.core.Hooks.ownProfileId
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import com.grindrplus.Hooker
import com.grindrplus.core.Utils.openProfile
import com.grindrplus.core.Utils.showDialog
import com.grindrplus.core.Utils.showToast

class Profile(recipient: String, sender: String) : CommandModule(recipient, sender) {
    @Command(name = "id", help = "Get the profile ID of the current profile.")
    private fun id(args: List<String>) {
        showDialog("Profile ID", "This person's profile ID is: ${this.recipient}",
            "OK", {}, "Copy ID",
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val clipboard = Hooker.appContext.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("Profile ID", this.recipient))
                }
                showToast(Toast.LENGTH_LONG, "Profile ID copied to clipboard.")
            }
        )
    }

    @Command(name = "myId", help = "Get your own profile ID.")
    private fun myId(args: List<String>) {
        showDialog("Profile ID", "Your profile ID is: ${ownProfileId}",
            "OK", {}, "Copy ID",
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val clipboard = Hooker.appContext.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("Profile ID", ownProfileId))
                }
                showToast(Toast.LENGTH_LONG, "Profile ID copied to clipboard.")
            }
        )
    }

    @Command(name = "open", help = "Open a profile by its ID.")
    private fun open(args: List<String>) {
        if (args.isNotEmpty()) {
            openProfile(args[0])
        } else {
            showToast(Toast.LENGTH_LONG, "Please provide a profile ID.")
        }
    }
}