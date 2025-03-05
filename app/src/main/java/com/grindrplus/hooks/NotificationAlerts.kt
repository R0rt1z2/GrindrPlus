package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class NotificationAlerts : Hook(
    "Notification Alerts",
    "Disable all Grindr warnings related to notifications"
) {
    private val notificationManager = "W6.d" // search for '0L, "notification_reminder_time"'

    override fun init() {
        findClass(notificationManager)
            .hook("a", HookStage.BEFORE) { param ->
                param.setResult(null)
            }
    }
}