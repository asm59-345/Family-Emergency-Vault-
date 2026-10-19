package com.example.util

import com.example.data.ImportantContact
import com.example.data.VaultItem
import java.util.Locale

/**
 * On-Device Emergency Knowledge Base & Legal Claims Guide.
 * 100% offline reference for Indian emergency protocols, RBI deceased claim settlement
 * guidelines, nominee rules, insurance claim procedures, and bank locker operations.
 * Operates purely via local structured reference lookup without external AI models or cloud calls.
 */
object EmergencyKnowledgeBase {

    data class GuideResponse(
        val answer: String,
        val suggestedFollowups: List<String> = emptyList(),
        val sourceBadge: String = "Offline Legal & Regulatory Reference"
    )

    fun searchGuide(
        query: String,
        userName: String,
        nomineeName: String,
        nomineeRelation: String,
        nomineePhone: String,
        contacts: List<ImportantContact>,
        vaultItems: List<VaultItem>,
        isHindi: Boolean = false
    ): GuideResponse {
        val q = query.trim().lowercase(Locale.ROOT)

        // 1. EMERGENCY / CRISIS / ACCIDENT / FIRST 24 HOURS
        if (q.contains("emergency") || q.contains("crisis") || q.contains("accident") ||
            q.contains("help") || q.contains("आपातकाल") || q.contains("मदद") || q.contains("first 24")
        ) {
            val primaryDoc = contacts.find { it.category.contains("Doctor", ignoreCase = true) }?.let { "${it.contactName} (${it.phone})" }
                ?: "Primary Family Doctor"
            val responseText = if (isHindi) {
                """🚨 आपातकालीन तत्काल कार्ययोजना (First 24-Hours Emergency Protocol):
1. जीवन रक्षा व मेडिकल सहायता:
   • 112 (राष्ट्रीय आपातकाल) या 108/102 (एम्बुलेंस) पर तुरंत संपर्क करें।
   • फैमिली डॉक्टर: $primaryDoc

2. प्राथमिक नॉमिनी से संपर्क:
   • प्राथमिक नॉमिनी: ${nomineeName.ifBlank { "पंजीकृत नॉमिनी" }} (${nomineeRelation.ifBlank { "परिवार" }}, फोन: ${nomineePhone.ifBlank { "N/A" }}) को तुरंत अलर्ट करें।
   • ऐप के "SOS" बटन से अपनी लाइव GPS लोकेशन SMS द्वारा 1-टैप में भेजें।

3. इंश्योरेंस कैशलेस क्लेम (अस्पताल भर्ती):
   • अस्पताल TPA डेस्क पर स्वास्थ्य बीमा कार्ड या पॉलिसी नंबर दिखाएं (24 घंटे के भीतर TPA प्री-ऑथराइजेशन आवश्यक है)।
   • ऐप के 'Vault' टैब में सभी मेडिकल पॉलिसी और क्लेम नंबर सुरक्षित हैं।

4. भौतिक दस्तावेज व लॉकर:
   • घर के सेफ या बैंक लॉकर की चाबी का स्थान चेक करें।"""
            } else {
                """🚨 Immediate Emergency & First 24-Hours Protocol:
1. Life Safety & Medical Triage:
   • Dial 112 (National SOS) or 108/102 (Ambulance).
   • Family Doctor: $primaryDoc

2. Notify Primary Nominee:
   • Primary Designated Nominee: ${nomineeName.ifBlank { "Registered Nominee" }} (${nomineeRelation.ifBlank { "Family" }}, Phone: ${nomineePhone.ifBlank { "N/A" }}).
   • Tap the top-bar 🆘 SOS button to dispatch live GPS coordinates via SMS (works offline).

3. Cashless Hospitalization Claim:
   • Present Health Insurance TPA card or policy details at the hospital desk within 24 hours.
   • Check the 'Vault' section in this app for exact policy numbers and physical kit locations.

4. Keep physical dossier accessible:
   • Export the 1-Page Emergency Physical Dossier from the SOS menu for triage reference."""
            }

            return GuideResponse(
                answer = responseText,
                suggestedFollowups = listOf(
                    if (isHindi) "बीमा क्लेम कैसे फाइल करें?" else "How to claim insurance securely?",
                    if (isHindi) "बैंक खाता क्लेम की प्रक्रिया क्या है?" else "Bank account claim procedure",
                    if (isHindi) "नॉमिनी और वारिस में क्या अंतर है?" else "Nominee vs Legal Heir rules"
                )
            )
        }

        // 2. INSURANCE CLAIMS
        if (q.contains("insurance") || q.contains("claim") || q.contains("lic") ||
            q.contains("mediclaim") || q.contains("बीमा") || q.contains("पॉलिसी")
        ) {
            val insItems = vaultItems.filter { it.category == "INSURANCE" }
            val insSummary = if (insItems.isNotEmpty()) {
                insItems.joinToString("\n") { "   • ${it.institution}: ${it.title} (No: ${it.numberOrId}) -> Nominee: ${it.nomineeName.ifBlank { "Not set" }}" }
            } else {
                "   • No insurance policies registered yet in your vault."
            }

            val text = if (isHindi) {
                """🛡️ बीमा क्लेम प्रक्रिया गाइड (IRDAI Guidelines):
1. टर्म लाइफ / जीवन बीमा क्लेम (Demise Claim):
   • आवश्यक फॉर्म: मृत्यु दावा प्रपत्र (Form 3783/3707)।
   • दस्तावेज: नगर निगम डेथ सर्टिफिकेट (मूल प्रति), मूल पॉलिसी बॉन्ड, नॉमिनी का बैंक खाता व कैंसिल चेक, KYC (आधार/PAN)।
   • समय सीमा: IRDAI नियमों के अनुसार 30 दिनों में क्लेम प्रक्रिया पूरी होना अनिवार्य है।

2. हेल्थ इंश्योरेंस कैशलेस / रीइंबर्समेंट:
   • कैशलेस: अस्पताल में भर्ती होने के 24 घंटे के भीतर TPA डेस्क पर इंटिमेशन दें।
   • रीइंबर्समेंट: डिस्चार्ज के 15 दिनों के अंदर ओरिजिनल बिल, डिस्चार्ज समरी, और मेडिकल रिपोर्ट जमा करें।

3. आपके वॉल्ट में दर्ज बीमा रिकॉर्ड्स:
$insSummary"""
            } else {
                """🛡️ Insurance Claim Settlement Guide (IRDAI Guidelines):
1. Term Life Claim Procedure:
   • Submit Form 3783 / 3707 with the insurer.
   • Mandatory Documents: Certified Municipal Death Certificate, Original Policy Document, Nominee cancelled cheque & KYC.
   • Statutory Timeline: IRDAI mandates settlement within 30 days of complete document submission.

2. Health Insurance Reimbursement / Cashless:
   • Cashless: Intimate hospital TPA desk within 24 hours of emergency admission.
   • Reimbursement: Submit original hospital bills, pharmacy receipts, and discharge summary within 15 days of discharge.

3. Policies Registered in Your Vault:
$insSummary"""
            }

            return GuideResponse(
                answer = text,
                suggestedFollowups = listOf(
                    if (isHindi) "बैंक खाता क्लेम कैसे करें?" else "How to claim bank accounts?",
                    if (isHindi) "लॉकर खोलने के क्या नियम हैं?" else "Bank safe locker access rules"
                )
            )
        }

        // 3. BANK CLAIM / RBI NOMINEE RULES
        if (q.contains("bank") || q.contains("rbi") || q.contains("account") ||
            q.contains("खाता") || q.contains("बैंक") || q.contains("savings")
        ) {
            val text = if (isHindi) {
                """🏦 बैंक खाता सेटलमेंट व RBI नियम (Nominee Claim Settlement):
1. यदि नॉमिनी पंजीकृत है (Section 45ZA, Banking Regulation Act):
   • बैंक को केवल डेथ सर्टिफिकेट और नॉमिनी का KYC (आधार/PAN) चाहिए।
   • किसी कोर्ट ऑर्डर या सक्सेशन सर्टिफिकेट की आवश्यकता नहीं है।
   • RBI निर्देश: बैंक को दस्तावेज प्राप्त होने के 15 दिनों के अंदर शेष राशि नॉमिनी को ट्रांसफर करनी होगी।

2. आवश्यक फॉर्म:
   • फॉर्म DA-2 (दावा निपटान प्रपत्र)
   • नॉमिनी का बचत खाता विवरण व कैंसिल चेक

3. संयुक्त खाते (Joint Accounts):
   • 'Either or Survivor' खाते में जीवित धारक सीधे खाते का संचालन जारी रख सकता है।"""
            } else {
                """🏦 Bank Account Claim Guidelines (RBI Mandate):
1. Registered Nominee Settlement (Section 45ZA, Banking Regulation Act):
   • The registered nominee needs only the Municipal Death Certificate and valid KYC.
   • No court succession certificate or probate is required for accounts with registered nominees.
   • Timeline: Under RBI circulars, banks must settle claims within 15 days of paperwork submission.

2. Required Documents:
   • Deceased Claim Application (Form DA-2).
   • Certified copy of Death Certificate.
   • Nominee photo KYC and cancelled cheque for balance disbursement.

3. Joint Accounts:
   • For 'Either or Survivor' or 'Former or Survivor' accounts, the surviving holder continues uninterrupted access upon presenting the death certificate."""
            }

            return GuideResponse(
                answer = text,
                suggestedFollowups = listOf(
                    if (isHindi) "लॉकर संचालन के नियम क्या हैं?" else "Bank locker operation rules",
                    if (isHindi) "डीमैट शेयर कैसे ट्रांसफर करें?" else "Demat share transmission process"
                )
            )
        }

        // 4. DEMAT / SHARES / MUTUAL FUNDS
        if (q.contains("demat") || q.contains("share") || q.contains("mutual") ||
            q.contains("zerodha") || q.contains("cdsl") || q.contains("nsdl") || q.contains("डीमैट")
        ) {
            val text = if (isHindi) {
                """📈 डिमैट शेयर व म्यूचुअल फंड ट्रांसमिशन गाइड (SEBI Guidelines):
1. डिमैट शेयर ट्रांसमिशन:
   • डिपॉजिटरी पार्टिसिपेंट (जैसे Zerodha, Groww, ICICI Direct) के पास ट्रांसमिशन फॉर्म (Annexure-O) जमा करें।
   • आवश्यक: अधिकृत डेथ सर्टिफिकेट, पैन कार्ड, और CAMS/KFintech में नॉमिनी का एक्टिव डिमैट खाता।

2. म्यूचुअल फंड फोलियो:
   • यदि नॉमिनी पहले से दर्ज है, तो AMC या CAMS केंद्र पर Form T2 जमा करें।
   • 5 लाख रुपये तक के फोलियो में केवल डेथ सर्टिफिकेट और नॉमिनी KYC से ट्रांसमिशन हो जाता है।"""
            } else {
                """📈 Demat Shares & Mutual Fund Transmission (SEBI Framework):
1. Demat Equity Transmission:
   • Submit Transmission Request Form (Annexure-O) to the Depository Participant (e.g. Zerodha, CDSL, NSDL).
   • Required: Notarized/Attested Death Certificate, Nominee Client Master Report (CMR), and PAN KYC.

2. Mutual Fund Transmission:
   • Submit Form T2 (Claim by Nominee) to registrar points (CAMS or KFintech) or directly to the AMC.
   • Value up to ₹5 Lakhs: Simplified processing with death certificate, nominee KYC, and bank mandate."""
            }

            return GuideResponse(
                answer = text,
                suggestedFollowups = listOf(
                    if (isHindi) "बैंक खाता क्लेम की प्रक्रिया क्या है?" else "Bank account claim procedure",
                    if (isHindi) "इमरजेंसी चेकलिस्ट कैसे देखें?" else "How to view emergency checklist?"
                )
            )
        }

        // DEFAULT: GENERAL CONTINUITY OVERVIEW
        val defaultText = if (isHindi) {
            """📋 आपातकालीन परिवार निरंतरता गाइड:
आप निम्न विषयों पर सटीक कानूनी व बैंकिंग प्रक्रियाएं पूछ सकते हैं:
1. "First 24 Hours": तत्काल जीवन रक्षा, अस्पताल भर्ती, और नॉमिनी सूचना।
2. "Insurance Claim": टर्म इंश्योरेंस और हेल्थ क्लेम के फॉर्म व नियम।
3. "Bank Accounts": RBI के तहत नॉमिनी क्लेम निपटान की प्रक्रिया।
4. "Demat & Shares": शेयर और म्यूचुअल फंड ट्रांसफर के SEBI नियम।
5. "Locker": बैंक सुरक्षित लॉकर खोलने के नियम।"""
        } else {
            """📋 Family Emergency Continuity Reference:
You can look up structured guidance on any of the following topics:
1. "First 24 Hours": Life triage, hospital admission, and immediate nominee alert.
2. "Insurance Claims": Life insurance death claim filing & health cashless protocols.
3. "Bank Accounts": RBI deceased claim settlement and Form DA-2.
4. "Demat & Shares": SEBI share transmission and mutual fund nominee claims.
5. "Lockers": Bank safe deposit locker release protocols."""
        }

        return GuideResponse(
            answer = defaultText,
            suggestedFollowups = listOf(
                if (isHindi) "आपातकालीन कार्ययोजना (First 24h)" else "Immediate Emergency Protocol",
                if (isHindi) "बीमा क्लेम गाइड" else "Insurance Claim Procedures",
                if (isHindi) "बैंक खाता क्लेम" else "Bank Account Nominee Rules"
            )
        )
    }
}
