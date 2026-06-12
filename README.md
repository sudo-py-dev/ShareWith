<div align="center">
  <img src="logo.svg" alt="ShareWith Logo" width="128" height="128">

  # ShareWith 🚀
  
  **A powerful, secure, and fully offline Android file sharing application.**

  [![Build](https://github.com/sudo-py-dev/ShareWith/actions/workflows/release.yml/badge.svg)](https://github.com/sudo-py-dev/ShareWith/actions/workflows/release.yml)
  [![Download APK](https://img.shields.io/badge/Download%20APK-Latest-green.svg)](https://github.com/sudo-py-dev/ShareWith/releases/latest)
  [![API](https://img.shields.io/badge/API-26%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=26)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
  [![Kotlin](https://img.shields.io/badge/kotlin-2.4.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
</div>

---

ShareWith turns your Android device into a robust, localized file server in seconds. Easily share files and entire folders over your local Wi-Fi network without requiring internet access or cloud services. Built with Kotlin, Jetpack Compose, and Ktor (Netty engine).

## ✨ Key Features

* **📂 Interactive Directory Browsing**: Don't want to download a massive ZIP file? Click "Open" on shared folders to dynamically browse their contents and download only the specific files you need directly from any web browser.
* **🔒 Secure by Default**: 
  * **Anti-Hijacking**: Every session is securely bound to the client's IP address.
  * **Rate Limiting**: Built-in protection against brute-force password guessing.
  * **CSRF & XSS Protection**: The web interface is hardened against cross-site attacks.
  * **Path Traversal Prevention**: Rigorous backend checks ensure clients can only access the files you explicitly share.
  * **HTTPS/SSL Support**: Host the server securely using a custom PKCS12 certificate to encrypt all data in transit.
* **📡 100% Offline**: The application and its beautifully crafted Web UI work entirely offline. No external Google Fonts, tracking scripts, or analytics. Your files and metadata never leave your local network.
* **📱 Modern Android UI**: A polished, responsive Material Design 3 interface built with Jetpack Compose. Includes real-time connection logging, active session tracking, and IP blocking.
* **📸 Quick Connect via QR**: Simply scan the auto-generated QR code on your Android device with any smartphone or tablet to instantly access the shared files.
* **🌍 Fully Localized**: Designed with global support, offering translations in Hebrew, Spanish, Arabic, French, Russian, and English.

## ⚡ Performance & Code Optimizations

We recently refactored the backend server to ensure maximum efficiency:
* **Zero-Allocation Zip Streaming**: Rather than allocating a new 64KB buffer for every recursive subdirectory during ZIP downloads, we now allocate a single buffer in the route handler and pass it down, significantly reducing garbage collection pressure.
* **$O(1)$ Exception-Free Validation**: Replaced exception-based session UUID parsing with a high-performance regular expression check, eliminating expensive stack-trace generation overhead on invalid request tokens.
* **Lazy Search Sorting**: Removed redundant recursive sorting during file system traversals. We now perform a flat, fast sort on the final matched results list only.
* **Graceful HTTPS Fallback**: Added real-time tracking of HTTPS status. If the user's keystore fails to load, the server automatically falls back to HTTP and updates the UI and notification URL banners accordingly.

## 🛠️ Security Modes

ShareWith offers flexible security options depending on your environment:

1. **Manual Approval (Recommended)**: New devices connecting to the server must be manually approved by you within the Android app. Unrecognized devices are kept pending.
2. **Password Protected**: Secure the server with a custom password.
3. **Open**: Anyone on the local network can view and download shared files (Use with caution on public networks!).

## 🏗️ Technical Stack

* **UI Framework**: Jetpack Compose (Material 3)
* **Backend Server**: Ktor 3 (Netty Engine)
* **Language**: Kotlin
* **Minimum SDK**: API 26 (Android 8.0 Oreo)
* **Target SDK**: API 36 (Android 15)

## 🚀 Building from Source

To compile and run the application yourself:

1. Clone the repository:
   ```bash
   git clone https://github.com/sudo-py-dev/ShareWith
   cd ShareWith
   ```

2. Open the project in **Android Studio**.

3. Sync Gradle and run the application on your emulator or physical device.

To build a release APK from the command line:
```bash
./gradlew assembleRelease
```
_Note: If signing the release build, ensure you provide the correct `release.keystore` and `keystore.password` inside `local.properties`._

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!
Feel free to check out the [issues page](https://github.com/sudo-py-dev/ShareWith/issues).

## 📜 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
