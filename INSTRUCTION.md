# Family Emergency Vault — User & Administration Instructions Manual 🛡️🔐

A comprehensive, step-by-step operational guide for setting up, organizing, and managing personal, legal, financial, and emergency records within the **Family Emergency Vault** application.

---

## Table of Contents
1. [Overview & Security Architecture](#1-overview--security-architecture)
2. [First-Time Setup & Account Registration](#2-first-time-setup--account-registration)
3. [Master Catalog of Documents & Information](#3-master-catalog-of-documents--information)
   - [A. Personal Identity Documents](#a-personal-identity-documents)
   - [B. Legal & Estate Documents](#b-legal--estate-documents)
   - [C. Financial & Banking Assets](#c-financial--banking-assets)
   - [D. Family Medical & Health Records](#d-family-medical--health-records)
   - [E. Important Emergency Contacts](#e-important-emergency-contacts)
   - [F. Digital Assets & Critical Credentials](#f-digital-assets--critical-credentials)
4. [Step-by-Step Instructions: How to Add & Manage Records](#4-step-by-step-instructions-how-to-add--manage-records)
5. [Emergency Protocols & Claim Roadmaps](#5-emergency-protocols--claim-roadmaps)
6. [Role-Based Access & Custody Handover](#6-role-based-access--custody-handover)
7. [Security Best Practices & Data Privacy](#7-security-best-practices--data-privacy)

---

## 1. Overview & Security Architecture

The **Family Emergency Vault** is designed to solve a critical real-world problem: *In times of sudden crisis, medical emergency, or incapacitation, family members often struggle to find crucial bank accounts, insurance policies, property titles, or legal wills.*

### Key Architectural Pillars
- **100% Offline-First Storage:** All data stays directly on your smartphone in an encrypted local database (Room/SQLite). No external cloud servers store your credentials.
- **On-Device Cryptographic Sandboxing (`AES-128 GCM`):** High-risk identifiers (PAN numbers, bank accounts, policy numbers) are encrypted using dynamic Initialization Vectors (`IV`) via hardware-backed cryptographic providers.
- **Dual-Language Interface:** Seamlessly toggle between **English** and **Hindi (हिन्दी)** at any moment.
- **Fail-Safe Biometric & MPIN Dual Layer:** Biometrics (fingerprint/face) provide convenient quick unlock, backed by a 4-digit Master MPIN and a security recovery question.

---

## 2. First-Time Setup & Account Registration

Follow these steps when launching the application for the very first time:

### Step 1: Open the Application
- Install and launch the app.
- If no account exists, the app directly opens the **Registration Screen** (never locks you out or prompts for biometrics prematurely).

### Step 2: Choose Your Language
- Tap the **[English | हिन्दी]** toggle in the top-right corner according to your preference. All labels and prompts will update instantly.

### Step 3: Enter Legal Identity & Nominee
- **Legal Full Name:** Enter your full name as it appears on official government IDs (e.g., Aadhaar / Passport).
- **Mobile Number:** 10-digit primary phone number.
- **Vault Owner Email:** Your primary email address.
- **Designated Primary Nominee:** Name of your trusted family member (Spouse, Child, Parent, or Brother/Sister).
- **Nominee Relationship:** Select relationship (e.g., Spouse, Son, Daughter, Parent).

### Step 4: Set Master MPIN & Recovery Question
- **4-Digit Master MPIN:** Choose a memorable 4-digit PIN (e.g., `1984`, `4933`). This is your primary vault decryption key.
- **Security Question & Answer:** Select a question (e.g., *"What is your mother's maiden name?"* or *"What was the name of your first school?"*) and provide a secret answer. This allows you to reset your MPIN if forgotten.

### Step 5: Optional Biometric Authentication
- A toggle checkbox **"Enable Fingerprint / Face Unlock"** is provided.
- You may leave it **unchecked** (default) if you prefer entering your 4-digit PIN every time.
- If enabled, you can authenticate with your fingerprint or facial scan on subsequent launches.

### Step 6: Accept Terms & Initialize Sandbox
- Review the local offline custody terms.
- Tap **"Register Vault Account"** (or use **"Quick Demo"** for instant sample data loading).
- Your encrypted vault is initialized, and you are taken directly to the main dashboard.

---

## 3. Master Catalog of Documents & Information

Here is the complete inventory of what documents and records you should record in the vault:

### A. Personal Identity Documents
| Document Type | Crucial Data to Record | Physical Storage Location Note |
|---|---|---|
| **Aadhaar Card** | 12-Digit Aadhaar No., Registered Mobile, Virtual ID (VID) | Bedroom Steel Almirah (Blue Binder) |
| **PAN Card** | 10-Digit Alphanumeric PAN, Tax Jurisdiction | Personal Wallet / Safe File Folder |
| **Passport** | Passport No., Expiry Date, Issue Date, Visa Details | Bank Safe Deposit Box / Home Vault |
| **Driving License** | DL No., Valid Till Date, Issuing RTO | Vehicle Glove Box / Wallet |
| **Voter ID Card** | EPIC Number, Assembly Constituency | Home Document Drawer |
| **Birth & Marriage Certificates** | Registration No., Registrar Office, Issue Date | Family Master Document Binder |

### B. Legal & Estate Documents
| Document Type | Crucial Data to Record | Notes & Purpose |
|---|---|---|
| **Registered Will & Testament** | Date of Registration, Sub-Registrar Office, Name of Executor | Prevents legal disputes. Note physical lawyer details. |
| **Power of Attorney (PoA)** | General or Medical PoA, Authorized Agent Name | Grants decision-making power during medical incapacitation. |
| **Property Deeds & Titles** | Sale Deed No., Khata / Khasra No., Registry Date | Title papers for apartment, agricultural land, or house. |
| **Bank Locker Details** | Bank Name, Branch, Locker No., Key Location | Record joint locker holders and safe key location code. |
| **Lease & Tenancy Contracts** | Tenant Name, Deposit Amount, Agreement Expiry | Important for regular rental income continuity. |

### C. Financial & Banking Assets
| Asset Category | Details to Include | Nominee Requirement |
|---|---|---|
| **Bank Savings / Current Accounts** | Bank Name, Account Number, IFSC Code, Branch | Verified Nominee Name & Relationship |
| **Fixed / Recurring Deposits (FD/RD)** | Deposit Certificate No., Maturity Date, Amount | Ensure auto-renewal or nominee transfer instructions. |
| **Term Life Insurance** | Policy No., Insurer (LIC, HDFC Life, etc.), Sum Assured | Primary and Secondary Nominees, Claim helpline |
| **Health Insurance (Mediclaim)** | Policy / TPA Card No., Network Hospitals, Cashless Toll-Free | Keep accessible for immediate hospital admission. |
| **Demat & Stock Trading Accounts** | Broker (Zerodha, Groww), DP ID, Client ID, BO-ID | Nominee verification status on CDSL/NSDL portal. |
| **Mutual Fund Folios** | AMC Name, Folio Number, Registrar (CAMS/KFintech) | Nominee declaration status. |
| **EPFO / PF & Pension** | 12-digit UAN (Universal Account Number), Member ID | Vital for provident fund & EPS widow pension claim. |
| **Public Provident Fund (PPF)** | 15-year Account No., Bank/Post Office Branch | Nominee registration form copy. |
| **National Pension System (NPS)** | 12-digit Permanent Retirement Account Number (PRAN) | Nominee percentage distribution. |
| **Outstanding Loans & Liabilities** | Loan Account No., Lending Institution, EMI Due Date | Loan insurance details to prevent family liability. |

### D. Family Medical & Health Records
- **Blood Groups & Rh Factor:** For every family member (Spouse, Children, Elderly Parents).
- **Chronic Conditions & Allergies:** High blood pressure, Diabetes, Penicillin allergies, cardiac history.
- **Regular Medications:** Daily prescriptions, dosage, and local pharmacy contacts.
- **Family Physician / Specialists:** Primary Doctor, Cardiologist, Pediatrician with mobile numbers.

### E. Important Emergency Contacts
Organize these contacts by priority level:
1. **Priority 1 (Immediate Next of Kin):** Spouse, Adult Children, Parents, Siblings.
2. **Priority 2 (Professional Advisors):** Chartered Accountant (CA), Family Lawyer, Insurance Agent/RM, Demat Broker.
3. **Priority 3 (Institutional):** Local Police Station, Nearest Hospital Ambulance, Fire Service, Bank Relationship Manager.

### F. Digital Assets & Critical Credentials
- **Password Manager & Master Key Binder Location:** Location of the physical sealed envelope containing recovery codes.
- **Primary Email Recovery Accounts:** Linked secondary phone number and recovery email address.
- **Cloud Backup Drives:** Folder links for Google Drive, iCloud, OneDrive, or local backup hard drives.

---

## 4. Step-by-Step Instructions: How to Add & Manage Records

### Step 1: Unlock the Vault
1. Launch the app.
2. Enter your **4-Digit Master MPIN** on the secure keypad or tap the **Biometric Scan** button.
3. The vault will decrypt and display your main Dashboard.

### Step 2: Initiate a New Entry
- On the Dashboard, tap the **`+` (Floating Action Button)** located at the bottom-right corner.

### Step 3: Choose the Asset Category
Select the appropriate category chip:
- `DOCUMENT` — for IDs, Passports, Driving Licenses, Certificates.
- `PROPERTY` — for Land, Flats, Commercial shops, deeds.
- `LOCKER` — for Bank Safe Deposit lockers.
- `BANK` — for Savings, Current accounts, FDs, RDs.
- `INSURANCE` — for Life, Term, Health, Motor covers.
- `INVESTMENT` — for Mutual funds, Demat, PPF, NPS, Gold.
- `LIABILITY` — for Home, Car, or Education loans.

### Step 4: Fill in the Record Fields
1. **Title:** Enter a descriptive name (e.g., *"HDFC Salary Account"* or *"Father's LIC Term Cover"*).
2. **Owner / Holder Name:** Name of the individual who owns this asset.
3. **Institution / Authority:** e.g., *"State Bank of India"*, *"LIC of India"*, *"UIDAI"*, *"Max Bupa"*.
4. **Account / Policy / ID Number:** Enter the official number (e.g., `501002341234`, `1234-5678-9012`).
5. **Primary Nominee & Relationship:** Enter the nominee's full name and relation (e.g., *"Priya Sharma (Spouse)"*).
6. **Nominee Verified Checkbox:** Mark as verified if verified on the bank/institution portal.
7. **Physical Binder Location:** Specify exactly where the original document is stored (e.g., *"Master Bedroom Wardrobe - Top Shelf, Red Leather Folder"*).
8. **Digital Location / Drive Link:** Enter cloud backup path or internal folder note.
9. **Attach Document Photo / Scan (New):** Tap *"Attach Photo (Card / Certificate / Policy)"* to attach a secure on-device photo of your Aadhaar card, PAN, Insurance Card, or Cheque leaf.
10. **Remarks & Custom Details:** Add IFSC code, customer service numbers, or special clauses.

### Step 5: Save & Verify Encryption
- Tap **"Save Record"**.
- The record is encrypted on-device. By default, sensitive numbers appear masked (`•••• 4321`) for visual privacy until tapped.
- If a document photo is attached, a visual badge `📎 Document Photo Attached` is shown directly on the item card.

---

### Step 6: Instant Hospital Triage with Family Medical Emergency Card (New)
1. In any medical emergency or hospital admission, open the app and tap **"🩺 Family Emergency Medical Card"** right on the Dashboard.
2. The card displays:
   - **Family Blood Groups** (e.g., O+, B+, A+) for all registered family members.
   - **Allergies & Critical Medical Notes** (e.g., Penicillin allergy, Hypertension).
   - **Health Insurance & Cashless TPA Policy Numbers** for instant cashless admission.
   - **1-Tap Direct Dial** for Family Doctor (`👨‍⚕️ Doctor`) and National Ambulance (`🚑 108`).
3. Tap **"Copy Details"** or **"Share / Print"** to send the triage summary directly to hospital reception or family members via SMS or WhatsApp.

---

## 5. Emergency Protocols & Claim Roadmaps

The vault provides a structured **Emergency Checklist** segmented into three chronological phases:

### Phase 1: First 24 Hours (Immediate Response)
- [ ] Inform closest family members and legal executor.
- [ ] Locate Medical Records, Blood Group info, and Health Insurance cashless TPA cards.
- [ ] Obtain official Medical Attendant Certificate / Death Certificate copies from hospital/municipal corporation.
- [ ] Secure physical keys, wallets, smartphones, and the Master Document Binder.

### Phase 2: First 7 Days (Securing & Reporting)
- [ ] Contact the Family Lawyer and review the Registered Will.
- [ ] Notify Employer HR regarding provident fund settlement, gratuity, and group life insurance.
- [ ] Inform the Chartered Accountant (CA) to safeguard active financial liabilities and tax obligations.
- [ ] Lock access to digital credit cards to prevent fraudulent automated recurring debits.

### Phase 3: First 30 Days (Asset Transfers & Settlement)
- [ ] Initiate Term Life Insurance death claim with original policy documents and death certificate.
- [ ] File bank nominee settlement applications for savings accounts, FDs, and lockers.
- [ ] Submit transmission request forms to Demat depository (CDSL/NSDL) and Mutual Fund registrars.
- [ ] Claim EPFO pension (Form 10D / 20) and PPF proceeds.

---

## 6. Role-Based Access & Custody Handover

The vault includes built-in role perspectives to simulate custody handovers:

1. **Owner:** Unrestricted administrative access to view, edit, mask/unmask, or wipe records.
2. **Spouse:** Immediate access to emergency contacts, bank accounts, and domestic checklists.
3. **Dependent:** Streamlined view focusing on medical instructions, doctor contacts, and immediate safety guidelines.
4. **Executor:** Guided legal roadmap that unlocks specific asset locations during probate or succession proceedings.
5. **Advisor (CA / Lawyer):** Read-only auditing mode where sensitive account numbers remain hashed or masked.

### Emergency Access Request Protocol (Dead-Man / Delay Window)
- If a designated nominee requests emergency vault access on their device, a **48-Hour Delay Window** is triggered.
- During these 48 hours, the Owner can cancel the request if it was accidental.
- If unrevoked after 48 hours, essential emergency records are released to the authorized nominee.

---

## 7. Security Best Practices & Data Privacy

1. **Physical Binder Pairing:**
   - Always keep a physical paper binder in a secure, fireproof home safe.
   - Use the **Physical Location** field in the app to document exact binder locations.
2. **No Cloud Exposure:**
   - Your MPIN and sensitive data never travel across the internet. Never share your 4-digit Master MPIN with casual acquaintances.
3. **Regular Audits:**
   - Every 6 months, review your bank accounts and demat folios in the vault to ensure nominee names are up to date.
4. **App Lock Timeout:**
   - The application automatically locks upon minimization or 5 minutes of inactivity to protect your data from shoulder surfing.

---
*Family Emergency Vault — Preparedness brings peace of mind to the ones you cherish.*
