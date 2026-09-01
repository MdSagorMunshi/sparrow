<div align="center">

# 🪶 SPARROW
**Institutional-Grade Android Wallet & Native Node Miner for the SPW Network**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android%2015%2F16%20(16KB%20Ready)-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%20%2F%20C%2B%2B%20JNI-7F52FF?logo=kotlin&logoColor=white)](#)
[![PoW](https://img.shields.io/badge/PoW%20Engine-RandomX%20Native-FF6F00)](#)
[![SPW](https://img.shields.io/badge/Network-Sparrow%20(SPW)-orange)](#)

*A production-grade, privacy-first crypto wallet featuring native C++ RandomX node mining, dual ECDSA/ECDH stealth address transfers, multi-wallet account management, bank-grade PDF financial statements, contactless NFC payments, and Android Keystore hardware encryption.*

**Official Links:** [SPW Network](https://spw.network/) | [SPW Web Wallet](https://wallet.spw.network/) | [Block Explorer](https://explorer.spw.network/)

[Features](#-key-features) • [Native Mining](#-native-randomx-mining) • [Multi-Wallet Hub](#-multi-wallet--account-management) • [Export & Statements](#-bank-grade-financial-statements) • [Architecture](#-architecture--tech-stack) • [Installation](#-installation--building) • [Security](#-security) • [License](#-license)

</div>

---

## ✨ Key Features

Sparrow provides an institutional-grade, privacy-preserving mobile wallet and decentralized node consensus engine for the SPW Network:

*   **⛏️ Real Native RandomX C++ Node Mining**: Fully embedded RandomX PoW engine compiled in C++ via NDK with ARM64/x86_64 JIT support, dynamic epoch key rotation (`SPW-v1-epoch-{height // 2048}`), and duty-cycle CPU throttling (10% to 100%).
*   **🧅 Tor & SOCKS5 Proxy Onion Routing**: Built-in network proxy layer routing RPC and mempool traffic over Tor (Orbot 127.0.0.1:9050 preset) or custom authenticated SOCKS5/HTTP proxies with live connectivity testing.
*   **🧊 Watch-Only (Cold Storage) Wallets**: Monitor air-gapped hardware cold storage addresses without private keys. Displays live balances and unspent outputs with spend-locking safeguards.
*   **🎛️ Coin Control & UTXO Management**: Fine-grained input selection bottom sheet with live unspent output listing, stealth output tagging, and Greedy Largest/Smallest fee optimization heuristics.
*   **🔥 Disposable Burner Invoices**: Generate ephemeral stealth payment requests with live countdown expiration timers, QR codes, and real-time on-chain settlement watchers.
*   **📱 Android Home Screen Glance AppWidget**: Real-time home screen widget displaying live portfolio balance, quick send shortcuts, and 1-tap mobile node mining toggles.
*   **📳 Background Notifications & Balance Alerts**: Push notification engine alerting on incoming transactions, outgoing broadcasts, and mined block coinbase rewards.
*   **💼 Multi-Wallet & Account Management**: Dedicated wallet hub with 1-tap account switching, custom labels, 12 vs 24-word BIP-39 mnemonic creation, multi-tab imports, and PIN/Biometrics-guarded safe deletion.
*   **📄 Bank-Grade PDF & CSV Financial Statements**: Generate and export publication-ready financial statements and CSV accounting ledgers with custom date ranges, year filters, and transaction type breakdowns.
*   **🕵️ Dual-Key ECDSA/ECDH Stealth Transfers**: Cryptographic one-time address shielding for total privacy. Send funds to private recipient stealth addresses without revealing identity on-chain.
*   **📲 NFC Tap-to-Pay & Card Provisioning**: Contactless Phone-to-Phone SPW payments powered by ephemeral secp256r1 ECDH key exchange with AES-GCM encryption and anti-replay protection.
*   **📴 Offline Balance Caching & Live Sync**: Instant offline balance display with connectivity status banner, auto-updating in the background whenever live internet is detected.
*   **🎯 Strict Financial Input Validation**: Strict numeric and decimal sanitization across all transfer and receive amount fields with dedicated decimal keypads.
*   **🔒 Hardware Keystore Encryption**: Private keys and seed phrases are guarded by Android Keystore hardware enclaves (`KeyStore` / `KeyGenerator`) with biometric and 6-digit PIN authentication gates.
*   **🚨 Duress & Decoy Protocol**: Configure a separate Decoy PIN that automatically opens a dummy wallet or initiates a hardware wipe if forced to unlock under duress.
*   **🎨 Dynamic Institutional Themes**: Minimalist Dark, crisp Financial Light, and 100% True OLED Pure Black modes with automatic hardware OLED panel detection.

---

## ⛏️ Native RandomX Mining

Sparrow embeds the official **C++ RandomX Proof-of-Work algorithm** directly into the Android runtime via JNI (`librandomx_jni.so`), enabling authentic mobile node mining and consensus validation:

- **16 KB Page Size Compatibility**: Fully aligned ELF `LOAD` segments (`-Wl,-z,max-page-size=16384`) compliant with Android 15, 16, and next-gen ARM64 memory architectures.
- **Dynamic Epoch Management**: Tracks blockchain block height and dynamically re-keys the RandomX VM on 2048-block epoch boundaries (`"SPW-v1-epoch-${height / 2048}"`).
- **Cryptographic Block Builder**:
  - Generates valid Coinbase reward transactions (`1 SPW = 100,000,000 feathers`).
  - Serializes block headers and transactions into deterministic, alphabetically sorted canonical JSON matching SPW node consensus.
  - Computes pairwise double-SHA256 Merkle roots and compact bit target thresholds (`coef * 256^(exp - 3)`).
- **Duty-Cycle CPU Throttler**: Precise microsecond thread yielding prevents device overheating and battery drain.
- **Auto-Hiding Status Bar**: The dashboard displays real-time telemetry (Total Mined SPW, Session Mined, Accepted/Rejected shares, Monospace Block Heights & Hashes) and automatically hides when mining is inactive.

---

## 💼 Multi-Wallet & Account Management

Manage multiple independent SPW wallets and sub-accounts from a dedicated management hub:

- **12 vs 24-Word Mnemonic Generation**: Choose between **12 Words (128-bit Standard)** and **24 Words (256-bit Maximum Security)** when generating new wallets.
- **Multi-Tab Import Suite**:
  - **BIP-39 Recovery Phrase Tab**: 12/24-word phrase import with word count validation and 1-tap clipboard paste.
  - **Private Key Tab**: 64-hex char spend key + optional view key input for stealth dual-key import.
- **1-Tap Account Switcher**: Switch active wallets with instant cached balance recovery and background RPC resync.
- **Caution & PIN/Biometrics-Guarded Deletion**: Deleting a wallet requires explicit backup confirmation and PIN/fingerprint verification. If the active wallet is removed, Sparrow automatically falls back to the next available account.

---

## 📄 Bank-Grade Financial Statements

Export official financial accounting statements for tax reporting, corporate audits, or personal record-keeping:

- **Corporate PDF Statement Generator**: Generates clean vector PDF documents complete with SPW corporate header, portfolio balance, period net flows, and an itemized transaction ledger.
- **CSV Accounting Export**: Standard tabular export compatible with CoinTracker, Koinly, and Excel.
- **Flexible Filters**: Filter records by Year, Custom Date Ranges, or Transaction Type (*All*, *Incoming*, *Outgoing*, *Stealth*).
- **Share & Save to Device**: Direct integration with the Android system Share sheet and local storage download.

---

## 🏗 Architecture & Tech Stack

```
spwwalletandroid/
├── app/
│   ├── src/main/cpp/             # Native C++ RandomX Engine & JNI Bridge
│   │   ├── CMakeLists.txt        # 16 KB page-aligned CMake configuration
│   │   ├── randomx/              # RandomX C++ source & assembly JIT
│   │   └── randomx-jni.cpp       # JNI bindings for RandomX VM
│   └── src/main/java/com/ryanshelby/spw/wallet/
│       ├── data/                 # Room Database, Remote RPC Client, Repositories
│       ├── mining/               # Native Mining Engine, Manager, Block Builder
│       ├── nfc/                  # NFC Tap-to-Pay Host Card Emulation
│       ├── security/             # Keystore Encryption, SPWCrypto, PDF/CSV Exporters
│       └── ui/                   # Jetpack Compose Screens, Themes, & Components
```

*   **Language**: Kotlin 2.0+ & C++17 (NDK 26.1)
*   **UI Framework**: Jetpack Compose & Material 3
*   **State & Concurrency**: Kotlin Coroutines, StateFlow, Room Flow
*   **Local Storage**: Encrypted Room DB & AndroidX DataStore
*   **Cryptography**: BouncyCastle ECDSA/ECDH (secp256k1 & secp256r1), RandomX PoW JNI, SHA-256, Keystore AES-256-GCM

---

## 🚀 Installation & Building

### Prerequisites
*   Android Studio Ladybug (2024.2.1+) or newer
*   Android SDK 35+ / API 24 minimum
*   Android NDK `26.1.10909125`
*   Java JDK 17

### Build from Source
```bash
# 1. Clone repository
git clone https://github.com/MdSagorMunshi/sparrow.git
cd sparrow

# 2. Run unit tests (26+ test cases)
./gradlew testDebugUnitTest

# 3. Build & install on connected device/emulator
./gradlew installDebug
```

---

## 🤝 Contributing

We welcome contributions! Please review our [Contributing Guidelines](CONTRIBUTING.md) and submit pull requests following standard Git branching conventions.

---

## 🔐 Security & Responsible Disclosure

If you discover a security vulnerability, please do NOT open a public issue. Email `ryn@disr.it` directly with steps to reproduce.

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

<div align="center">
  <b>Developed with ❤️ by <a href="https://github.com/MdSagorMunshi">Ryan Shelby (@MdSagorMunshi)</a></b>
</div>
