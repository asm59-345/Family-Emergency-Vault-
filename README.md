# Family Emergency Vault 🛡️🔐

> **Secure, offline-first personal financial continuity and emergency succession organizer for families.**  
> Built with modern Kotlin, Jetpack Compose, Material Design 3, Room SQLite, and hardware-backed Android KeyStore AES-256-GCM encryption.

---

## 1. Project Overview & Problem Statement

During unexpected family emergencies, sudden hospitalizations, or untimely deaths, surviving family members frequently face compounding crises:
- **Frozen or Unknown Accounts**: Nominees often do not know where bank accounts, insurance policies, demat folios, locker keys, or title deeds are located.
- **Critical Timeline Delays**: Statutory deadlines (e.g., insurance claim intimation Form 3707 within 24–48 hours, death certificate registration within 21 days under the RBD Act) lapse due to lack of immediate standard operating procedures (SOPs).
- **Nominee Discrepancies**: Under Indian law (e.g., Supreme Court rulings, RBI Master Directions, and the Indian Succession Act), a nominee is only a temporary custodian/trustee, not the absolute owner, often leading to protracted disputes between legal heirs and nominees.
- **Cloud Security Risks**: Uploading master financial portfolios, locker locations, and identity records to third-party cloud servers risks data harvesting, server breaches, and credential stuffing.

**Family Emergency Vault** solves this as a **100% offline, on-device digital continuity vault**. It allows a family head or individual to record vital assets, documents, and emergency procedures locally with hardware-backed encryption, role-based masking, and a controlled emergency access workflow that protects family members during a crisis without compromising day-to-day privacy.

---

## 2. Key Implemented Features

1. **Category-Based Asset & Liability Ledger**:
   - Organizes household records into 6 statutory categories: `BANK`, `INSURANCE`, `INVESTMENT`, `PROPERTY`, `LOCKER`, and `LIABILITY`.
   - Tracks primary and secondary nominees, nominee verification flags, physical binder locations, digital links, and notes.
2. **Nominee Gap Audit Engine**:
   - Automatically cross-checks all 6 categories to detect accounts lacking designated nominees.
   - Highlights gaps with actionable direct-edit buttons and cites statutory guidelines (RBI Master Directions on deceased depositor claims, IRDAI claim settlement norms).
3. **Hardware-Backed Authenticated Document Encryption (`FEV_ENC1`)**:
   - Document attachments captured via camera or Android Photo Picker are encrypted with AES-256-GCM via the Android KeyStore before touching internal storage (`filesDir/vault_documents/`).
   - Uses atomic file writes (`.tmp` -> disk sync -> rename) to prevent file corruption during crashes.
   - Automatically detects ciphertext tampering via GCM authentication tag verification.
4. **PBKDF2-Encrypted Passphrase Backups**:
   - Exports complete vault state using PBKDF2-HMAC-SHA256 (65,536 iterations) with AES-256-GCM encryption and random salt.
   - Transactional restore with atomic rollback (`database.withTransaction`) to prevent partial or corrupted record imports.
   - Explicit unrecoverable-passphrase warning preventing accidental data lockouts.
5. **Role-Based Privilege Sandboxing (RBAC)**:
   - Supports 5 distinct roles: `OWNER`, `SPOUSE`, `DEPENDENT`, `EXECUTOR`, and `ADVISOR`.
   - Sensitive identifiers (`numberOrId`, document attachments) are masked (e.g., `••••••••`) for secondary roles until emergency access is officially released.
6. **Controlled Emergency Access Workflow**:
   - Family members or executors can submit emergency access requests with reason tracking.
   - Approvals are strictly restricted to the `OWNER`; unauthorized attempts by secondary roles are blocked and logged.
   - Revocation can be initiated immediately by `OWNER` or `SPOUSE`.
7. **Offline Bilingual Emergency Succession Guide**:
   - 100% on-device statutory reference in English and Hindi for RBI depositor claim procedures, death certificate timelines, locker operations, Demat transmissions, and motor vehicle succession.
   - Zero external HTTP network calls or cloud dependencies.
8. **1-Tap Emergency SOS Direct Dial**:
   - High-contrast direct dialers for national emergency (112), medical trauma (108), maternal/child care (102), and national health helpline (1075).
9. **Dual Household Isolation (Real vs. Demo Mode)**:
   - Fully isolated sample demo dataset (`userId = "DEMO_HOUSEHOLD"`) for safe walkthroughs without contaminating real family records.

---

## 3. Technology Stack & Project Structure

