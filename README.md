# CloakVault
A secure Android vault application utilizing AES-256 encryption, biometric access controls, and decoy environments.

CloakVault

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=white)
![Security](https://img.shields.io/badge/Security-AES--256-red?style=for-the-badge)

CloakVault is a high-security Android vault application designed for zero-knowledge text and media storage. It leverages AES-256 encryption, biometric access controls, and autonomous threat-logging to safeguard sensitive data against unauthorized physical access.

Core Security Features

* **Cryptographic Storage:** All text notes and media files are encrypted locally using AES-256 before being transmitted or stored.
* **Duress Mode (Plausible Deniability):** Incorporates a covert secondary PIN that provisions a sanitized, decoy environment under coerced-entry scenarios.
* **Autonomous Intruder Telemetry:** Failed authentication attempts trigger a silent background service (via CameraX) that captures front-facing telemetry and securely uploads the incident log to the cloud.
* **Biometric Access Control:** Integrated with Android's BiometricPrompt API for secure, hardware-backed authentication.

Technical Architecture

* **Language:** Kotlin
* **Architecture:** Modern Android Architecture (MVVM principles)
* **Backend:** Firebase Firestore (NoSQL Database), Firebase Storage (Media Hosting)
* **Hardware APIs:** CameraX (Silent Capture), Biometric API
* **Cryptography:** `javax.crypto` (AES/CBC/PKCS7Padding)

System Previews
*(Note: Add 2-3 screenshots of your app here later!)*
* **Left:** Secure Biometric Login
* **Center:** Encrypted Media Gallery
* **Right:** Intruder Telemetry Logs

Installation & Setup
1. Clone the repository: `git clone https://github.com/OmSawant8/CloakVault.git`
2. Open the project in **Android Studio**.
3. Connect your own Firebase project:
   * Create a project in the [Firebase Console](https://console.firebase.google.com/).
   * Download your `google-services.json` file.
   * Place it in the `app/` directory of this project.
4. Sync Gradle and Run on an emulator or physical device.

Disclaimer
This project was developed for educational purposes to demonstrate modern mobile security implementations, cryptographic data handling, and threat-logging mechanisms. 

---
*Developed by Om Sawant *
