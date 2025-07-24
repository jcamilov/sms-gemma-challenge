# Android SMS Semantic Search Assets

This directory contains all the files needed for SMS semantic search in your Android app.

## Files Included:

### Model Files:
- `sms_embedding_model.tflite` - TensorFlow Lite model for generating embeddings
- Model: all-MiniLM-L6-v2

### Embedding Files:
- `smishing_embeddings.json` - Pre-computed embeddings for smishing SMS
- `benign_embeddings.json` - Pre-computed embeddings for benign SMS
- `embeddings_metadata.json` - Metadata about the embeddings

## How to Use:

1. Copy all files to your Android app's `assets` folder
2. Use TensorFlow Lite to load the model
3. Load the JSON files to get pre-computed embeddings
4. Implement cosine similarity search

## Model Information:
- Model: all-MiniLM-L6-v2
- Classes: smishing, benign
- Embedding dimension: Check metadata file

## Android Implementation Steps:

1. Add TensorFlow Lite dependency to your `build.gradle`:
```gradle
implementation 'org.tensorflow:tensorflow-lite:2.9.0'
implementation 'org.tensorflow:tensorflow-lite-support:0.4.2'
```

2. Load the model:
```kotlin
val model = Interpreter(loadModelFile(context, "sms_embedding_model.tflite"))
```

3. Load embeddings from JSON files
4. Implement semantic search using cosine similarity

## File Sizes:
- benign_embeddings.json: 10.70 MB
- embeddings_metadata.json: 0.00 MB
- smishing_embeddings.json: 10.80 MB
- sms_embedding_model.tflite: 1.35 MB
