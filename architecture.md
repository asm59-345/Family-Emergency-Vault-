# Family Emergency Vault — System Architecture Documentation (`architecture.md`)

## 1. High-Level Architecture Overview

Family Emergency Vault is built following **Clean Architecture** and **Modern Android MVVM (Model-View-ViewModel)** design patterns. It emphasizes a **Local-First, Zero-Knowledge Privacy** philosophy, ensuring complete offline availability, high responsiveness, and zero dependency on central cloud infrastructure for core survival operations.

```
+---------------------------------------------------------------------------------------+
|                                    PRESENTATION LAYER                                 |
|                                                                                       |
|   [Jetpack Compose UI]                                      [Web Companion / PWA]     |
|   - MainActivity (Single Activity)                          - index.html (Tailwind)   |
|   - AuthScreens (Biometric / PIN)                           - Alpine.js State Store   |
|   - EmergencySosScreen (1-Tap Dialers)                      - Web Ledger Modal & UI   |
|   - MedicalEmergencyCardDialog                              - 2s Confirmation Toast   |
|   - ArchitectureAndSchemaHub                                                          |
|   - ObservabilityScreen                                                               |
+------------------------------------------+--------------------------------------------+
                                           |
                                     UI State / Intents
                                           |
+------------------------------------------v--------------------------------------------+
|                                      VIEWMODEL LAYER                                  |
|                                                                                       |
|   [VaultViewModel]                                                                    |
|   - StateFlow<VaultUiState> (Unidirectional Data Flow)                                |
|   - CoroutineScope (viewModelScope on Dispatchers.Main.immediate)                     |
|   - Search & Category Filter Buffering                                                |
|   - Role-Based Sandboxing Logic (Owner, Executor, Advisor)                            |
+------------------------------------------+--------------------------------------------+
                                           |
                                Repository Interface
                                           |
+------------------------------------------v--------------------------------------------+
|                                    DATA & REPOSITORY LAYER                            |
|                                                                                       |
|   [VaultRepository]                                                                   |
|   - Coordinates local persistence, queries, imports, and exports                      |
|   - Thread offloading via Dispatchers.IO                                              |
|                                                                                       |
|   [Room Database Engine (AppDatabase)]                                                |
|   - VaultItemDao (Financial accounts, insurance, properties, binders)                 |
|   - EmergencyActionItemDao (SOP checklist items: 24h, 7d, 30d)                        |
|   - EmergencyContactDao (Doctors, advocates, CAs, next-of-kin)                        |
|   - DependentFamilyDao (Minors, elders, special care instructions)                    |
|                                                                                       |
|   [Security & Encryption Engine]                                                      |
|   - AndroidX BiometricPrompt (BiometricManager)                                       |
|   - SubtleCrypto / PBKDF2 / AES-GCM (Optional Encrypted JSON Snapshots)               |
+---------------------------------------------------------------------------------------+
```

---

## 2. Layer-by-Layer Architectural Breakdown

### A. Presentation Layer (Jetpack Compose & Web Companion)
1. **Unidirectional Data Flow (UDF)**:
   - Composables observe UI state emitted by `VaultViewModel` as `StateFlow`.
   - UI elements do not mutate database entities directly. Every user interaction emits an intent/event to the ViewModel, which executes business logic and updates the immutable state.
2. **Edge-to-Edge & Accessibility**:
   - Built on Material Design 3 (M3) components (`Scaffold`, `LazyColumn`, `FilledTonalButton`).
   - Strict 48dp minimum touch targets for high-stress usability.
   - Dynamic insets handling (`WindowInsets.systemBars`) ensuring zero obstruction by Android system gesture bars or camera cutouts.
3. **Web Companion Architecture**:
   - Lightweight, dependency-free Alpine.js + Tailwind CSS architecture.
   - 100% offline client-side execution via standard `localStorage` caching.
   - Modular modals for adding, editing, and categorizing ledger entries with a reactive 2-second confirmation toast feedback loop.

### B. ViewModel Layer (`VaultViewModel.kt`)
1. **Lifecycle Safety**:
   - Scoped to Android ViewModel lifecycle, surviving configuration changes (screen rotations, multi-window splits) without data re-fetching.
