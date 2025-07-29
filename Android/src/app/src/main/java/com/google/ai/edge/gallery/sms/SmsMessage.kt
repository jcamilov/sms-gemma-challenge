package com.google.ai.edge.gallery.sms

import androidx.compose.runtime.Immutable

@Immutable
data class SmsMessage(
    val id: String,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val isSmishing: Boolean? = null,
    val explanation: String? = null,
    val tips: String? = null
) {
    companion object {
        fun createId(): String = System.currentTimeMillis().toString()
    }
} 