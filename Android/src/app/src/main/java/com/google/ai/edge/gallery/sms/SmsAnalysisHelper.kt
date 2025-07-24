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

import android.content.Context
import android.util.Log
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.TASK_LLM_CHAT
import com.google.ai.edge.gallery.data.getModelByName
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "AGSmsAnalysisHelper"

data class SmsAnalysisResult(
    val isSmishing: Boolean,
    val explanation: String,
    val confidence: Float = 0.0f
)

object SmsAnalysisHelper {
    
    /**
     * Check if a model is actually downloaded and available for use
     */
    private fun isModelDownloaded(context: Context, model: Model): Boolean {
        val externalFilesDir = context.getExternalFilesDir(null)
        if (externalFilesDir == null) {
            Log.w(TAG, "External files directory is null")
            return false
        }
        
        val modelPath = model.getPath(context)
        val modelFile = File(modelPath)
        
        Log.d(TAG, "Checking if model is downloaded: ${model.name} at path: $modelPath")
        val exists = modelFile.exists()
        Log.d(TAG, "Model file exists: $exists")
        
        return exists
    }
    
    /**
     * Get the default model for SMS analysis.
     * Priority: Qwen2.5-1.5B-Instruct q8 > first available downloaded LLM model > null
     */
    fun getDefaultModel(context: Context): Model? {
        // First try to find Qwen2.5-1.5B-Instruct q8 (user's preferred model)
        val preferredModel = getModelByName("Qwen2.5-1.5B-Instruct q8")
        if (preferredModel != null && isModelDownloaded(context, preferredModel)) {
            Log.d(TAG, "Using preferred model: ${preferredModel.name}")
            return preferredModel
        }
        
        // If preferred model not found or not downloaded, get the first available downloaded LLM model
        for (model in TASK_LLM_CHAT.models) {
            if (isModelDownloaded(context, model)) {
                Log.d(TAG, "Using first available downloaded model: ${model.name}")
                return model
            }
        }
        
        Log.w(TAG, "No downloaded LLM models available for SMS analysis")
        return null
    }
    
    /**
     * Analyze an SMS message using the AI model with semantic search
     */
    suspend fun analyzeSms(
        context: Context,
        smsText: String,
        model: Model? = null,
        useSemanticSearch: Boolean = true
    ): SmsAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            val targetModel = model ?: getDefaultModel(context)
            if (targetModel == null) {
                Log.e(TAG, "No downloaded model available for SMS analysis")
                return@withContext null
            }
            
            Log.d(TAG, "Starting SMS analysis with model: ${targetModel.name}")
            
            // Create the prompt for smishing detection
            val prompt = if (useSemanticSearch) {
                createPromptWithSemanticSearch(context, smsText)
            } else {
                createSmishingDetectionPrompt(smsText)
            }
            
            // Log the final prompt for debugging
            Log.d(TAG, "Final prompt length: ${prompt.length} characters")
            Log.d(TAG, "Final prompt: $prompt")
            
            var analysisResult: SmsAnalysisResult? = null
            var isCompleted = false
            var fullResponse = ""
            
            // Initialize the model if needed
            if (targetModel.instance == null) {
                Log.d(TAG, "Initializing model: ${targetModel.name}")
                LlmChatModelHelper.initialize(context, targetModel) { error ->
                    if (error.isNotEmpty()) {
                        Log.e(TAG, "Failed to initialize model: $error")
                        isCompleted = true
                    } else {
                        Log.d(TAG, "Model initialized successfully")
                    }
                }
                
                // Wait for initialization
                while (targetModel.instance == null && !isCompleted) {
                    kotlinx.coroutines.delay(100)
                }
                
                if (targetModel.instance == null) {
                    Log.e(TAG, "Model initialization failed")
                    return@withContext null
                }
            }
            
            // Run the inference
            LlmChatModelHelper.runInference(
                model = targetModel,
                input = prompt,
                resultListener = { partialResult, done ->
                    Log.d(TAG, "Inference result: $partialResult, done: $done")
                    fullResponse += partialResult
                    if (done) {
                        Log.d(TAG, "Full response accumulated: '$fullResponse'")
                        analysisResult = parseAnalysisResult(fullResponse)
                        isCompleted = true
                    }
                },
                cleanUpListener = {
                    Log.d(TAG, "Model cleanup completed")
                }
            )
            
            // Wait for analysis to complete
            while (!isCompleted) {
                kotlinx.coroutines.delay(100)
            }
            
