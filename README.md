<div align="center">

# 🪶 SPARROW
**Unofficial Mobile Wallet for the SPW Network**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](#)
[![SPW](https://img.shields.io/badge/Network-Sparrow%20(SPW)-orange)](#)

*A production-grade, privacy-first crypto wallet featuring dual ECDSA/ECDH stealth address transfers, air-gapped cold storage QR vaults, real-time RPC node synchronization, and local hardware-backed encryption.*

[Features](#-features) • [Installation](#-installation) • [Security](#-security) • [Contributing](#-contributing) • [Contact](#-contact)

</div>

---

## ✨ Features

Sparrow provides a seamless and highly secure environment for managing your SPW assets.

*   **🕵️ Stealth Transfers**: Privacy-focused dual ECDSA/ECDH stealth address transactions. Keep your financial footprints hidden.
*   **🔒 Encrypted Local Storage**: Your private keys and recovery seeds are heavily encrypted using the Android hardware Keystore. They never leave your device.
*   **🛡️ Biometric Security**: Lock your application natively with Fingerprint or Face Unlock, falling back to a custom 6-digit PIN.
*   **🚨 Duress & Decoy Wallet**: Configure a specific Decoy PIN that automatically opens a dummy wallet or wipes the device data immediately when entered under duress.
*   **📡 Real-Time RPC Sync**: Connect directly to SPW Network nodes for fast, trustless blockchain synchronization.
*   **📱 QR Air-Gapped Vaults**: Generate and scan QR codes for secure, offline cold-storage interactions without exposing keys to an active network.

## 🚀 Installation & Building

### Prerequisites
*   Android Studio (Ladybug or newer)
*   Java JDK 17
*   Android device running API 24 (Nougat) or higher.

### Build from Source
1. **Clone the repository:**
   ```bash
   git clone https://github.com/MdSagorMunshi/SPARROW.git
   cd SPARROW
   ```
2. **Open the project:**
   Launch Android Studio and open the `SPARROW` directory. Let Gradle sync the project dependencies.

3. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   *The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`*

### Release Build & Code Signing
To build a production release, you must sign the APK with your own keystore.

1. Generate a keystore using `keytool`.
2. Configure your environment variables `KEYSTORE_PASSWORD` and `KEY_PASSWORD`.
3. Run the release build task:
   ```bash
   ./gradlew assembleRelease
   ```

## 🏗 Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose
*   **Local Database**: Room DB & DataStore
*   **Architecture**: MVVM (Model-View-ViewModel) + Coroutines/Flow
*   **Cryptography**: BouncyCastle ECDSA/ECDH, Android hardware-backed Keystore

## 🤝 Contributing

We welcome contributions from the community! Whether you want to fix bugs, improve documentation, or add exciting new features, please feel free to fork the repository and submit a Pull Request.

1. Check our [Issue Tracker](https://github.com/MdSagorMunshi/SPARROW/issues) for open tasks.
2. Read our [Contributing Guidelines](CONTRIBUTING.md) to understand the workflow.
3. Submit a PR using our provided templates!

## 🔐 Security

If you discover any security-related issues, please do NOT open a public issue. Instead, email `ryn@disr.it` directly.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
<div align="center">
  <b>Developed with ❤️ by <a href="mailto:ryn@disr.it">Ryan Shelby</a></b>
</div>
