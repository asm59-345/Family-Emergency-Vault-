package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FamilyDependent::class,
        ImportantContact::class,
        VaultItem::class,
        EmergencyActionItem::class,
        ClaimRecord::class,
        EmergencyAccessRequest::class,
        AuditLog::class,
        ServerUserRecord::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun familyDependentDao(): FamilyDependentDao
    abstract fun importantContactDao(): ImportantContactDao
    abstract fun vaultItemDao(): VaultItemDao
    abstract fun emergencyActionItemDao(): EmergencyActionItemDao
    abstract fun claimRecordDao(): ClaimRecordDao
    abstract fun emergencyAccessRequestDao(): EmergencyAccessRequestDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun serverUserDao(): ServerUserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vault_items ADD COLUMN attachmentUri TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vault_items ADD COLUMN documentPhotoBase64 TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS server_user_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        email TEXT NOT NULL,
                        fullName TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        passwordHash TEXT NOT NULL,
                        passwordSalt TEXT NOT NULL,
                        mpin TEXT NOT NULL,
                        nomineeName TEXT NOT NULL,
                        nomineeRelation TEXT NOT NULL,
                        securityQuestion TEXT NOT NULL,
                        securityAnswerHash TEXT NOT NULL,
                        digitalCertificateId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastLoginAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        role TEXT NOT NULL,
                        tokenJwt TEXT NOT NULL,
                        deviceFingerprint TEXT NOT NULL,
                        isBiometricEnrolled INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_server_user_records_email ON server_user_records(email)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_server_user_records_userId ON server_user_records(userId)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "family_continuity_vault_db"
                )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_1_2, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance

                // Proactively verify & seed if empty on a background thread
                scope.launch(Dispatchers.IO) {
                    ensureSeeded(instance)
                }

                instance
            }
        }

        suspend fun runVacuum(db: AppDatabase) {
            try {
                db.openHelper.writableDatabase.execSQL("VACUUM")
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error running VACUUM", e)
            }
        }

        suspend fun runWalCheckpoint(db: AppDatabase) {
            try {
                db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error running wal_checkpoint", e)
            }
        }

        suspend fun checkIntegrity(db: AppDatabase): String {
            return try {
                val cursor = db.openHelper.writableDatabase.query("PRAGMA integrity_check")
                var result = "ok"
                if (cursor.moveToFirst()) {
                    result = cursor.getString(0) ?: "ok"
                }
                cursor.close()
                result
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }

        suspend fun ensureSeeded(db: AppDatabase) {
            try {
                val count = db.vaultItemDao().getCount()
                if (count == 0) {
                    seedData(db)
                }
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error ensuring database is seeded", e)
            }
        }

        suspend fun resetToDefaults(db: AppDatabase) {
            try {
                db.vaultItemDao().deleteAll()
                db.familyDependentDao().deleteAll()
                db.importantContactDao().deleteAll()
                db.emergencyActionItemDao().deleteAll()
                db.claimRecordDao().deleteAll()
                db.emergencyAccessRequestDao().deleteAll()
                seedData(db)
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error resetting database to defaults", e)
            }
        }

        suspend fun seedData(db: AppDatabase) {
            // 1. Seed Family Members
            val dependents = listOf(
                FamilyDependent(
                    fullName = "Priya Sharma",
                    relation = "Spouse",
                    dob = "1988-11-20",
                    bloodGroup = "O+",
                    mobile = "+91 98765 43210",
                    email = "priya.sharma@gmail.com",
                    address = "Flat 502, Orchid Heights, Sector 45, Gurgaon, Haryana",
                    dependentStatus = "Co-Owner / Depended",
                    guardianDetails = "N/A",
                    notes = "Has access to physical document cabinet file alpha."
                ),
                FamilyDependent(
                    fullName = "Aarav Sharma",
                    relation = "Son",
                    dob = "2015-06-12",
                    bloodGroup = "A+",
                    mobile = "N/A",
                    email = "N/A",
                    address = "Flat 502, Orchid Heights, Sector 45, Gurgaon, Haryana",
                    dependentStatus = "Dependent Minor",
                    guardianDetails = "Priya Sharma (Mother)",
                    notes = "School record and immunisation papers kept in Cabinet Drawer 2."
                ),
                FamilyDependent(
                    fullName = "Ramesh Sharma",
                    relation = "Father",
                    dob = "1954-04-05",
                    bloodGroup = "B+",
                    mobile = "+91 99112 23344",
                    email = "ramesh.sharma@yahoo.com",
                    address = "House 104, Civil Lines, Jaipur, Rajasthan",
                    dependentStatus = "Dependent Parent",
                    guardianDetails = "N/A",
                    notes = "Under treatment for hypertension. Mediclaim linked."
                )
            )
            dependents.forEach { db.familyDependentDao().insert(it) }

            // 2. Seed Important Contacts
            val contacts = listOf(
                ImportantContact(
                    contactName = "Dr. Sameer Joshi",
                    category = "Family Doctor",
                    phone = "+91 98111 22233",
                    email = "dr.joshi@clinic.com",
                    address = "Joshi Medical Centre, Galleria Road, Gurgaon",
                    priority = "Priority 1 (Nearest)",
                    remarks = "Our primary general physician. Knows father's cardiac history."
                ),
                ImportantContact(
                    contactName = "Alok Mehta (CA)",
                    category = "Chartered Accountant",
                    phone = "+91 93123 45678",
                    email = "alokmehta.ca@co.in",
                    address = "Mehta & Associates, CP, New Delhi",
                    priority = "Priority 2",
                    remarks = "Manages our annual tax filing and corporate returns. Has physical files of IT returns."
                ),
                ImportantContact(
                    contactName = "Sanjay Krishna",
                    category = "Lawyer",
                    phone = "+91 98100 98100",
                    email = "sanjay.krishna@legalpartners.com",
                    address = "Patiala House Courts Chambers, New Delhi",
                    priority = "Secondary",
                    remarks = "Drafted my physical Will. Keeps one signed copy in secure safe."
                ),
                ImportantContact(
                    contactName = "Mahendra Sen",
                    category = "RM/Broker",
                    phone = "+91 91100 11001",
                    email = "mahendra.sen@zerodha.com",
                    address = "DLF Cyber City, Gurgaon",
                    priority = "Secondary",
                    remarks = "RM for Zerodha Portfolio and demat transmission matters."
                )
            )
            contacts.forEach { db.importantContactDao().insert(it) }

            // 3. Seed Vault Items (Bank Accounts, Investments, Insurance, etc. with Indian terminology)
            val vaultItems = listOf(
                // Bank Accounts
                VaultItem(
                    category = "BANK",
                    title = "HDFC Savings Account - Primary",
                    ownerName = "Rahul Sharma",
                    institution = "HDFC Bank",
                    numberOrId = "50100045239123",
                    nomineeName = "Priya Sharma (Spouse)",
                    nomineeRelation = "Spouse",
                    nomineeVerified = true,
                    physicalLocation = "Cabinet A, File 1",
                    digitalLocation = "GDrive/Finance/Bank/HDFC",
                    remarks = "Primary salary account. Interent banking associated with registered mobile.",
                    detailsString = "IFSC: HDFC0000102\nType: Savings Account\nJoint Holder: None\nRegistered Mobile: +91 99999 88888\nUPI Linked: sharma.rahul@okhdfc\nSalary Tag: Yes\nStatus: Active"
                ),
                VaultItem(
                    category = "BANK",
                    title = "SBI Joint Account",
                    ownerName = "Rahul & Priya Sharma",
                    institution = "State Bank of India",
                    numberOrId = "10984532109",
                    nomineeName = "Ramesh Sharma",
                    nomineeRelation = "Father",
                    nomineeVerified = false, // Nominee mismatch alert demo
                    physicalLocation = "Cabinet A, File 1",
                    digitalLocation = "GDrive/Finance/Bank/SBI",
                    remarks = "Emergency back up cash and household bill payments account.",
                    detailsString = "IFSC: SBIN0004512\nType: Joint Savings (Either or Survivor)\nRegistered Mobile: +91 99999 88888\nDebit Card Linked: Yes\nNominee Note: Double check if nominee Ramesh needs to be changed to child."
                ),
                // Investments
                VaultItem(
                    category = "INVESTMENT",
                    title = "Zerodha Demat Portfolio",
                    ownerName = "Rahul Sharma",
                    institution = "Zerodha / CDSL",
                    numberOrId = "IN30012015949231",
                    nomineeName = "Priya Sharma",
                    nomineeRelation = "Spouse",
                    nomineeVerified = true,
                    physicalLocation = "No physical paper (Digital only)",
                    digitalLocation = "GDrive/Finance/Investments/Demat",
                    remarks = "Consists of long term blue-chip Indian equities.",
                    detailsString = "DP ID: 12081600\nClient ID: 05831201\nAdvisor: Direct\nLinked Bank: HDFC Bank\nApprox Value: ₹18,50,000\nInvested Amount: ₹12,00,000"
                ),
                VaultItem(
                    category = "INVESTMENT",
                    title = "SBI Public Provident Fund (PPF)",
                    ownerName = "Rahul Sharma",
                    institution = "State Bank of India",
                    numberOrId = "3051421095",
                    nomineeName = "", // Empty to trigger "No Nominee Warning"
                    nomineeRelation = "",
                    nomineeVerified = false,
                    physicalLocation = "Cabinet A, File PPF",
                    digitalLocation = "GDrive/Finance/Investments/PPF",
                    remarks = "Tax-saving long-term investment. Maturing in 2032.",
                    detailsString = "Maturity Date: 2032-03-31\nAnnual Deposit: ₹1,50,000\nLinked Bank: SBI Joint\nApprox Value: ₹6,80,000\nWARNING: Missing nominee in SBI online portal! Urgent update required."
                ),
                // Insurance
                VaultItem(
                    category = "INSURANCE",
                    title = "LIC Amulya Jeevan Term Plan",
                    ownerName = "Rahul Sharma",
                    institution = "Life Insurance Corp of India",
                    numberOrId = "194512304",
                    nomineeName = "Priya Sharma",
                    nomineeRelation = "Spouse",
                    nomineeVerified = true,
                    physicalLocation = "Cabinet B, Premium Cover Folder",
                    digitalLocation = "GDrive/Finance/Insurance/LIC",
                    remarks = "Term coverage of ₹1 Core. Kept active yearly.",
                    detailsString = "Policy Type: Term Life Insurance\nSum Insured: ₹1,00,00,000\nPremium: ₹18,500/year\nDue Date: 2026-08-15\nExpiry Date: 2045-08-15\nHelpline: 022-68276827\nClaim Procedure: Submit original policy paper, Form 3707, Death Cert, and NEFT details to Gurgaon branch."
                ),
                VaultItem(
                    category = "INSURANCE",
                    title = "HDFC Ergo Optima Restore Health Cover",
                    ownerName = "Family Floater",
                    institution = "HDFC Ergo General Insurance",
                    numberOrId = "2815200451000",
                    nomineeName = "Priya Sharma",
                    nomineeRelation = "Spouse",
                    nomineeVerified = true,
                    physicalLocation = "Car dashboard glove box + Main Ledger",
                    digitalLocation = "GDrive/Finance/Insurance/Health",
                    remarks = "Covers Rahul, Priya, and Aarav. Cashless active across fortis / max.",
                    detailsString = "Policy Type: Family Floater Mediclaim\nSum Insured: ₹10,00,000\nPremium: ₹24,000/year\nDue Date: 2026-11-05\nTPA Card Details: 3 separate cards kept in wallet\nTPA Number: 1800-2666-400\nClaim Process: Cashless request at admission desk via Mediclaim ID cards. Reimbursement files within 30 days."
                ),
                // Card details
                VaultItem(
                    category = "CARD",
                    title = "ICICI Amazon Pay Credit Card",
                    ownerName = "Rahul Sharma",
                    institution = "ICICI Bank",
                    numberOrId = "4315 **** **** 1094",
                    nomineeName = "N/A",
                    nomineeRelation = "N/A",
                    nomineeVerified = true,
                    physicalLocation = "Rahul's Wallet",
                    remarks = "Primary cashback card. Auto-debit active on utility bills.",
                    detailsString = "Card network: Visa\nRegistered Mobile: +91 99999 88888\nValid Till: 2029-09\nStatus: Active\nSECURITY Note: Strictly NO PIN or CVV stored here. Managed on physical Bitwarden password vault instructions."
                ),
                // Property & Locker
                VaultItem(
                    category = "PROPERTY",
                    title = "Residential Flat 502",
                    ownerName = "Rahul & Priya Sharma",
                    institution = "Gurgaon Registry Office",
                    numberOrId = "Reg-2016-D44521",
                    nomineeName = "Priya Sharma",
                    nomineeRelation = "Spouse",
                    nomineeVerified = true,
                    physicalLocation = "Locker Cabinet SBI, Jaipur (Under dad's custody)",
                    digitalLocation = "GDrive/Finance/Property/TitleDeed_Deed",
                    remarks = "A 3BHK flat under joint registration.",
                    detailsString = "Property Type: Residential Real Estate\nPurchase Date: 2016-04-12\nPurchase Value: ₹75,00,000\nOutstanding Loan: Ref HDFC Home Loan\nSuccessor Will Class: Bequeathed fully to Priya Sharma in drafted physical Will."
                ),
                VaultItem(
                    category = "LOCKER",
                    title = "Safe Locker SBI Branch",
                    ownerName = "Rahul & Ramesh Sharma",
                    institution = "SBI Golf Course Road",
                    numberOrId = "Locker-204-ClassB",
                    nomineeName = "Priya Sharma",
                    nomineeRelation = "Spouse",
                    nomineeVerified = true,
                    physicalLocation = "Key in Bedroom Wooden Almirah, small black cup",
                    remarks = "Contains ancestral jewellery, gold biscuits, and flat registered title deeds.",
                    detailsString = "Branch Address: Ground Floor, Unit 1, Sector 54, Gurgaon\nLocker Number: 204\nKey Code/Label: S-204-B\nAccess instructions: Joint authentication with Dad or Single sign. Keep locker rent active on SBI account."
                ),
                // Liability
                VaultItem(
                    category = "LIABILITY",
                    title = "HDFC Auto Loan (Kia Seltos)",
                    ownerName = "Rahul Sharma",
                    institution = "HDFC Bank Ltd",
                    numberOrId = "LAN-ALT90812301",
                    nomineeName = "N/A",
                    nomineeRelation = "N/A",
                    nomineeVerified = true,
                    physicalLocation = "Cabinet C, File 3",
                    digitalLocation = "GDrive/Finance/Loans/Car",
                    remarks = "5-year automobile loan, EMIs deducted automatic on 5th of every month.",
                    detailsString = "Original Loan: ₹9,50,000\nOutstanding Amount: ₹3,40,000\nEMI: ₹18,450\nDue Date: Monthly 5th\nInsurance Linked: Yes (HDFC Ergo Motor Policy)\nCo-borrower: None\nClosure Steps: Upon final payment, obtain NOC from HDFC online portal and file for RTO hypothecation removal."
                ),
                // Important Document
                VaultItem(
                    category = "DOCUMENT",
                    title = "Aadhaar Card (UIDAI)",
                    ownerName = "Rahul Sharma",
                    institution = "UIDAI (Govt of India)",
                    numberOrId = "XXXX-XXXX-4819",
                    nomineeName = "N/A",
                    nomineeRelation = "N/A",
                    nomineeVerified = true,
                    physicalLocation = "Master Bedroom Wardrobe - Red File 1",
                    digitalLocation = "DigiLocker / UIDAI Portal",
                    remarks = "Linked to mobile +91 99999 88888. Biometrics locked via mAadhaar app.",
                    detailsString = "Issuing Authority: Unique Identification Authority of India (UIDAI)\nVID: 9182 3491 0293 8412\nRegistered Mobile: +91 99999 88888\nAddress: Sector 45, Gurgaon, Haryana\nmAadhaar PIN: In physical safe envelope"
                ),
                VaultItem(
                    category = "DOCUMENT",
                    title = "PAN Card (Income Tax)",
                    ownerName = "Rahul Sharma",
                    institution = "Income Tax Department of India",
                    numberOrId = "ABCPS1234F",
                    nomineeName = "N/A",
                    nomineeRelation = "N/A",
                    nomineeVerified = true,
                    physicalLocation = "Personal Wallet + Red File 1",
                    digitalLocation = "e-Filing Portal / DigiLocker",
                    remarks = "Linked to Aadhaar and HDFC/SBI bank accounts.",
                    detailsString = "Category: Individual\nAadhaar Linked: Yes (Verified on IT Portal)\nJurisdiction: Ward 22(1), New Delhi\ne-Filing User ID: ABCPS1234F"
                ),
                VaultItem(
                    category = "DOCUMENT",
                    title = "Indian Passport",
                    ownerName = "Rahul Sharma",
                    institution = "Ministry of External Affairs, India",
                    numberOrId = "Z1094827",
                    nomineeName = "Priya Sharma (Spouse)",
                    nomineeRelation = "Spouse",
                    nomineeVerified = true,
                    physicalLocation = "SBI Safe Locker (Unit 204) - Gurgaon",
                    digitalLocation = "GDrive/Personal/Passport_Scan.pdf",
                    remarks = "Valid till 2031. Kept in bank locker when not traveling.",
                    detailsString = "Passport Type: Regular (Type P - 36 pages)\nDate of Issue: 2021-05-18\nDate of Expiry: 2031-05-17\nPlace of Issue: Delhi\nEmigration Check: ECNR"
                ),
                VaultItem(
                    category = "DOCUMENT",
                    title = "Original Physical Will Draft",
                    ownerName = "Rahul Sharma",
                    institution = "Sanjay Krishna Chambers (Draft)",
                    numberOrId = "Will-Ref-2024SR",
                    nomineeName = "Priya Sharma (Executor)",
                    nomineeRelation = "Spouse",
                    nomineeVerified = true,
                    physicalLocation = "SBI Safe Locker (Unit 204) - Gurgaon",
                    digitalLocation = "GDrive/Personal/Will_Encrypted.pdf",
                    remarks = "The final signed Will. Drafted in 2024, witnessed by Dr. Joshi & Sanjay Mehta.",
                    detailsString = "Signing Date: 2024-10-01\nExecuting Lawyer: Sanjay Krishna\nOriginal Witnesses: Dr. Sameer Joshi & Alok Mehta\nRecovery Password: Refer physical card in wallet binder."
                ),
                // Digital Assets
                VaultItem(
                    category = "DIGITAL",
                    title = "Bitwarden Primary Password Vault Hint",
                    ownerName = "Rahul Sharma",
                    institution = "Bitwarden cloud",
                    numberOrId = "rahul.sharma@gmail.com (Acc ID)",
                    nomineeName = "Priya Sharma",
                    nomineeRelation = "Spouse",
                    nomineeVerified = true,
                    physicalLocation = "Main safe key envelope contains master password hint phrase.",
                    remarks = "Contains credentials for every net banking, mutual fund app, and insurance logins.",
                    detailsString = "Master Email: rahul.sharma.finance@gmail.com\nRecovery Email: priya.sharma@gmail.com\nEmergency Access Switch: Nominee requested setup is enabled on Bitwarden with 4-day dead-man threshold.\nEmergency Tech Person: Mahendra Sen (RM/Friend)\nInstruction: Priya knows the physical phrase written behind our wedding photograph card structure."
                )
            )
            vaultItems.forEach { db.vaultItemDao().insert(it) }

            // 4. Seed Default Checklist Tasks (Emergency Action Plan)
            val actions = listOf(
                // 24 hours
                EmergencyActionItem(
                    phase = "First 24 Hours",
                    taskName = "Primary Notifications",
                    instructions = "Contact closest family members, Dr. Sameer Joshi (+91 98111 22233) for formal certificate, and immediate employer HR to inform.",
                    completed = false,
                    status = "In progress",
                    whoToContact = "• Dr. Sameer Joshi (Family Physician): +91 98111 22233\n• Employer HR Desk: hr@company.com / +91 124 4567890\n• Designated family emergency contacts",
                    documentsToPrepare = "• Identity cards (Aadhaar, Voter ID) of the individual\n• Hospital admission records & medical file\n• Employer employee badge / corporate ID",
                    stepByStepGuidance = "1. Contact attending doctor or hospital registrar to obtain formal Medical Certificate of Cause of Death (Form 4/4A).\n2. Dispatch emergency SOS message & location coordinates to immediate relatives.\n3. Inform employer HR department to pause corporate devices and initiate compassionate benefits.",
                    updatedBy = "System"
                ),
                EmergencyActionItem(
                    phase = "First 24 Hours",
                    taskName = "Locate Personal Will",
                    instructions = "Retrieve the envelope from wooden cupboard or access SBI locker 204. Contains funeral/medical instructions.",
                    completed = false,
                    status = "Pending",
                    whoToContact = "• Advocate Sanjay Krishna (Legal Counsel): +91 98100 98100\n• Ramesh Sharma (Father / Locker Co-holder): +91 99112 23344\n• SBI Branch Locker Officer: +91 124 2381200",
                    documentsToPrepare = "• Wooden cupboard safe key / SBI Locker 204 safe key\n• Original registered copy of Will or registered codicil\n• Identity proof of the nominated executor",
                    stepByStepGuidance = "1. Retrieve safe key from bedroom wooden almirah.\n2. Access Safe Deposit Locker 204 at SBI Golf Course Road branch.\n3. Keep the sealed Will envelope intact without breaking legal seals until legal counsel arrives.",
                    updatedBy = "System"
                ),
                EmergencyActionItem(
                    phase = "First 24 Hours",
                    taskName = "Contact Executor",
                    instructions = "Inform Sanjay Krishna Lawyer (+91 98100 98100). He will initiate Will probate or official readings.",
                    completed = false,
                    status = "Pending",
                    whoToContact = "• Sanjay Krishna (Advocate & Designated Executor): +91 98100 98100, Patiala House Courts Chambers\n• Alok Mehta (Family CA): +91 93123 45678",
                    documentsToPrepare = "• Death confirmation memo / hospital slip\n• Original signed physical Will\n• Asset inventory ledger downloaded from this Vault",
                    stepByStepGuidance = "1. Schedule meeting with Adv. Sanjay Krishna.\n2. Provide certified copy of death confirmation and Will copy.\n3. Establish legal executor timeline and court probate requirements.",
                    updatedBy = "System"
                ),
                // 7 Days
                EmergencyActionItem(
                    phase = "First 7 Days",
                    taskName = "Request Death Certificate",
                    instructions = "Obtain 15 physical copies of official Death Certificate from MCG (Municipal Corporate Gurgaon). These are required for all asset claims.",
                    completed = false,
                    status = "In progress",
                    whoToContact = "• Municipal Corporation of Gurgaon (MCG) Registrar Desk\n• Local Crematorium / Hospital Civil Registry counter\n• Adv. Sanjay Krishna (for attestation)",
                    documentsToPrepare = "• Hospital Death Report / Form 4A\n• Cremation / Burial slip\n• Deceased Aadhaar card & Applicant Aadhaar card\n• Application form with fee receipt",
                    stepByStepGuidance = "1. Submit Form 2 along with hospital memo to the local Registrar of Births & Deaths within 21 days.\n2. Apply for at least 15 to 20 original certified copies with official holographic stamp.\n3. Keep digital scans saved in secure drive and physically laminate 2 copies.",
                    updatedBy = "System"
                ),
                EmergencyActionItem(
                    phase = "First 7 Days",
                    taskName = "Trigger Health & Term Insurance",
                    instructions = "Inform LIC Helpline & Health TPA immediately. Check claim procedure notes pinned under LIC and Health Cover modules.",
                    completed = false,
                    status = "Pending",
                    whoToContact = "• LIC India Claims Assistance Cell: 1800-33-4433 / Local Branch Manager\n• Star Health / HDFC Ergo TPA desk: 1800-425-2255\n• Mahendra Sen (Insurance Advisor): +91 91100 11001",
                    documentsToPrepare = "• Original insurance policy bond / digital certificate\n• Attested Death Certificate (3 copies)\n• Claim Form A (filled and signed by Nominee)\n• Nominee Cancelled Cheque & Bank passbook\n• Nominee KYC (PAN & Aadhaar)",
                    stepByStepGuidance = "1. Inform the insurer hotline within 7 days of incident citing policy number.\n2. Download Claim Form 3783 (LIC) or TPA death claim dossier.\n3. Submit claim packet along with doctor's medical history report and death certificate.\n4. Note claim intimation reference number for weekly status tracking.",
                    updatedBy = "System"
                ),
                EmergencyActionItem(
                    phase = "First 7 Days",
                    taskName = "Freeze Credit/Debit Cards",
                    instructions = "Log into ICICI & HDFC or call helpdesk to pause or block credit cards to avoid fraudulent auto-debits.",
                    completed = true,
                    status = "Completed",
                    whoToContact = "• HDFC Bank Credit Card Emergency: 1800-266-4332\n• ICICI Bank Card Helpdesk: 1800-1080\n• Card Customer Care Desk",
                    documentsToPrepare = "• Card last 4 digits (stored in Vault)\n• Registered mobile number for confirmation",
                    stepByStepGuidance = "1. Call bank emergency line or use mobile banking to immediately freeze cards.\n2. Cancel active recurring standing instructions and auto-debit mandates.\n3. Avoid closing card accounts until all insurance and hospital payouts clear.",
                    updatedBy = "System"
                ),
                // 30 Days
                EmergencyActionItem(
                    phase = "First 30 Days",
                    taskName = "Bank Account Transmission",
                    instructions = "Apply for Nominee settlement in HDFC and SBI using death cert & nominee Aadhaar/PAN. The joint account in SBI can continue directly with Priya.",
                    completed = false,
                    status = "Pending",
                    whoToContact = "• HDFC Bank Branch Manager (DLF Phase 4 Branch)\n• SBI Golf Course Road Branch Service Desk\n• Alok Mehta (CA) for tax clearance guidance",
                    documentsToPrepare = "• Form DA-2 (Nominee Claim Application)\n• Certified Death Certificate copy\n• Nominee Aadhaar, PAN card, and 2 passport photos\n• Passbook and unused cheque leaves of the deceased",
                    stepByStepGuidance = "1. For Joint Accounts ('Either or Survivor'), submit death certificate to delete first holder name and continue single operation.\n2. For single savings account with registered nominee, submit Form DA-2. Funds are transferred within 15 working days without succession certificate.\n3. Collect final interest certificate for filing final tax returns.",
                    updatedBy = "System"
                ),
                EmergencyActionItem(
                    phase = "First 30 Days",
                    taskName = "Mutual Fund and Demat transmission",
                    instructions = "Contact RM Mahendra Sen (+91 91100 11001) to initiate Zerodha CDSL transmission. Priya is nominee, requires Form ISR-1, ISR-2, and client booklet.",
                    completed = false,
                    status = "Pending",
                    whoToContact = "• Mahendra Sen (Zerodha RM): +91 91100 11001\n• CDSL Helpline: 1800-22-5533\n• CAMS / KFintech Investor Center",
                    documentsToPrepare = "• Form ISR-1 (Transmission Request Form)\n• Client Master Report (CMR) of Nominee Demat Account\n• Notarized Death Certificate\n• Nominee self-attested PAN and Aadhaar",
                    stepByStepGuidance = "1. Open a new Demat account for the nominee if not already having one.\n2. Submit Annexure-A transmission application to Zerodha / DP.\n3. Shares and mutual fund units will be credited directly to Nominee's demat account off-market without capital gains tax event.",
                    updatedBy = "System"
                )
            )
            actions.forEach { db.emergencyActionItemDao().insert(it) }

            // 5. Seed Claim & Closure Tracker
            val claims = listOf(
                ClaimRecord(
                    itemType = "Insurance Claim",
                    institution = "LIC Term Cover (Policy: 194512304)",
                    status = "Not Started",
                    assignedPerson = "Priya Sharma",
                    pendingDocuments = "Death Certificate (Original), Claimant Statement (Form 3707), Cancelled Cheque, Aadhaar/PAN",
                    expectedTimeline = "30-45 Days from submission",
                    notes = "Reach out to LIC Gurgaon branch manager directly."
                ),
                ClaimRecord(
                    itemType = "Demat Transmission",
                    institution = "Zerodha Portfolio (CDSL ID: 05831201)",
                    status = "Not Started",
                    assignedPerson = "Priya Sharma",
                    pendingDocuments = "Transmission Request Form, Client Master Report, Notarised death cert copy",
                    expectedTimeline = "15-20 Days",
                    notes = "Mahendra Sen is helping guide this file compilation."
                )
            )
            claims.forEach { db.claimRecordDao().insert(it) }

            // 6. Seed Audit Logs
            val auditLogs = listOf(
                AuditLog(
                    action = "System Setup",
                    details = "Initialized database schema with robust India-focused seed records.",
                    userRole = "Owner"
                ),
                AuditLog(
                    action = "Database Seeding",
                    details = "Standard records populated securely for bank accounts, term covers, lockers, real estate, liabilities, and contacts.",
                    userRole = "Owner"
                )
            )
            auditLogs.forEach { db.auditLogDao().insert(it) }

            // 7. Seed Enterprise Server Users for Instant Return Login and Admin Audit
            if (db.serverUserDao().getCount() == 0) {
                val seedSalt = "sAlT_sEcUrE_2026_xYz"
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                digest.update(seedSalt.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                val adminHash = digest.digest("admin".toByteArray(java.nio.charset.StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }

                val seedUsers = listOf(
                    ServerUserRecord(
                        userId = "usr_fev_8819201",
                        email = "rahul@gmail.com",
                        fullName = "Rahul Sharma",
                        phone = "+91 98765 43210",
                        passwordHash = adminHash,
                        passwordSalt = seedSalt,
                        mpin = "4321",
                        nomineeName = "Priya Sharma",
                        nomineeRelation = "Spouse",
                        securityQuestion = "What was your first school name?",
                        securityAnswerHash = "greenwood_hash_verified",
                        digitalCertificateId = "FEV-SRV-2026-CERT-8819",
                        status = "ACTIVE_VERIFIED",
                        role = "OWNER",
                        tokenJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.seed.rahul.verified",
                        deviceFingerprint = "Android-Enclave-Pixel8-Sandboxed",
                        isBiometricEnrolled = true
                    ),
                    ServerUserRecord(
                        userId = "usr_fev_8819202",
                        email = "priya@gmail.com",
                        fullName = "Priya Sharma",
                        phone = "+91 98765 43211",
                        passwordHash = adminHash,
                        passwordSalt = seedSalt,
                        mpin = "1234",
                        nomineeName = "Rahul Sharma",
                        nomineeRelation = "Spouse",
                        securityQuestion = "What is your mother's maiden name?",
                        securityAnswerHash = "gupta_hash_verified",
                        digitalCertificateId = "FEV-SRV-2026-CERT-8820",
                        status = "ACTIVE_VERIFIED",
                        role = "SPOUSE",
                        tokenJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.seed.priya.verified",
                        deviceFingerprint = "Android-Enclave-Samsung-Sandboxed",
                        isBiometricEnrolled = true
                    ),
                    ServerUserRecord(
                        userId = "usr_fev_8819203",
                        email = "admin.auditor@familyvault.io",
                        fullName = "Ashmit Gautam (System Auditor)",
                        phone = "+91 98110 02299",
                        passwordHash = adminHash,
                        passwordSalt = seedSalt,
                        mpin = "9900",
                        nomineeName = "Executive Board",
                        nomineeRelation = "Organization",
                        securityQuestion = "What was your childhood nickname?",
                        securityAnswerHash = "ash_hash_verified",
                        digitalCertificateId = "FEV-SRV-2026-CERT-9900",
                        status = "ACTIVE_VERIFIED",
                        role = "ADMIN",
                        tokenJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.seed.admin.auditor",
                        deviceFingerprint = "Admin-Console-Cluster-Node01",
                        isBiometricEnrolled = true
                    )
                )
                seedUsers.forEach { db.serverUserDao().insert(it) }
            }
        }
    }
}

