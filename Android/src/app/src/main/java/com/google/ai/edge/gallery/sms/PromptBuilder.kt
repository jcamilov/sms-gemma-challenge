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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

private const val TAG = "AGPromptBuilder"

object PromptBuilder {
    
    private var promptTemplate: String? = null
    
    /**
     * Load a file from assets
     */
    private fun loadAssetFile(context: Context, fileName: String): String {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.readText()
    }
    
    /**
     * Load the prompt template from assets
     */
    suspend fun loadPromptTemplate(context: Context): String = withContext(Dispatchers.IO) {
        try {
            if (promptTemplate == null) {
                Log.d(TAG, "Loading prompt template from assets...")
                promptTemplate = loadAssetFile(context, "prompt.md")
                Log.d(TAG, "Prompt template loaded successfully")
            }
            promptTemplate!!
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load prompt template", e)
            // Return a fallback template
            getFallbackTemplate()
        }
    }
    
    /**
     * Build a prompt with semantic search examples
     */
    suspend fun buildPromptWithExamples(
        context: Context,
        smsText: String,
        searchResult: SemanticSearchResult
    ): String = withContext(Dispatchers.IO) {
        try {
            val template = loadPromptTemplate(context)
            
            // Build examples block
            val examplesBlock = buildExamplesBlock(searchResult)
            
            // Replace placeholders in template
            val prompt = template
                .replace("{example_block}", examplesBlock)
                .replace("{sms_text}", smsText)
            
            Log.d(TAG, "Built prompt with ${searchResult.benignExamples.take(2).size} benign and ${searchResult.smishingExamples.take(2).size} smishing examples")
            Log.d(TAG, "Prompt length: ${prompt.length} characters")
            Log.d(TAG, "Examples block length: ${examplesBlock.length} characters")
            prompt
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build prompt with examples", e)
            // Fallback to simple prompt
            createSimplePrompt(smsText)
        }
    }
    
    /**
     * Build examples block from semantic search results
     */
    private fun buildExamplesBlock(searchResult: SemanticSearchResult): String {
        val examples = mutableListOf<String>()
        
        // Add benign examples (limit to 2 examples)
        searchResult.benignExamples.take(2).forEach { example ->
            examples.add("""
                ## Example (Benign)
                **Message:** "${example.text}"
                **Classification:** benign
                **Explanation:** Legitimate message without fraudulent intent.
            """.trimIndent())
        }
        
        // Add smishing examples (limit to 2 examples)
        searchResult.smishingExamples.take(2).forEach { example ->
            examples.add("""
                ## Example (Smishing)
                **Message:** "${example.text}"
                **Classification:** smishing
                **Explanation:** Contains fraudulent intent to deceive.
            """.trimIndent())
        }
        
        return examples.joinToString("\n\n")
    }
    
    /**
     * Create a simple fallback prompt without examples
     */
    private fun createSimplePrompt(smsText: String): String {
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

Analyze the message and respond:
""".trim()
    }
    
    /**
     * Get fallback template if loading from assets fails
     */
    private fun getFallbackTemplate(): String {
        return """
## GOAL ##
Classify SMS messages as 'smishing' or 'benign' based solely on **intent to deceive or defraud**.

## ROLE ##
You are an SMS cybersecurity analyst specializing in detecting SMS phishing.

## DEFINITIONS ##
- 'Smishing': A fraudulent SMS aiming to deceive the recipient into doing harm to themselves.
- 'Benign': A legitimate and harmless SMS that does not seek to defraud.

## GUIDELINES ##
Classify only if there is a clear **malicious objective** like phishing, impersonation, or trickery.

## EXAMPLES ##
{example_block}

## INPUT MESSAGE ##
"{sms_text}"

## OUTPUT FORMAT ##
## Classification: smishing or benign
## Explanation: Brief explanation in 35 words or less.
""".trim()
    }
    
    /**
     * Clear cached template
     */
    fun clearCache() {
        promptTemplate = null
        Log.d(TAG, "Prompt template cache cleared")
    }
} 