2. **Role-Based Sandboxing (Access Control Matrix)**:
   - **Primary Owner**: Full read/write access to all entities, attachments, and settings.
   - **Executor Mode**: Unlocks guided emergency SOP checklists and necessary document locations only.
   - **Advisor / CA Mode**: Data is transformed on the fly; sensitive account and policy numbers are automatically masked (e.g., `XXXX-XXXX-3912`) to prevent data leakage during financial audits.

### C. Data & Persistence Layer (`AppDatabase.kt`, `Dao.kt`, `Entities.kt`)
1. **SQLite / Room ORM**:
   - Type-safe, compile-time verified SQL queries via KSP.
   - Pre-seeded with critical Indian emergency numbers (112 National Emergency, 1078 Disaster, 1091 Women Helpline) and the standard 3-phase succession SOP.
2. **Database Entities**:
   - `VaultItem`: Stores category, institution, folio/account number, primary nominee, nominee verification boolean, physical folder location, digital link, and photo/document URI.
   - `EmergencyActionItem`: Stores operational recovery tasks categorized into:
     - *Phase 1 (First 24 Hours)*: Medical death certificate, mortuary release, immediate family alert.
     - *Phase 2 (Days 2 to 7)*: Municipal registration, bank debit freezes, employer notification.
     - *Phase 3 (Month 1)*: Transmission of securities, insurance claim Form 3707 settlement, property succession.
   - `EmergencyContact`: Stores priority contacts, relationship, phone numbers, and professional roles (Doctor, Lawyer, CA, Close Relative).
   - `DependentFamily`: Stores dependents requiring immediate guardianship, ongoing medical prescriptions, or emergency funding.

---

## 3. Security, Privacy & Zero-Knowledge Architecture

1. **Local-First Boundary**:
   - The application does not maintain external analytics servers, telemetry trackers, or cloud user databases.
   - Eliminates threat vectors related to server breaches, credential stuffing, and unauthorized data mining.
2. **Authentication Gate**:
   - Dual-tier authentication: **Hardware-backed BiometricPrompt (Class 3 Strong Biometrics)** or fallback to a locally hashed **6-digit Master Emergency PIN**.
   - Nominee access is guaranteed even in offline hospital basements without SMS 2FA latency.
3. **Zero-Permission Media Handling**:
   - Compliant with modern Android storage permissions. Uses standard Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) and persistent content URI permissions rather than requiring dangerous external storage permissions (`READ_EXTERNAL_STORAGE`).

---

## 4. Concurrency & Performance Architecture

1. **Structured Concurrency with Kotlin Coroutines**:
   - Database operations run exclusively on `Dispatchers.IO` to protect the UI main thread from frame drops or ANR (Application Not Responding) timeouts.
2. **Reactive Streams with Kotlin Flow**:
   - Room DAOs expose `Flow<List<T>>`, streaming instant updates to the ViewModel whenever items are inserted, updated, or deleted.
3. **Safe Memory & Image Management**:
   - Attached document scans and images are decoded using downsampled decoding boundaries, preventing `OutOfMemoryError` (OOM) during memory-constrained situations.

---

## 5. Technology Stack Summary

| Component | Technology / Library | Role |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0+ | Modern, expressive, type-safe development |
| **UI Framework** | Jetpack Compose (BOM 2024+) | Declarative, modern Android UI with Material Design 3 |
| **Local Database** | AndroidX Room (SQLite + KSP) | Embedded, compile-time checked local data persistence |
| **Concurrency** | Kotlin Coroutines & Flow | Asynchronous I/O and reactive data streams |
| **Authentication** | AndroidX Biometric (`BiometricPrompt`) | Secure on-device hardware authentication |
| **Unit Testing** | Robolectric & JUnit 4 | Fast JVM local test suite for Room DAOs and business logic |
| **Web Companion** | HTML5, Tailwind CSS, Alpine.js | Standalone client-side web ledger and cross-platform backup tool |
| **Build System** | Gradle (Kotlin DSL - `.gradle.kts`) | Standardized modular build system |
