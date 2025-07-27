package com.google.ai.edge.gallery.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import com.google.ai.edge.gallery.sms.SmsAnalysisActivity

private const val TAG = "AGSmsReceiver"

class SmsReceiver : BroadcastReceiver() {

    init {
        Log.d(TAG, "SmsReceiver initialized")
    }
    
    companion object {
        fun logReceiverStatus(context: Context) {
            Log.d(TAG, "SmsReceiver status check - App package: ${context.packageName}")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Broadcast received with action: ${intent.action}")
        
        when (intent.action) {
            "android.provider.Telephony.SMS_RECEIVED" -> {
                Log.d(TAG, "SMS received")
                
                val bundle = intent.extras
                if (bundle != null) {
                    Log.d(TAG, "Bundle contains keys: ${bundle.keySet()}")
                    
                    val pdus = bundle["pdus"] as Array<*>?
                    if (pdus != null) {
                        Log.d(TAG, "PDUs found: ${pdus.size}")
                        
                        val messages = arrayOfNulls<SmsMessage>(pdus.size)
                        var sender = ""
                        var messageBody = ""
                        
                        for (i in pdus.indices) {
                            val pdu = pdus[i] as ByteArray
                            val message = SmsMessage.createFromPdu(pdu)
                            messages[i] = message
                            
                            if (i == 0) {
                                sender = message.originatingAddress ?: ""
                            }
                            messageBody += message.messageBody
                        }
                        
                        Log.d(TAG, "SMS from: $sender, body: $messageBody")
                        launchSmsAnalysis(context, sender, messageBody)
                    } else {
                        Log.w(TAG, "No PDUs found in bundle")
                    }
                } else {
                    Log.w(TAG, "No extras bundle found")
                }
            }
            
            "com.google.ai.edge.gallery.TEST_SMS" -> {
                Log.d(TAG, "Test SMS received")
                val sender = intent.getStringExtra("sms_sender") ?: "+1234567890"
                val body = intent.getStringExtra("sms_body") ?: "Test SMS message"
                launchSmsAnalysis(context, sender, body)
            }
            
            else -> {
                Log.d(TAG, "Received broadcast with different action: ${intent.action}")
            }
        }
    }
    
    private fun launchSmsAnalysis(context: Context, sender: String, body: String) {
        val analysisIntent = Intent(context, SmsAnalysisActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("sms_sender", sender)
            putExtra("sms_body", body)
            putExtra("sms_timestamp", System.currentTimeMillis())
        }
        
        try {
            context.startActivity(analysisIntent)
            Log.d(TAG, "SMS Analysis Activity launched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch SMS Analysis Activity", e)
        }
    }
} 