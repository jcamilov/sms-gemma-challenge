/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.sms

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SmsAnalysisActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("SmsAnalysisActivity", "Activity created")
        
        val sender = intent.getStringExtra("sms_sender") ?: "Unknown"
        val body = intent.getStringExtra("sms_body") ?: ""
        val timestamp = intent.getLongExtra("sms_timestamp", System.currentTimeMillis())
        
        Log.d("SmsAnalysisActivity", "Received data - Sender: $sender, Body: $body")
        
        setContent {
            GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmsAnalysisScreen(
                        sender = sender,
                        body = body,
                        timestamp = timestamp,
                        context = this,
                        onBackPressed = { finish() },
                        onSmishingDetails = { explanation ->
                            val intent = Intent(this, SmsDetailsActivity::class.java).apply {
                                putExtra("sms_sender", sender)
                                putExtra("sms_body", body)
                                putExtra("sms_explanation", explanation)
                                putExtra("sms_timestamp", timestamp)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsAnalysisScreen(
    sender: String,
    body: String,
    timestamp: Long,
    context: android.content.Context,
    onBackPressed: () -> Unit,
    onSmishingDetails: (String) -> Unit
) {
    var analysisStatus by remember { mutableStateOf(AnalysisStatus.PROCESSING) }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var modelInfo by remember { mutableStateOf<String?>(null) }
    var explanation by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Perform real AI analysis
    LaunchedEffect(Unit) {
        Log.d("SmsAnalysisActivity", "Starting AI analysis for SMS: $body")
        
        // Check if models are available
        if (!SmsAnalysisHelper.hasAvailableModels(context)) {
            Log.w("SmsAnalysisActivity", "No AI models available")
            analysisStatus = AnalysisStatus.ERROR
            errorMessage = "No AI models available for analysis. Please download a model first."
            return@LaunchedEffect
        }
        
        // Log available models for debugging
        val availableModels = SmsAnalysisHelper.getAvailableModelNames(context)
        Log.d("SmsAnalysisActivity", "Available models: $availableModels")
        modelInfo = "Using: ${availableModels.firstOrNull() ?: "Unknown model"}"
        
        try {
            val result = SmsAnalysisHelper.analyzeSms(
                context = context,
                smsText = body
            )
            
            if (result != null) {
                Log.d("SmsAnalysisActivity", "Analysis completed: ${result.isSmishing}, Explanation: ${result.explanation}")
                analysisResult = if (result.isSmishing) AnalysisResult.SMISHING else AnalysisResult.BENIGN
                explanation = result.explanation
                analysisStatus = AnalysisStatus.COMPLETED
            } else {
                Log.e("SmsAnalysisActivity", "Analysis returned null result")
                analysisStatus = AnalysisStatus.ERROR
                errorMessage = "Failed to analyze message. Please try again."
            }
        } catch (e: Exception) {
            Log.e("SmsAnalysisActivity", "Error during analysis", e)
            analysisStatus = AnalysisStatus.ERROR
            errorMessage = "Analysis failed: ${e.message}"
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SMS Content Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "From: $sender",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Message:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        ).padding(12.dp)
                    )
                }
            }
            
            // Model Info Card (if available)
            modelInfo?.let { info ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AI Model",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Analysis Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Analysis Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    when (analysisStatus) {
                        AnalysisStatus.PROCESSING -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Analyzing message with AI...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        AnalysisStatus.COMPLETED -> {
                            analysisResult?.let { result ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = when (result) {
                                            AnalysisResult.BENIGN -> Icons.Filled.CheckCircle
                                            AnalysisResult.SMISHING -> Icons.Filled.Warning
                                        },
                                        contentDescription = null,
                                        tint = when (result) {
                                            AnalysisResult.BENIGN -> Color.Green
                                            AnalysisResult.SMISHING -> Color.Red
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    
                                    Text(
                                        text = when (result) {
                                            AnalysisResult.BENIGN -> "Message is safe"
                                            AnalysisResult.SMISHING -> "Potential smishing detected"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when (result) {
                                            AnalysisResult.BENIGN -> Color.Green
                                            AnalysisResult.SMISHING -> Color.Red
                                        }
                                    )
                                }
                                
                                if (result == AnalysisResult.SMISHING) {
                                    Text(
                                        text = "This message contains suspicious content that may be a phishing attempt. Be cautious and do not click on any links or provide personal information.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    // Clickable details button for smishing messages
                                    explanation?.let { exp ->
                                        Button(
                                            onClick = { onSmishingDetails(exp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Red
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Warning,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("View Details")
                                        }
                                    }
                                }
                            }
                        }
                        
                        AnalysisStatus.ERROR -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Analysis failed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                            }
                            
                            errorMessage?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class AnalysisStatus {
    PROCESSING,
    COMPLETED,
    ERROR
}

enum class AnalysisResult {
    BENIGN,
    SMISHING
} 