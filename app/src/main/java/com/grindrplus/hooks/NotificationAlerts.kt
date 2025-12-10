package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class NotificationAlerts : Hook(
    "Notification Alerts",
    "Disable all Grindr warnings related to notifications"
) {
    private val notificationManager = "ue.e" // search for 'notification_reminder_time' (shows reminder dialog)

    override fun init() {
        findClass(notificationManager)
            .hook("a", HookStage.BEFORE) { param ->
                param.setResult(null)
            }
    }
}