- **Language**: Kotlin 2.0 (Kotlin DSL for Gradle)
- **UI Framework**: Jetpack Compose (BOM 2024+) with Material Design 3
- **Local Persistence**: AndroidX Room 2.6.1 with SQLite and Kotlin Symbol Processing (KSP)
- **Cryptography**: Android KeyStore (`AndroidKeyStore`), AES-256-GCM (`Cipher`), PBKDF2 (`SecretKeyFactory`), HMAC-SHA256
- **Architecture**: Clean Architecture / MVVM with Kotlin Coroutines and StateFlow
- **Testing**: Robolectric 4.14, JUnit 4, Roborazzi Screenshot Testing

```
Family-Emergency-Vault-/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml          # App manifest (single exported MainActivity)
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt          # UI screens, dialogs, and navigation
│   │   │   │   ├── data/
│   │   │   │   │   ├── AppDatabase.kt       # Room database declaration, migrations, and seeding
│   │   │   │   │   ├── Dao.kt               # Room DAOs (VaultItem, Dependents, Contacts, Audit, etc.)
│   │   │   │   │   ├── Entities.kt          # Database entities
│   │   │   │   │   └── Repository.kt        # Repository abstraction layer
│   │   │   │   ├── ui/
│   │   │   │   │   ├── VaultViewModel.kt    # UI state, role gating, and backup/restore
│   │   │   │   │   ├── SubtleCrypto.kt      # Android KeyStore AES-256-GCM cryptographic engine
│   │   │   │   │   ├── AuthScreens.kt       # Local registration and MPIN authentication UI
│   │   │   │   │   └── theme/               # Color, Typography, and Theme definitions
│   │   │   │   └── util/
│   │   │   │       ├── DocumentStorageManager.kt  # Authenticated FEV_ENC1 file manager
│   │   │   │       ├── EmergencyKnowledgeBase.kt  # Offline statutory succession guide
│   │   │   │       ├── EnterpriseServerGateway.kt # Local device auth gateway (PBKDF2-HMAC-SHA256)
│   │   │   │       └── CsvImporter.kt             # Offline CSV parsing and duplicate validation
│   │   │   └── res/
│   │   │       ├── xml/
│   │   │       │   ├── backup_rules.xml           # Excludes private DB & vault_documents
│   │   │       │   └── data_extraction_rules.xml  # Excludes cloud auto-backup
│   │   │       └── drawable/                      # Static image assets and icons
│   │   └── test/
│   │       └── java/com/example/
│   │           ├── ExampleRobolectricTest.kt # 27 automated JVM tests for DB, crypto, RBAC, backups
│   │           ├── ExampleUnitTest.kt        # Unit tests
│   │           └── GreetingScreenshotTest.kt # Roborazzi UI snapshot verification
│   └── build.gradle.kts                     # App module build and dependency configuration
├── public/
│   ├── index.html                           # Web Companion demo & SOP planning guide (Alpine.js)
│   └── app-debug.apk                        # Developer preview debug APK build
├── architecture.md                          # Comprehensive technical architecture specification
└── README.md                                # This document
```

---

## 4. Prerequisites & Build Commands

### Prerequisites
- **JDK**: Java 17 or Java 21
- **Android SDK**: Compile SDK 36 (Android 16 DP), Min SDK 24 (Android 7.0 Nougat)
- **Gradle**: 9.3.1 (pre-configured)

### Build & Test Commands
Run the following commands from the project root:

```bash
# 1. Compile the project
gradle compileDebugKotlin

# 2. Run the automated test suite (Robolectric + Unit tests)
gradle :app:testDebugUnitTest

# 3. Assemble the Developer Preview Debug APK
gradle assembleDebug

# 4. Dry-run verify the Release Build configuration
gradle :app:assembleRelease --dry-run
```

*Note: In this container environment, use `gradle` directly (the wrapper `./gradlew` is not used).*

---

## 5. Onboarding & Demo Mode

### Fresh User Onboarding
Upon first launch, a new installation starts in an unseeded, zero-data state:
1. Displays the welcome onboarding screen explaining offline storage.
2. Prompts the household head to create a local account with Full Name, Email, Password, and a 4-digit Master MPIN.
3. The dashboard initializes with **0% Readiness** and zero financial items.

### Demo Mode Walkthrough
Users can explore the app's features using safe sample records:
1. Tap the **"Explore Demo"** button on the onboarding screen or in settings.
2. Populates 8 sample portfolio records (HDFC Bank, LIC Jeevan Anand, Zerodha Demat, SBI Locker, etc.) tagged with `userId = "DEMO_HOUSEHOLD"`.
3. Demonstrates nominee gap flags, SOP timelines, and category breakdowns.
4. Tapping **"Exit Demo Mode"** cleanly purges all `DEMO_HOUSEHOLD` records from Room without affecting any real user data.

---

## 6. Backup, Export & Restore Workflow

