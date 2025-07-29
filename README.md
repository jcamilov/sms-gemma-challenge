# SMS Guard 🛡️

**Personal AI-Powered Defense Against SMS Phishing Attacks**

SMS Guard is an innovative Android application that leverages on-device artificial intelligence to protect users from the growing threat of smishing (SMS phishing) attacks. In today's digital landscape, smishing attacks are becoming increasingly sophisticated and widespread, posing significant risks to personal data, financial security, and privacy. Traditional SMS filtering solutions often rely on cloud-based processing, raising concerns about data privacy and requiring constant internet connectivity.

SMS Guard addresses these challenges by providing a first-line defense tool that operates entirely on your device, ensuring complete privacy and independence from third-party providers or internet connection. The app not only detects malicious SMS messages in real-time but also educates users through explainable AI results, helping them understand why a message is flagged as suspicious and providing tips on how to react. This educational approach empowers users to develop better security awareness beyond SMS protection, creating a more informed and security-conscious user base.

## 🙏 Acknowledgments

This application is built upon the excellent foundation provided by the [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) project, which offers robust on-device AI capabilities and model management infrastructure. We extend our gratitude to the Google AI Edge team for their pioneering work in making on-device AI accessible and practical.

## ✨ Core Features

*   **🛡️ Real-Time Smishing Detection:** Instantly analyzes incoming SMS messages using advanced AI models to identify phishing attempts
*   **🔒 Complete Privacy Protection:** All processing happens locally on your device - no data leaves your phone
*   **📚 Educational Insights:** Get detailed explanations of why messages are flagged as suspicious, helping you learn to recognize threats
*   **🌍 Multilingual Support:** Detects smishing attempts across multiple languages and regional variations
*   **🧠 Adaptive Intelligence:** Uses few-shot prompting to adapt to new attack patterns and emerging threats
*   **⚡ Offline Operation:** Works without internet connection once models are downloaded
*   **🎯 High Accuracy:** Powered by Google's Gemma-3n model and supports other advanced language models
*   **📱 User-Friendly Interface:** Clean, intuitive design with clear visual indicators for message safety

## 🔄 How It Works

When an SMS arrives on your device, SMS Guard immediately processes the message content through its local AI model (see research approach and more technical details in our [report](). The system analyzes the text for common smishing indicators such as urgent requests, suspicious links, financial pressure tactics, and social engineering patterns. A small loading indicator shows the analysis is in progress.

**Safe Messages:** Receive a green verification mark indicating the message has been analyzed and deemed safe.

**Suspicious Messages:** Get a red warning mark for potentially malicious messages. The user can tap the mark to view detailed analysis explaining why the message was flagged, including specific threat indicators and educational information about similar attack patterns.

## 📱 Installation

1. **Download the APK:** Get the latest version from our releases (**TBD!!**)
2. **Enable Unknown Sources:** Go to Settings > Security > Unknown Sources (if not already enabled)
3. **Install the App:** Open the downloaded APK file and follow the installation prompts
4. **Grant Permissions:** Allow SMS access when prompted (required for message analysis)
5. **Download AI Models:** Click on the top right gear icon and follow any of the provided options for model donwload.

## 🚀 Usage Guide

**First Launch:**
- Grant necessary permissions for SMS access

**General Use:**
- For now the app does not persist messages, as it is meant for **demonstration purposes**.
- Incoming messages are analyzed instantly
- Check message status via colored indicators
- Tap red warnings for detailed threat analysis

**Model Management:**
- The app uses Gemma-3n-EB2-int4 by default
- Automatically falls back to other available models if needed
- No manual configuration required

## 🛠️ Technology Stack

*   **Google AI Edge:** Core on-device AI infrastructure and APIs
*   **LiteRT:** Lightweight runtime for optimized model execution
*   **Gemma-3n Model:** Primary AI model for smishing detection
*   **Android SMS APIs:** For message reception and processing
*   **Kotlin & Jetpack Compose:** Modern Android development framework

## 🔐 Privacy & Security

**Your Data Stays Private:**
- All SMS analysis happens locally on your device
- No message content is transmitted to external servers
- No personal data is collected or stored by third parties
- AI models run entirely offline after initial download

**Security Features:**
- End-to-end local processing
- No cloud dependencies for message analysis
- Secure model storage on device
- Minimal permissions required

## 🤝 Authors

- [Juan Vargas](https://github.com/jcamilov), Researcher at Fraunhofer IAO/IAT, Germany
- [Ayush Mittal](https://github.com/ayushmittalde), MSc Information studente, Technology Information (**TDB**). Univertät Stuttgart, Germany


## 🔗 Useful Links

*   [Research and technical report]() (**TBD!!!**)
*   [Google AI Edge Documentation](https://ai.google.dev/edge)
*   [LiteRT Community](https://huggingface.co/litert-community)

