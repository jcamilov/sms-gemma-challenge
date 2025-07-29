package com.google.ai.edge.gallery.sms

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SmsListViewModel : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    val messages: List<SmsMessage> get() = SmsMessageManager.messages
    
    fun addMessage(sender: String, body: String) {
        SmsMessageManager.addMessage(sender, body)
    }
    
    fun updateMessageAnalysis(messageId: String, isSmishing: Boolean, explanation: String, tips: String) {
        SmsMessageManager.updateMessageAnalysis(messageId, isSmishing, explanation, tips)
    }
    
    fun clearMessages() {
        SmsMessageManager.clearMessages()
    }
} 