# SPARROW - Unofficial Mobile Wallet for SPW NETWORK

A production-grade unofficial mobile wallet for the Sparrow (SPW) Network. This Android application features dual ECDSA/ECDH stealth address transfers, real-time RPC node synchronization, and encrypted local storage.

## Features

- **Stealth Transfers**: Privacy-focused dual ECDSA/ECDH stealth address transactions.
- **Encrypted Local Storage**: Your private keys never leave your device.
- **Biometric Security**: Protect your wallet with fingerprint/face unlock and a custom 6-digit PIN.
- **Decoy Wallet**: Configure a decoy PIN to open a fake wallet or wipe your device on duress.
- **Real-Time RPC**: Synchronize seamlessly with SPW Network nodes.

## Developer & Contact

**Developer:** Ryan Shelby  
**GitHub Repository:** [MdSagorMunshi/SPARROW](https://github.com/MdSagorMunshi/SPARROW)  
**Contact:** [ryn@disr.it](mailto:ryn@disr.it)

## Building the App

1. Clone the repository:
   ```bash
   git clone https://github.com/MdSagorMunshi/SPARROW.git
   ```
2. Open the project in Android Studio.
3. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

To build a release version, you will need to generate your own signing key and configure the `release` build type in `app/build.gradle.kts`.

## Contributions

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
