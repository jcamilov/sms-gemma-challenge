#!/usr/bin/env python3
"""
Test script to verify Android assets work correctly.
"""

import json
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

def test_android_assets():
    """
    Test the prepared Android assets.
    """
    print("Testing Android assets...")
    
    # Test embeddings loading
    for class_name in ['smishing', 'benign']:
        json_file = f'{class_name}_embeddings.json'
        try:
            with open(json_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            embeddings = np.array(data['embeddings'])
            texts = data['texts']
            
            print(f"✓ {class_name}: {len(embeddings)} embeddings loaded")
            print(f"  - Dimension: {embeddings.shape[1]}")
            print(f"  - Sample: '{texts[0][:50]}...'")
            
        except Exception as e:
            print(f"✗ {class_name}: Error - {e}")
    
    print("\nTest completed!")

if __name__ == "__main__":
    test_android_assets()
