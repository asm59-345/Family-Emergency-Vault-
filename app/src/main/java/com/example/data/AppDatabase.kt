package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        AuditLog::class
    ],
    version = 1,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "family_continuity_vault_db"
                )
                .addCallback(DatabaseSeederCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseSeederCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    seedData(database)
                }
            }
        }

        private suspend fun seedData(db: AppDatabase) {
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
                    updatedBy = "System"
                ),
                EmergencyActionItem(
                    phase = "First 24 Hours",
                    taskName = "Locate Personal Will",
                    instructions = "Retrieve the envelope from wooden cupboard or access SBI locker 204. Contains funeral/medical instructions.",
                    completed = false,
                    updatedBy = "System"
                ),
                EmergencyActionItem(
                    phase = "First 24 Hours",
                    taskName = "Contact Executor",
                    instructions = "Inform Sanjay Krishna Lawyer (+91 98100 98100). He will initiate Will probate or official readings.",
                    completed = false,
                    updatedBy = "System"
                ),
                // 7 Days
                EmergencyActionItem(
                    phase = "First 7 Days",
                    taskName = "Request Death Certificate",
                    instructions = "Obtain 15 physical copies of official Death Certificate from MCG (Municipal Corporate Gurgaon). These are required for all asset claims.",
                    completed = false,
                    updatedBy = "System"
                ),
                EmergencyActionItem(
                    phase = "First 7 Days",
                    taskName = "Trigger Health & Term Insurance",
                    instructions = "Inform LIC Helpline & Health TPA immediately. Check claim procedure notes pinned under LIC and Health Cover modules.",
                    completed = false,
                    updatedBy = "System"
                ),
                EmergencyActionItem(
                    phase = "First 7 Days",
                    taskName = "Freeze Credit/Debit Cards",
                    instructions = "Log into ICICI & HDFC or call helpdesk to pause or block credit cards to avoid fraudulent auto-debits.",
                    completed = false,
                    updatedBy = "System"
                ),
                // 30 Days
                EmergencyActionItem(
                    phase = "First 30 Days",
                    taskName = "Bank Account Transmission",
                    instructions = "Apply for Nominee settlement in HDFC and SBI using death cert & nominee Aadhaar/PAN. The joint account in SBI can continue directly with Priya.",
                    completed = false,
                    updatedBy = "System"
                ),
                EmergencyActionItem(
                    phase = "First 30 Days",
                    taskName = "Mutual Fund and Demat transmission",
                    instructions = "Contact RM Mahendra Sen (+91 91100 11001) to initiate Zerodha CDSL transmission. Priya is nominee, requires Form ISR-1, ISR-2, and client booklet.",
                    completed = false,
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
        }
    }
}