            analysisResult
        } catch (e: Exception) {
            Log.e(TAG, "Error during SMS analysis", e)
            null
        }
    }
    
    /**
     * Create prompt with semantic search examples
     */
    private suspend fun createPromptWithSemanticSearch(context: Context, smsText: String): String {
        try {
            // Initialize embedding model if not already done
            if (!EmbeddingHelper.isInitialized()) {
                Log.d(TAG, "Initializing embedding model for semantic search...")
                val initialized = EmbeddingHelper.initialize(context)
                if (!initialized) {
                    Log.w(TAG, "Failed to initialize embedding model, falling back to simple prompt")
                    return createSmishingDetectionPrompt(smsText)
                }
            }
            
            // Find similar examples
            Log.d(TAG, "Performing semantic search for SMS: $smsText")
            val searchResult = EmbeddingHelper.findSimilarExamples(smsText, 2)
            if (searchResult == null) {
                Log.w(TAG, "Semantic search failed, falling back to simple prompt")
                return createSmishingDetectionPrompt(smsText)
            }
            
            // Build prompt with examples
            Log.d(TAG, "Building prompt with semantic search examples")
            return PromptBuilder.buildPromptWithExamples(context, smsText, searchResult)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in semantic search, falling back to simple prompt", e)
            return createSmishingDetectionPrompt(smsText)
        }
    }
    
    /**
     * Create a prompt for smishing detection
     */
    private fun createSmishingDetectionPrompt(smsText: String): String {
        return """
You are an SMS security expert. Analyze the following SMS message and determine if it's a smishing (SMS phishing) attempt or a benign message.

SMS Message: "$smsText"

Instructions:
1. Look for common smishing indicators:
   - Urgency or threats
   - Requests for personal information
   - Suspicious links
   - Impersonation of trusted entities
   - Unusual grammar or spelling
   - Requests for immediate action

2. Respond in this exact format:
CLASSIFICATION: [smishing/benign]
EXPLANATION: [Brief explanation in 35 words or less]

Example responses:
CLASSIFICATION: smishing
EXPLANATION: Urgent IRS claim with suspicious link suggests phishing attempt.

CLASSIFICATION: benign
EXPLANATION: Legitimate delivery notification from known courier service.

Analyze the message and respond:
""".trim()
    }
    
    /**
     * Parse the AI response to extract classification and explanation
     */
    private fun parseAnalysisResult(response: String): SmsAnalysisResult {
        Log.d(TAG, "Parsing response: $response")
        
        try {
            val lines = response.split("\n")
            var classification = ""
            var explanation = ""
            
            for (line in lines) {
                when {
                    // Handle both formats: "CLASSIFICATION:" and "## Classification:"
                    line.startsWith("CLASSIFICATION:", ignoreCase = true) -> {
                        classification = line.substringAfter(":").trim().lowercase()
                        Log.d(TAG, "Found classification: '$classification'")
                    }
                    line.startsWith("## CLASSIFICATION:", ignoreCase = true) -> {
                        classification = line.substringAfter("## CLASSIFICATION:").trim().lowercase()
                        Log.d(TAG, "Found classification: '$classification'")
                    }
                    line.startsWith("## Classification:", ignoreCase = true) -> {
                        classification = line.substringAfter("## Classification:").trim().lowercase()
                        Log.d(TAG, "Found classification: '$classification'")
                    }
                    // Handle both formats: "EXPLANATION:" and "## Explanation:"
                    line.startsWith("EXPLANATION:", ignoreCase = true) -> {
                        explanation = line.substringAfter(":").trim()
                        Log.d(TAG, "Found explanation: '$explanation'")
                    }
                    line.startsWith("## EXPLANATION:", ignoreCase = true) -> {
                        explanation = line.substringAfter("## EXPLANATION:").trim()
                        Log.d(TAG, "Found explanation: '$explanation'")
                    }
                    line.startsWith("## Explanation:", ignoreCase = true) -> {
                        explanation = line.substringAfter("## Explanation:").trim()
                        Log.d(TAG, "Found explanation: '$explanation'")
                    }
                }
            }
            
            // Check if classification contains "smishing" (case insensitive)
            val isSmishing = classification.contains("smishing", ignoreCase = true)
            Log.d(TAG, "Classification: '$classification', isSmishing: $isSmishing")
            
            return SmsAnalysisResult(
                isSmishing = isSmishing,
                explanation = explanation.ifEmpty { 
                    if (isSmishing) "Message contains suspicious content" 
                    else "Message appears to be legitimate" 
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing analysis result", e)
            return SmsAnalysisResult(
                isSmishing = false,
                explanation = "Unable to analyze message"
            )
        }
    }
    
    /**
     * Check if any LLM models are downloaded and available for analysis
     */
    fun hasAvailableModels(context: Context): Boolean {
        return TASK_LLM_CHAT.models.any { isModelDownloaded(context, it) }
    }
    
    /**
     * Get list of available downloaded model names for debugging
     */
    fun getAvailableModelNames(context: Context): List<String> {
        return TASK_LLM_CHAT.models
            .filter { isModelDownloaded(context, it) }
            .map { it.name }
    }
    
    /**
     * Clean up embedding resources
     */
    fun cleanupEmbeddings() {
        EmbeddingHelper.cleanup()
        PromptBuilder.clearCache()
        Log.d(TAG, "Embedding resources cleaned up")
    }
    
    /**
     * Check if semantic search is available
     */
    fun isSemanticSearchAvailable(context: Context): Boolean {
        return try {
            // Check if embedding assets exist
            val assets = context.assets
            val files = assets.list("") ?: emptyArray()
            files.contains("sms_embedding_model.tflite") &&
            files.contains("benign_embeddings.json") &&
            files.contains("smishing_embeddings.json") &&
            files.contains("prompt.md")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking semantic search availability", e)
            false
        }
    }
}