### Encrypted Backup Export
1. Navigate to Settings -> **"Export Vault Backup"**.
2. Select **"AES-256 Encrypted"**.
3. Enter a strong secret passphrase.
4. **Unrecoverable Passphrase Warning**:
   > ⚠️ **CRITICAL WARNING**: This passphrase CANNOT be recovered or reset by anyone. If you lose or forget this passphrase, this backup file will be permanently unrecoverable.
5. The app derives a 256-bit AES key using PBKDF2-HMAC-SHA256 (65,536 iterations, 16-byte random salt, 12-byte IV) and produces an encrypted JSON envelope.

### Backup Restore
1. Navigate to Settings -> **"Restore Vault from Backup"**.
2. Paste the encrypted JSON string and enter the master passphrase.
3. The app authenticates the payload, validates compatibility (`version <= 1`), skips duplicates against existing records, and commits records within a database transaction (`database.withTransaction`).
4. Incorrect passphrases, corrupt ciphertext, or schema errors trigger an immediate transaction rollback with zero records imported.
5. **Role Gating**: Restores are strictly restricted to `OWNER` and `SPOUSE` roles.

---

## 7. Security Model & Trust Boundaries

| Component | Security Mechanism | Threat Addressed |
| :--- | :--- | :--- |
| **Local Account Passwords** | PBKDF2-HMAC-SHA256 (600,000 iterations) + 16-byte salt + auto-upgrade | Offline dictionary / GPU brute-force cracking if database is extracted |
| **Document Attachments** | Authenticated AES-256-GCM via `AndroidKeyStore` (`FEV_ENC1`) | Direct file inspection if device storage is accessed |
| **Database Encryption** | Private internal app storage (`context.filesDir`); field-level masking in UI/exports | Unauthorized shoulder-surfing and export inspection |
| **Passphrase Backups** | PBKDF2-HMAC-SHA256 (65,536 iterations) + AES-256-GCM | Plaintext exposure during backup transfer or cloud sharing |
| **Cloud Auto-Backup** | Excluded via `data_extraction_rules.xml` and `backup_rules.xml` | Unintended Google Drive synchronization of KeyStore-bound data |
| **Role-Based Access** | Role checking at ViewModel and Repository layers | Unauthorized access by secondary household members |
| **Emergency Access** | Owner-only approval authority; 48-hour delay window; Owner/Spouse revocation | Premature or hostile vault takeover |

### Known Security Limitations
- **No Cloud Synchronization**: By design, records do not sync across devices automatically; manual encrypted backup export is required.
- **Physical Device Compromise**: On a rooted device with root access, memory inspection of decrypted strings in RAM is possible while the app is unlocked.

---

## 8. Web Companion vs. Native Android App Boundary

| Dimension | Native Android Application (`app/`) | Web Companion (`public/index.html` / `index.html`) |
| :--- | :--- | :--- |
| **Platform** | Android OS (APK / Jetpack Compose) | Any Modern Web Browser (Alpine.js / Tailwind) |
| **Purpose** | **Production Vault** for real family records | **Demo & SOP Planning Guide Only** |
| **Real Data Entry** | **Enabled** (Protected by PIN, Biometrics & RBAC) | **Disabled** (Safety alert modal blocks data entry) |
| **Data Storage** | Private SQLite (`Room`) + Encrypted Files | Browser `localStorage` (Plaintext) |
| **Encryption** | Hardware `AndroidKeyStore` AES-256-GCM | None (Demo records only) |
| **Sync** | None (100% Offline) | None (Does NOT sync with Android phone) |

*Important: Never enter real bank account numbers or confidential credentials on the web companion.*

---

## 9. Current Verification Status & Test Evidence

- **Verification Date**: October 22, 2026
- **Test Framework**: Robolectric 4.14, JUnit 4, Roborazzi
- **Total Tests Executed**: **40**
- **Passed**: **40 (100%)** | **Failed**: 0 | **Skipped**: 0

```
Gradle Test Run :app:testDebugUnitTest
├── ExampleRobolectricTest (38 tests) - 100% PASS
├── ExampleUnitTest (1 test)          - 100% PASS
└── GreetingScreenshotTest (1 test)   - 100% PASS
BUILD SUCCESSFUL in ~1m 17s
```

### Verified Build Artifacts
- **File**: `public/app-debug.apk` & `app-debug.apk`
- **Application ID**: `com.aistudio.familyemergencyvault.kndprs`
- **Version**: `versionCode 1`, `versionName "1.0"`
- **Exact Size**: `30,199,211 bytes` (~28.8 MB)
- **SHA-256 Checksum**:
  `3b1aa518b448404113905ec41f4c7d570a539b47678da301b8a4e8ea0f9b2738`
- **Archive Integrity**: Verified via `unzip -t` (No errors detected across 6 classes.dex files and assets).

