# ShareWith 🚀

ShareWith is a powerful, secure, and fully offline Android application that lets you easily share files and entire folders over your local Wi-Fi network. Built with Kotlin, Jetpack Compose, Ktor, and Netty, it turns your Android device into a robust, localized file server in seconds.

## ✨ Key Features

*   **📂 Interactive Directory Browsing**: Don't want to download a massive ZIP file? You can now click "Open" on shared folders to browse their contents and download only the specific files you need directly from your web browser.
*   **🔒 Secure by Default**: 
    *   **Anti-Hijacking**: Every session is securely bound to the client's IP address.
    *   **Rate Limiting**: Built-in protection against brute-force password guessing.
    *   **CSRF & XSS Protection**: The web interface is hardened against cross-site attacks.
    *   **Path Traversal Prevention**: Rigorous backend checks ensure clients can only access the files you explicitly share.
*   **📡 100% Offline**: The application and its beautifully crafted Web UI work entirely offline. No external Google Fonts, tracking scripts, or analytics. Your files and metadata never leave your local network.
*   **📱 Modern Android UI**: A polished, responsive Material Design 3 interface built with Jetpack Compose. Includes real-time connection logging, active session tracking, and IP blocking.
*   **📸 Quick Connect via QR**: Simply scan the auto-generated QR code on your Android device with any smartphone or tablet to instantly access the shared files.

## 🛠️ Security Modes

ShareWith offers flexible security options depending on your environment:
1.  **Manual Approval (Recommended)**: New devices connecting to the server must be manually approved by you within the Android app.
2.  **Password Protected**: Secure the server with a custom password.
3.  **Open**: Anyone on the local network can view and download shared files (Use with caution on public networks!).

## 🏗️ Technical Stack

*   **UI**: Jetpack Compose (Material 3)
*   **Server**: Ktor & Netty
*   **Language**: Kotlin
*   **Minimum SDK**: API 23 (Android 6.0 Marshmallow)

## 🚀 Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/sudo-py-dev/ShareWith
   ```
2. Open the project in Android Studio.
3. Sync Gradle and run the application on your emulator or physical device.

To build a release APK from the command line:
```bash
./gradlew assembleRelease
```

## 📜 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
