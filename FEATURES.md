# SPARROW Mobile Wallet - Features History

### v2.1.0 (2026-08-30)
- **Financial-Grade Institutional Interface Redesign**:
  - Ground-up overhaul replacing legacy cyberpunk neon and violet gradients with a calm, institutional visual language inspired by Coinbase and high-end banking apps.
  - Deep charcoal and graphite foundation (`#0C0E12`, `#14171F`) with hairline structural borders (`#1E2430`) and soft ivory typography (`#F9FAFB`).
- **Ultra-Clean Monochrome & Restrained Emerald Palette**:
  - High-contrast stark white primary action buttons (`ButtonPrimary = #FFFFFF`, `ButtonPrimaryText = #0C0E12`) for transfer authorization, address sharing, and key confirmations.
  - Emerald green (`#10B981`) strictly restrained to positive financial gains (`+SPW`), confirmed status pills, and small live node connectivity dots.
- **Financial Polarity for Spends & Outgoing Transactions**:
  - Outgoing spend transactions explicitly displayed in financial crimson red (`SemanticError = #EF4444`) with minus prefixes and tinted action icons across Explorer/History, Dashboard Recent Activity, and Transaction Details.
  - Clear visual distinction between incoming (`+SPW` green) and outgoing (`-SPW` red) transfers, including dual-key stealth transactions.
- **Physics-Based Motion & Tactile Feedback System**:
  - Natural spring curves (`BouncySpring`, `StandardSpring`) replacing linear easing.
  - Tactile interactive scale feedback (`Modifier.bouncyClickable`) on all interactive cards, chips, and numpad buttons.
  - Rolling ticker mechanical digits (`AnimatedBalanceCounter`) with `fontFeatureSettings = "tnum"` tabular numbers preventing jitter during balance updates.
  - Sequential staggered entrance animations (`Modifier.staggeredEntrance`) on list cards and history items.
- **Bone-Matching Skeleton Loading States**:
  - Subtle sweeping shimmer loaders (`SkeletonShimmer`) matching exact transaction row geometry to eliminate layout shifts during RPC network sync.
- **Institutional Key & Scanner Utilities**:
  - High-contrast isolated QR plate with instant haptic clipboard feedback and stealth key disclosure.
  - Minimalist viewfinder reticle with hairline white corner brackets in QR camera scanner.
  - Spring-driven security PIN numpad with pure white filled dot indicators.

### v2.0.0 (2026-08-28)
- Wallet notifications + Mining UI (preview)
- Wallet transaction push notifications for received and sent tokens.
- Modern in-app notification permission request flow.
- Added Notification Settings page to customize and toggle incoming, outgoing, and mining alerts.
- Added Mining screen UI scaffolding (Note: Active mining logic is disabled pending native RandomX algorithm integration).

### v1.0.0 (2026-08-28)
- Initial public release for the SPW Network.
- Dual ECDSA/ECDH stealth address transfers.
- Encrypted local hardware-backed storage for private keys.
- Biometric security (Fingerprint / Face Unlock) with PIN fallback.
- Duress protocol with Decoy Wallet PIN configuration.
- Real-Time RPC node synchronization.
- Encrypted local keystore security.
- Comprehensive setting & UI theme engine.
