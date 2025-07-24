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
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.sqrt

private const val TAG = "AGEmbeddingHelper"

data class EmbeddingExample(
    val text: String,
    val classification: String, // "benign" or "smishing"
    val similarity: Float
)

data class SemanticSearchResult(
    val benignExamples: List<EmbeddingExample>,
    val smishingExamples: List<EmbeddingExample>
)

object EmbeddingHelper {
    
    private var benignEmbeddings: List<FloatArray> = emptyList()
    private var smishingEmbeddings: List<FloatArray> = emptyList()
    private var benignTexts: List<String> = emptyList()
    private var smishingTexts: List<String> = emptyList()
    private var isInitialized = false
    
    /**
     * Initialize and load pre-computed embeddings
     */
    suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing embedding helper...")
            loadEmbeddings(context)
            isInitialized = true
            Log.d(TAG, "Embedding helper initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize embedding helper", e)
            false
        }
    }
    
    /**
     * Load pre-computed embeddings from JSON files
     */
    private suspend fun loadEmbeddings(context: Context) = withContext(Dispatchers.IO) {
        try {
            // Load benign embeddings
            val benignJson = loadAssetFile(context, "benign_embeddings.json")
            val benignData = JSONObject(benignJson)
            val benignEmbeddingsArray = benignData.getJSONArray("embeddings")
            val benignTextsArray = benignData.getJSONArray("texts")
            
            benignEmbeddings = List(benignEmbeddingsArray.length()) { i ->
                val embeddingArray = benignEmbeddingsArray.getJSONArray(i)
                FloatArray(embeddingArray.length()) { j ->
                    embeddingArray.getDouble(j).toFloat()
                }
            }
            benignTexts = List(benignTextsArray.length()) { i ->
                benignTextsArray.getString(i)
            }
            
            // Load smishing embeddings
            val smishingJson = loadAssetFile(context, "smishing_embeddings.json")
            val smishingData = JSONObject(smishingJson)
            val smishingEmbeddingsArray = smishingData.getJSONArray("embeddings")
            val smishingTextsArray = smishingData.getJSONArray("texts")
            
            smishingEmbeddings = List(smishingEmbeddingsArray.length()) { i ->
                val embeddingArray = smishingEmbeddingsArray.getJSONArray(i)
                FloatArray(embeddingArray.length()) { j ->
                    embeddingArray.getDouble(j).toFloat()
                }
            }
            smishingTexts = List(smishingTextsArray.length()) { i ->
                smishingTextsArray.getString(i)
            }
            
            Log.d(TAG, "Loaded ${benignEmbeddings.size} benign embeddings and ${smishingEmbeddings.size} smishing embeddings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load embeddings", e)
            throw e
        }
    }
    
    /**
     * Load a file from assets
     */
    private fun loadAssetFile(context: Context, fileName: String): String {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.readText()
    }
    
    /**
     * Generate a simple embedding for text using word frequency
     */
    private fun generateSimpleEmbedding(text: String): FloatArray {
        val words = text.lowercase()
            .replace(Regex("[^a-z\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }
        
        // Create a simple word frequency vector
        val wordFreq = mutableMapOf<String, Int>()
        words.forEach { word ->
            wordFreq[word] = (wordFreq[word] ?: 0) + 1
        }
        
        // Convert to a fixed-size vector (384 dimensions)
        val embedding = FloatArray(384) { 0.0f }
        var index = 0
        wordFreq.entries.forEach { (word, freq) ->
            if (index < 384) {
                embedding[index] = freq.toFloat()
                index++
            }
        }
        
        // Normalize the embedding
        normalizeEmbedding(embedding)
        return embedding
    }
    
    /**
     * Normalize embedding vector
     */
    private fun normalizeEmbedding(embedding: FloatArray) {
        val norm = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 0) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
    }
    
    /**
     * Calculate cosine similarity between two embeddings
     */
    private fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        var dotProduct = 0.0f
        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
        }
        return dotProduct
    }
    
    /**
     * Find most similar examples for a given text
     */
    suspend fun findSimilarExamples(text: String, numExamples: Int = 2): SemanticSearchResult? = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.e(TAG, "Embedding helper not initialized")
            null
        } else {
            try {
                val queryEmbedding = generateSimpleEmbedding(text)
                
                // Find similar benign examples
                val benignExamples = findSimilarExamplesInList(queryEmbedding, benignEmbeddings, benignTexts, "benign", numExamples)
                
                // Find similar smishing examples
                val smishingExamples = findSimilarExamplesInList(queryEmbedding, smishingEmbeddings, smishingTexts, "smishing", numExamples)
                
                SemanticSearchResult(benignExamples, smishingExamples)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to find similar examples", e)
                null
            }
        }
    }
    
    /**
     * Find similar examples in a given embedding list
     */
    private fun findSimilarExamplesInList(
        queryEmbedding: FloatArray,
        embeddings: List<FloatArray>,
        texts: List<String>,
        classification: String,
        numExamples: Int
    ): List<EmbeddingExample> {
        val similarities = embeddings.mapIndexed { index, embedding ->
            val similarity = cosineSimilarity(queryEmbedding, embedding)
            EmbeddingExample(texts[index], classification, similarity)
        }.sortedByDescending { it.similarity }
        
        return similarities.take(numExamples)
    }
    
    /**
     * Check if the embedding helper is initialized
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        benignEmbeddings = emptyList()
        smishingEmbeddings = emptyList()
        benignTexts = emptyList()
        smishingTexts = emptyList()
        isInitialized = false
        Log.d(TAG, "Embedding helper cleaned up")
    }
} 