---

## 10. Pending Physical-Device Checks

The following verifications cannot be performed inside containerized cloud JVM environments and must be completed on physical hardware prior to production release:
1. **BiometricPrompt Hardware Interaction**: Physical fingerprint and facial recognition sensors, prompt cancellation, lockout after 5 failed attempts, and fallback to MPIN.
2. **OEM Hardware KeyStore Retention**: Verification that the `fev_vault_hardware_key` alias persists across device reboots and major Android OS updates on various OEM hardware (Samsung Knox, Google Pixel Titan M, Xiaomi, etc.).
3. **StrongBox Availability**: Testing on devices supporting StrongBox Keymaster to evaluate whether `setIsStrongBoxBacked(true)` can be enabled with dynamic software fallback.
4. **Camera Resolution & Aspect Ratio**: Testing document capture across varied hardware camera sensors and lighting conditions.

---

## 11. Debug / Preview Build vs. Production Release Distinction

| Attribute | Preview / Debug Build (`app-debug.apk`) | Production Release Build |
| :--- | :--- | :--- |
| **Signing Key** | Insecure default `debug.keystore` | Secure Organizational Release Keystore |
| **Debuggable Flag** | `android:debuggable="true"` (Inspectable) | `android:debuggable="false"` (Enforced) |
| **ProGuard / R8** | Disabled | Enabled (`proguard-android-optimize.txt`) |
| **Distribution** | Sideloading for developer evaluation only | Google Play Store / Official APK Distribution |

*Warning: The `app-debug.apk` included in the repository is strictly for developer preview and testing. It must never be published as a production release.*

---

## 12. Verified UI Assets & Visual Showcase

The following assets are verified and present in `app/src/main/res/drawable/` and `public/`:
- `img_vault_hero_banner_1789194543546.jpg`: Hero banner showcasing family continuity protection.
- `img_app_mobile_showcase_1789194560982.jpg`: Mobile interface showcase showing the dashboard.
- `img_app_features_mockup_1789322562671.jpg`: Feature showcase highlighting nominee gap audits and SOP timelines.
- `img_family_continuity_shield_1789320119569.jpg`: Family continuity shield emblem.
- `img_emergency_documents_organizer_1789320102141.jpg`: Physical and digital binder organizer.

---

## 13. Security Consistency & Future Work

### Password Hashing Specification & Auto-Upgrade (Implemented)
- **Active Scheme**: PBKDF2-HMAC-SHA256 (`PBKDF2WithHmacSHA256`) with per-password 16-byte random salts and 600,000 iterations (OWASP recommendation).
- **Self-Contained Format**: `$pbkdf2-sha256$<iterations>$<base64Salt>$<base64Hash>` with separate independent salts for passwords and security answers.
- **Bounded Work Factor & DoS Defense**: Validates iterations (`10,000 <= iterations <= 2,000,000`) and validates the derived key length (32 bytes = 256 bits) prior to derivation.
- **Zero Plaintext Persistence (P0 Enforcement)**: Plaintext passwords and security answers are never stored in `SharedPreferences`, RAM properties, loggers, or audit trails. Startup purge cleans any legacy plaintext preferences.
- **Credential-Verifier UI Redaction (P1 Enforcement)**: Raw password hashes, salts, and JWT token signatures are completely redacted in UI diagnostic inspectors (`AdminAuditDashboard.kt`).
- **Memory Scrubbing**: Password character arrays and PBEKeySpec buffers are explicitly cleared in `finally` blocks. All derivations run asynchronously on `Dispatchers.IO`.
- **Seamless Backward Compatibility**:
  - Legacy single-round salted SHA-256 accounts verify cleanly via constant-time comparison.
  - Upon successful return login, the account is transparently upgraded to PBKDF2 with a fresh 16-byte salt, database record updated, and `"Password Hash Upgraded"` audit event recorded.
  - Zero account resets, zero data loss, zero disruption to existing users.
- **Verification Evidence**: 37 automated tests passing in `gradle :app:testDebugUnitTest`. Dedicated tests verify PBKDF2 hashing, salt separation, iteration bounding, plaintext credential purge, wrong-password rejection, malformed record resilience, and security question password recovery.

### Base64 Attachment Support Classification
- `VaultItem.documentPhotoBase64` is an **isolated legacy backward-compatibility field** from Phase 1.
- All active production document saves use `DocumentStorageManager.kt` via `attachmentUri`.
- The legacy field is retained solely to prevent deserialization failures for pre-existing database rows.

### Future Roadmap
1. Encrypted Peer-to-Peer Wi-Fi Direct export for local device transfer without third-party cloud apps.
2. Hardware-tested StrongBox fallback integration on physical devices.
