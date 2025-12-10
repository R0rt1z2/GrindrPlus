package com.grindrplus.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ServerNotificationReceived(
    val typeValue: String,
    val notificationId: String?,
    val payload: JSONObject?,
    val status: Int?,
    val refValue: String?
)

object EventManager {
    private val eventScope = CoroutineScope(Dispatchers.IO)

    private val _serverNotifications = MutableSharedFlow<ServerNotificationReceived>(
        replay = 0,
        extraBufferCapacity = 100
    )
    val serverNotifications: SharedFlow<ServerNotificationReceived> = _serverNotifications.asSharedFlow()

    fun emitServerNotification(
        typeValue: String,
        notificationId: String?,
        payload: JSONObject?,
        status: Int?,
        refValue: String?
    ) {
        eventScope.launch {
            try {
                val notification = ServerNotificationReceived(
                    typeValue, notificationId, payload, status, refValue
                )
                _serverNotifications.emit(notification)
                Logger.d("Server notification event emitted: $typeValue", LogSource.MODULE)
            } catch (e: Exception) {
                Logger.e("Failed to emit server notification event: ${e.message}", LogSource.MODULE)
            }
        }
    }
}