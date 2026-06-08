# Family Emergency Vault 🛡️🔐

[![Android Build Code](https://img.shields.io/badge/Android-Kotlin-orange.svg?style=flat&logo=android)](https://kotlinlang.org)
[![Aesthetic-M3](https://img.shields.io/badge/Design-Material--3-violet.svg?style=flat)](https://m3.material.io)
[![Database-Room](https://img.shields.io/badge/Storage-Room--SQL-blue.svg?style=flat)](https://developer.android.com/training/data-storage/room)
[![Security-AES--GCM](https://img.shields.io/badge/Security-AES--GCM-green.svg?style=flat)](https://developer.android.com/reference/javax/crypto/Cipher)

The **Family Emergency Vault** is a secure, offline-first personal financial continuity and emergency access organizer built for families. Developed entirely in modern Kotlin with Jetpack Compose and Material 3, it bridges the information gap during critical times. This vault allows a user to register high-fidelity assets, documents, and contacts while offering structured, delayed, and role-based custody access to designated family members and trusted executors.

---

## 🌟 Core Architecture & Key Features

### 1. ⚙️ Cryptographic Sandboxing (`AES/GCM/NoPadding`)
All sensitive information is encrypted locally using the **SubtleCrypto** engine. 
* **State-of-the-art symmetric encryption**: Employs AES-128 GCM with dynamic IVs derived via `SecureRandom` block providers.
* **Separation of Concerns**: Encrypted data payloads (`Base64` formatted `IV:CipherText`) are securely stored in the local SQLite sandboxed Room database. 

### 2. 🎭 Role-Based Privilege Hierarchy (Multi-Custody)
The system supports simulated logins representing different critical emergency contexts:
* **Owner (Rahul)**: Full access to view or modify all assets, contacts, and manage global settings/logs.
* **Spouse (Priya)**: Real-time high-level access with instant dual-signature or manual verify-receipt approvals.
* **Dependent (Ramesh)**: Limited, focus-driven interface highlighting critical medical files and immediate checklist tasks.
* **Executor (Sanjay)**: Zero-visibility sandboxed route that unlocks a guided step-by-step roadmap *only* upon emergency approval.
* **Advisor (Alok, CA)**: Read-only compliance portal where sensitive asset tracking numbers are automatically hashed or masked.

### 3. 🏦 Category-Based Resources Vault
Organize the entire family's assets under a centralized, search-filtered database with fields for nominee status, physical location binders, and digital backup links:
* **Financial Portfolios**: Banks, Demat Investments, Insurance Covers, and Liabilities.
* **Estate & Assets**: Registered Property details, Physical Safe Lockers, and Tax returns.
* **Corporate Details**: Employer HR details, Income streams, and Active subscriptions.

### 4. ⏳ Advanced Emergency Access & Dead-Man Switches
* **Delayed Request Releases**: Family members or Executors can request vault release through a structured workflow.
* **Approval Timeline Simulation**: Incorporates a visual wait timer simulating the mandatory 48-hour delay window, giving the Owner plenty of time to revoke accidental access.
* **Check-In Dormancy Auto-Lock**: Automatically locks the screen layout and logs a dormancy check if there is no user interaction within 5 minutes.

### 5. 📋 Dynamic Rescue Timelines
Actionable items segmented logically into phases mapped for stressful situations:
* **First 24 Hours**: Urgent actions (Funeral directors, nearest hospitals, reporting).
* **First 7 Days**: Immediate estate reporting (Securing physical binders, contacting CA/lawyer).
* **First 30 Days**: Financial transfers (Initiating demat inheritance, bank transfers, and claim tracking flows).

### 6. 📝 Cryptographic Audit Logs
In compliance with digital estate planning standards, an immutable local **audit database trail** monitors every security-sensitive action, including MPIN verification, asset unmasking, role switches, and emergency triggers.

---

## 🛠️ Technology Stack

* **Language**: 100% Kotlin (Modern, expressive, and type-safe async programming).
* **UI Toolkit**: Jetpack Compose (Declarative, reactive components styled with a custom dark Slate primary theme).
* **Local Databases**: Room (Engineered with an efficient KSP compilation layer for custom type converters and clean DAOs).
* **Asynchronous Flow**: Kotlin Coroutines & Cold StateFlow streams enabling real-time UI synchronization.
* **Networking**: OkHttpClient for resilient REST/JSON communication.
* **Designing Standards**: Material Design 3 guidelines featuring dynamic colors, responsive form sheets, and generous touch-target sizing (min 48dp).

---

## 🏗️ Directory Roadmap

The layout aligns with clean architectural patterns:
```groovy
app/src/main/java/com/example/
├── MainActivity.kt               # Central Navigation Hub & Screen Entry Composables
├── data/
│   ├── AppDatabase.kt            # Room Instance Handler
│   ├── Dao.kt                    # Data Access Interfaces for Vault, Checklist, & Auditing
│   ├── Entities.kt               # Secure SQLite Schema Declarations
│   └── Repository.kt             # Clean Data Provider Abstraction Boundary
└── ui/
    ├── AppTermsConsentScreen.kt # Responsive Legal Terms & Cryptographic Signature Agreement
    ├── ArchitectureAndSchemaHub.kt # Tech Reference Specs & Live Database Inspect Utility
    ├── SubtleCrypto.kt           # On-Device AES/GCM Encryption Cipher Engine
    ├── VaultViewModel.kt         # Secure State Repository & Inactivity Autolocks
    └── theme/                    # High-Contrast Type.kt, Color.kt, and Theme.kt Setup
```

---

## 🚀 Accessing the App

### Standard Access Details
1. **Default App-PIN**: `4321` (Master decryption key)
2. **Instant Evaluate Mode**: Starts pre-logged into a rich mock database (10+ pre-filled mock portfolios) to make quick evaluations fast and interactive.
3. **Legal Check**: Requires a simple scroll confirmation and a dynamic, cryptographically signed consent stamp to initialize the secure hardware sandbox.

---

## 📖 Building from Source

To compile the APK and run the application locally on an Android device:

### Prerequisites
* Android Studio (Koala / Ladybug or newer)
* Android SDK 34+
* JDK 17

### Build Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/asm59-345/Family-Emergency-Vault-.git
   cd Family-Emergency-Vault-
   ```
2. Build the project debug APK:
   ```bash
   gradle assembleDebug
   ```
3. Locate your compiled APK:
   ```bash
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🔐 Security & Peace of Mind
This application is designed with **privacy as a human right**. It does not transmit your master recovery phrases, bank login passwords, or MPINs to any external server. All assets and information live inside your device's sandbox.

*Developed with ❤️ to protect corporate estate, digital records, and families.*
