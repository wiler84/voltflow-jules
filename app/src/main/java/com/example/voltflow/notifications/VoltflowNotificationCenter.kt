package com.example.voltflow.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class VoltflowNotificationEvent(
    val title: String,
    val body: String,
    val type: String = "system",
)

object VoltflowNotificationCenter {
    private val _events = MutableSharedFlow<VoltflowNotificationEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun publish(event: VoltflowNotificationEvent) {
        _events.tryEmit(event)
    }
}
