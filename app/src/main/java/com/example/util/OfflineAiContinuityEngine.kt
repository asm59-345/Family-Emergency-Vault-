package com.example.util

import com.example.data.ImportantContact
import com.example.data.VaultItem
import java.util.Locale

/**
 * Intelligent Offline Continuity AI Knowledge Base & Reasoning Engine.
 * Operates 100% locally on-device without internet or cloud APIs.
 * Answers questions about emergency preparedness, insurance claims, nominee disputes,
 * bank claim settlements, dead-man switch rules, physical lockers, and immediate crisis steps.
 */
object OfflineAiContinuityEngine {

    data class AiResponse(
        val answer: String,
        val suggestedFollowups: List<String> = emptyList(),
        val confidenceBadge: String = "100% Offline AI Knowledge"
    )

    fun generateResponse(
        userQuery: String,
        userName: String,
        nomineeName: String,
        nomineeRelation: String,
        nomineePhone: String,
        contacts: List<ImportantContact>,
        vaultItems: List<VaultItem>,
        isHindi: Boolean = false
    ): AiResponse {
        val q = userQuery.trim().lowercase(Locale.ROOT)

        // 1. EMERGENCY / IMMEDIATE CRISIS / ACCIDENT / FIRST 24 HOURS
        if (q.contains("emergency") || q.contains("crisis") || q.contains("accident") || q.contains("what should my family do") || q.contains("help") || q.contains("आपातकाल") || q.contains("मदद")) {
            val primaryDoc = contacts.find { it.category.contains("Doctor", ignoreCase = true) }?.let { "${it.contactName} (${it.phone})" } ?: "Registered Medical Professional"
            val responseText = if (isHindi) {
                """🚨 आपातकालीन तत्काल कार्ययोजना (First 24-Hours Emergency Protocol):
1. जीवन रक्षा व मेडिकल सहायता:
   • 112 (राष्ट्रीय आपातकाल) या 108/102 (एम्बुलेंस) पर तुरंत संपर्क करें।
   • फैमिली डॉक्टर: $primaryDoc

2. प्राथमिक नॉमिनी से संपर्क:
   • प्राथमिक नॉमिनी: $nomineeName ($nomineeRelation, फोन: $nomineePhone) को तुरंत अलर्ट करें।
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
   • Primary Designated Nominee: $nomineeName ($nomineeRelation, Phone: $nomineePhone).
   • Tap the top-bar 🆘 SOS button to dispatch live GPS coordinates via SMS (works offline).

3. Cashless Hospitalization Claim:
   • Present Health Insurance TPA card or policy details at the hospital desk within 24 hours.
   • Check the 'Vault' section in this app for exact policy numbers and physical kit locations.

4. Keep physical dossier accessible:
   • Export the 1-Page Emergency Physical Dossier from the SOS menu for triage reference."""
            }

            return AiResponse(
                answer = responseText,
                suggestedFollowups = listOf(
                    if (isHindi) "बीमा क्लेम कैसे फाइल करें?" else "How to claim insurance securely?",
                    if (isHindi) "बैंक खाता क्लेम की प्रक्रिया क्या है?" else "Bank account claim procedure",
                    if (isHindi) "नॉमिनी और वारिस में क्या अंतर है?" else "Nominee vs Legal Heir rules"
                )
            )
        }

        // 2. INSURANCE CLAIMS (HEALTH / TERM / LIFE)
        if (q.contains("insurance") || q.contains("claim") || q.contains("lic") || q.contains("policy") || q.contains("बीमा") || q.contains("क्लेम")) {
            val insurances = vaultItems.filter { it.category.equals("INSURANCE", ignoreCase = true) }
            val insSummary = if (insurances.isNotEmpty()) {
                insurances.joinToString("\n") { "• ${it.institution} (${it.title}) - Policy #${it.numberOrId}, Physical Kit: ${it.physicalLocation.ifBlank { "Home Safe" }}" }
            } else {
                "• No insurance policies registered yet. Please add them in the Vault tab."
            }

            val responseText = if (isHindi) {
                """📋 बीमा क्लेम प्रक्रिया गाइड (Insurance Settlement Guide):

1. हेल्थ इंश्योरेंस (Health Insurance):
   • कैशलेस: अस्पताल में नेटवर्क TPA डेस्क को सूचना दें (इमरजेंसी में 24 घंटे, प्लान्ड में 48 घंटे पहले)।
   • प्रतिपूर्ति (Reimbursement): अस्पताल के सभी मूल बिल, डिस्चार्ज सारांश और डॉक्टर पर्चे सुरक्षित रखें। डिस्चार्ज के 30 दिनों में फॉर्म जमा करें।

2. टर्म व लाइफ इंश्योरेंस (Term & Life Insurance):
   • आवश्यक दस्तावेज: मूल पॉलिसी बॉन्ड, मृत्यु प्रमाण पत्र (Death Certificate), नॉमिनी का आधार/पैन और रद्द चेक (Cancelled Cheque)।
   • IRDAI नियमों के अनुसार दावा प्रस्तुत करने के 30 दिनों के भीतर बीमा कंपनी को निर्णय लेना होता है।

3. आपके वॉल्ट में दर्ज बीमा:
$insSummary"""
            } else {
                """📋 Insurance Claim Settlement Guide:

1. Health Insurance Claims:
   • Cashless: Intimate hospital TPA desk within 24 hours of emergency admission.
   • Reimbursement: Preserve original discharge summary, consolidated final bill, diagnostic reports, and medical bills. File within 30 days of discharge.

2. Term & Life Insurance Claims:
   • Required Documents: Original policy kit/bond, official Municipal Death Certificate, Claimant/Nominee KYC (PAN + Aadhaar), and Claimant Cancelled Cheque (for NEFT credit).
   • Under IRDAI guidelines, simple uncontested claims must be processed within 30 days of complete paperwork.

3. Vault Registered Policies:
$insSummary"""
            }

            return AiResponse(
                answer = responseText,
                suggestedFollowups = listOf(
                    if (isHindi) "बैंक खाते का पैसा कैसे निकालें?" else "How to claim bank accounts?",
                    if (isHindi) "नॉमिनी के क्या अधिकार हैं?" else "What rights does a nominee have?",
                    if (isHindi) "लॉकर कैसे एक्सेस करें?" else "How to access safe locker?"
                )
            )
        }

        // 3. BANK ACCOUNT CLAIMS / DECEASED CLAIM SETTLEMENT
        if (q.contains("bank") || q.contains("account") || q.contains("fd") || q.contains("fixed deposit") || q.contains("खाता") || q.contains("बैंक")) {
            val banks = vaultItems.filter { it.category.equals("BANK", ignoreCase = true) }
            val bankSummary = if (banks.isNotEmpty()) {
                banks.joinToString("\n") { "• ${it.institution} - A/C #${it.numberOrId} (Nominee: ${it.nomineeName.ifBlank { "Not set" }})" }
            } else {
                "• No bank accounts registered yet in Vault."
            }

            val responseText = if (isHindi) {
                """🏦 बैंक डेथ क्लेम प्रक्रिया (RBI Deceased Claim Framework):

1. यदि नॉमिनी पंजीकृत है (Nominee Registered):
   • RBI के दिशानिर्देशानुसार, नॉमिनी को बैंक जाने पर बिना किसी उत्तराधिकार प्रमाण पत्र (Succession Certificate) के 15 दिनों के भीतर भुगतान किया जाना अनिवार्य है।
   • आवश्यक: बैंक का Deceased Claim Form (Annexure A), मृत्यु प्रमाण पत्र (मूल प्रति सत्यापन हेतु), नॉमिनी का KYC और पासबुक/चेकबुक।

2. यदि संयुक्त खाता है (Joint Account with 'Either or Survivor'):
   • जीवित खाताधारक केवल मृत्यु प्रमाण पत्र और फ्रेश KYC देकर खाते का पूर्ण संचालन जारी रख सकता है।

3. आपके वॉल्ट में पंजीकृत बैंक:
$bankSummary"""
            } else {
                """🏦 Bank Account Claim Framework (RBI Mandate):

1. If Nominee is Registered:
   • Per Reserve Bank of India (RBI) circulars, banks MUST settle accounts with registered nominees within 15 days without demanding Succession Certificates or Letters of Administration.
   • Required Documents: Bank's Deceased Claim Settlement Form, Certified Municipal Death Certificate, Nominee's KYC (PAN, Aadhaar), and original Passbook/Chequebook.

2. Joint Accounts (Either or Survivor mode):
   • The surviving holder can continue operating the account smoothly upon submitting a certified copy of the death certificate.

3. Vault Registered Accounts:
$bankSummary"""
            }

            return AiResponse(
                answer = responseText,
                suggestedFollowups = listOf(
                    if (isHindi) "नॉमिनी बनाम कानूनी वारिस का नियम" else "Nominee vs Legal Heir difference",
                    if (isHindi) "म्यूचुअल फंड क्लेम कैसे करें?" else "Mutual fund transmission process",
                    if (isHindi) "बैंक लॉकर के नियम" else "Safe locker handover rules"
                )
            )
        }

        // 4. NOMINEE VS LEGAL HEIR / WILL / SUCCESSION
        if (q.contains("nominee") || q.contains("legal heir") || q.contains("will") || q.contains("inheritance") || q.contains("वारिस") || q.contains("वसीयत") || q.contains("नॉमिनी")) {
            val responseText = if (isHindi) {
                """⚖️ नॉमिनी बनाम कानूनी वारिस (Nominee vs Legal Heir Law):

1. सुप्रीम कोर्ट का स्पष्ट नियम:
   • नॉमिनी (Nominee) संपत्ति का "मालिक" (Owner) नहीं होता, बल्कि एक "ट्रस्टी" (Trustee / Custodian) होता है।
   • बैंक या बीमा कंपनी नॉमिनी को पैसा सौंप देती है ताकि पैसा न अटके, लेकिन अंतिम कानूनी मालिकाना हक वैध वसीयत (Will) या उत्तराधिकार कानून (Hindu/Indian Succession Act) के तहत कानूनी वारिसों का होता है।

2. अपवाद (Exceptions):
   • कंपनी शेयर्स (Companies Act) में कुछ विशिष्ट प्रावधान हैं।
   • यदि नॉमिनी ही एकमात्र कानूनी वारिस है, तो कोई विवाद नहीं होता।

3. सुरक्षित रखने की सलाह:
   • सुनिश्चित करें कि आपकी वसीयत (Will) और आपके सभी बैंक/डीमैट खातों के नॉमिनी समान व्यक्ति हों, ताकि परिवार में कोई विवाद न हो।"""
            } else {
                """⚖️ Legal Rule: Nominee vs. Legal Heir (Supreme Court Precedent):

1. Role of Nominee:
   • Under Indian jurisprudence, a Nominee is a designated custodian/trustee, NOT the absolute legal owner of the estate.
   • The financial institution (Bank, LIC, AMC) discharges its liability by paying the nominee directly. However, the nominee holds those funds in trust for the legal heirs.

2. Legal Heirs & Testamentary Will:
   • The ultimate ownership belongs to legal heirs defined under personal succession laws (or beneficiaries named in a registered Will).

3. Best Practice Recommendation:
   • Align your registered nominees across all Bank Accounts, Mutual Funds, and Insurance policies with the beneficiaries declared in your registered Will to eliminate probate friction."""
            }

            return AiResponse(
                answer = responseText,
                suggestedFollowups = listOf(
                    if (isHindi) "वसीयत कैसे बनाएं?" else "How to draft a simple Will?",
                    if (isHindi) "बैंक क्लेम प्रक्रिया" else "Bank claim checklist",
                    if (isHindi) "डेड-मैन स्विच क्या है?" else "How does Dead-Man Switch work?"
                )
            )
        }

        // 5. MUTUAL FUNDS & DEMAT TRANSMISSION
        if (q.contains("mutual fund") || q.contains("demat") || q.contains("shares") || q.contains("stock") || q.contains("शेयर") || q.contains("म्यूचुअल")) {
            val investments = vaultItems.filter { it.category.equals("INVESTMENT", ignoreCase = true) || it.category.equals("CRYPTO", ignoreCase = true) }
            val responseText = if (isHindi) {
                """📈 म्यूचुअल फंड व डीमैट ट्रांसमिशन (Transmission of Securities):

1. म्यूचुअल फंड (MF Central / CAMS / KFintech):
   • फॉर्म: "Transmission Request Form (T3 for Nominee)"।
   • आवश्यकताएं: मृत्यु प्रमाण पत्र (नोटरी सत्यापित), नॉमिनी का नया बैंक खाता (जहाँ पैसा जाएगा), और KYC (Aadhaar + PAN)।
   • आप MF Central या CAMS कार्यालय में जाकर एकल आवेदन से सभी फोलियो ट्रांसफर कर सकते हैं।

2. डीमैट खाता (Zerodha / Groww / Angel / NSDL / CDSL):
   • ब्रोकर को ट्रांसमिशन फॉर्म, मृत्यु प्रमाण पत्र और नॉमिनी का एक्टिव डीमैट खाता विवरण (Client Master Report - CMR) भेजें।
   • सभी शेयर नॉमिनी के डीमैट खाते में ट्रांसफर कर दिए जाते हैं।"""
            } else {
                """📈 Mutual Fund & Demat Transmission Process:

1. Mutual Funds (CAMS / KFintech / MF Central):
   • Submit 'Form T3 - Transmission Request for Nominee'.
   • Enclose: Notarized/Attested Death Certificate, Nominee's Verified KYC, and Client Cancelled Cheque for NEFT mandate.
   • A single consolidated transmission can be filed via MFCentral to cover multiple fund houses.

2. Demat & Equities (CDSL / NSDL):
   • Submit the Transmission Request Form to the Depository Participant (DP) / Broker.
   • Include: Certified Death Certificate and Nominee's Client Master Report (CMR) with active target Demat account.
   • Securities are directly moved in-specie to the Nominee's Demat portfolio."""
            }

            return AiResponse(
                answer = responseText,
                suggestedFollowups = listOf(
                    if (isHindi) "बीमा क्लेम गाइड" else "Insurance claim guide",
                    if (isHindi) "बैंक खाता क्लेम" else "Bank account claim",
                    if (isHindi) "नॉमिनी सत्यापन कैसे करें?" else "How to audit nominees?"
                )
            )
        }

        // 6. SAFE LOCKER / PHYSICAL KEYS
        if (q.contains("locker") || q.contains("safe") || q.contains("key") || q.contains("लॉकर") || q.contains("चाबी")) {
            val lockers = vaultItems.filter { it.category.equals("LOCKER", ignoreCase = true) }
            val locSummary = if (lockers.isNotEmpty()) {
                lockers.joinToString("\n") { "• ${it.institution} - Locker #${it.numberOrId} | Location: ${it.physicalLocation.ifBlank { "Home Safe" }}" }
            } else {
                "• No physical lockers registered in the Vault."
            }

            val responseText = if (isHindi) {
                """🔐 बैंक लॉकर व सेफ एक्सेस गाइड (Safe Locker Access Rules):

1. नॉमिनी एक्सेस (Nominee Operations):
   • यदि लॉकर में नॉमिनी है, तो बैंक दो स्वतंत्र गवाहों और एक बैंक अधिकारी की उपस्थिति में लॉकर खुलवाता है।
   • लॉकर की सभी सामग्रियों की एक विस्तृत सूची (Inventory Form) बनाई जाती है और नॉमिनी को सामान सौंप दिया जाता है।

2. महत्वपूर्ण चाबी स्थान (Key Records):
$locSummary

3. सावधानी:
   • बैंक लॉकर की चाबी कभी खोनी नहीं चाहिए, अन्यथा ब्रेक-ओपन (Break-open) का खर्च व लंबी प्रक्रिया होती है।"""
            } else {
                """🔐 Bank Safe Locker Settlement & Inventory Procedure:

1. Operations with Registered Nominee:
   • The bank opens the safe locker in the presence of the Nominee, two independent witnesses, and the Branch Manager.
   • A comprehensive signed inventory list of articles is drafted before handing over the locker contents to the nominee.

2. Registered Locker Keys in your Vault:
$locSummary

3. Crucial Security Practice:
   • Never misplace physical keys; break-open protocols entail substantial delay, locksmith charges, and high bank penalties."""
            }

            return AiResponse(
                answer = responseText,
                suggestedFollowups = listOf(
                    if (isHindi) "बैंक क्लेम चेकलिस्ट" else "Bank claim checklist",
                    if (isHindi) "फिजिकल डॉसियर कैसे प्रिंट करें?" else "How to print emergency dossier?",
                    if (isHindi) "इमरजेंसी SOS कैसे काम करता है?" else "How does SOS location work?"
                )
            )
        }

        // 7. DEAD-MAN SWITCH / EMERGENCY HANDOVER WORKFLOW
        if (q.contains("dead man") || q.contains("deadman") || q.contains("switch") || q.contains("handover") || q.contains("access") || q.contains("हैंडओवर")) {
            val responseText = if (isHindi) {
                """⏳ डेड-मैन स्विच व इमरजेंसी एक्सेस हैंडओवर (Security Architecture):

1. 48-घंटे का कूलिंग पीरियड (Delay Mechanism):
   • यदि नॉमिनी या एग्जीक्यूटर इमरजेंसी एक्सेस का अनुरोध करता है, तो ऐप तुरंत डेटा नहीं खोलता।
   • 48 घंटे का सुरक्षा समय शुरू होता है और ओनर को चेतावनी नोटिफिकेशन मिलता है।
   • यदि ओनर सुरक्षित है, तो वह 1-टैप में अनुरोध खारिज (Reject) कर सकता है।

2. ओनर इनएक्टिविटी (Lapse Detection):
   • यदि 48 घंटे तक ओनर की कोई प्रतिक्रिया नहीं आती, तो वॉल्ट नॉमिनी के लिए डिक्रिप्ट होकर केवल आवश्यक आपातकालीन जानकारी प्रदर्शित करता है।

3. शून्य क्लाउड जोखिम (Zero Cloud Exposure):
   • यह पूरा तर्क आपके फोन में स्थानीय रूप से चलता है। कोई पासवर्ड सर्वर पर नहीं भेजा जाता।"""
            } else {
                """⏳ Dead-Man Switch & Emergency Handover Architecture:

1. 48-Hour Cryptographic Timed Delay:
   • When an authorized Nominee requests emergency handover, the vault triggers an irreversible 48-hour challenge countdown.
   • High-priority alerts are dispatched to the primary owner.

2. Rejection & False-Alarm Cancellation:
   • If the account owner is healthy and active, they can instantly dismiss/revoke the request with 1 tap.

3. Graceful Deceased Handover:
   • If the timeout expires without owner cancellation, the vault releases the designated emergency action checklist and read-only claim guides to the nominee.
   • 100% On-Device: Zero remote server dependence or cloud telemetry."""
            }

            return AiResponse(
                answer = responseText,
                suggestedFollowups = listOf(
                    if (isHindi) "आपातकाल में क्या करें?" else "What should my family do in an emergency?",
                    if (isHindi) "बीमा क्लेम गाइड" else "Insurance claim settlement",
                    if (isHindi) "ऑफ़लाइन GPS कैसे शेयर करें?" else "How to share offline GPS location?"
                )
            )
        }

        // 8. DEFAULT INTELLIGENT CONTINUITY CONSULTATION
        val responseText = if (isHindi) {
            """🛡️ ऑफ़लाइन फैमिली कंटीन्यूइटी एआई सलाहकार (Offline Continuity Advisor):

मैं आपके डिवाइस पर 100% ऑफ़लाइन काम करने वाला AI असिस्टेंट हूँ। आपका कोई भी डेटा इंटरनेट पर नहीं जाता।

मैं निम्नलिखित विषयों पर तत्काल सलाह दे सकता हूँ:
• 🚨 आपातकाल के पहले 24 घंटे: डॉक्टर, एम्बुलेंस और नॉमिनी अलर्ट।
• 📄 बीमा क्लेम प्रक्रिया: हेल्थ, टर्म और लाइफ इंश्योरेंस के आवश्यक दस्तावेज।
• 🏦 बैंक खाता क्लेम: RBI के 15-दिवसीय नॉमिनी भुगतान नियम।
• ⚖️ नॉमिनी बनाम कानूनी वारिस: सुप्रीम कोर्ट के कानूनी नियम।
• 📈 म्यूचुअल फंड व शेयर ट्रांसमिशन (CAMS/Demat)।
• 🔐 बैंक लॉकर और फिजिकल चाबी एक्सेस।

ऊपर दिए गए किसी भी सुझाव पर टैप करें या अपना प्रश्न लिखें!"""
        } else {
            """🛡️ Offline Family Continuity AI Advisor:

I am running 100% on-device directly inside your phone. No internet or external API is required, keeping your family's financial secrets completely private.

I can provide instant, actionable guidance on:
• 🚨 Emergency First 24-Hours: Medical dispatch, nominee alert, and triage steps.
• 📄 Insurance Claims: Health cashless filing and Term/Life insurance settlement documents.
• 🏦 Bank Claims: RBI-mandated 15-day deceased account transmission rules.
• ⚖️ Nominee vs. Legal Heir: Supreme Court precedents & estate distribution laws.
• 📈 Mutual Funds & Demat: CAMS, KFintech, and broker transmission forms.
• 🔐 Safe Locker: Inspection, inventory protocols, and physical key records.

Ask any specific question or choose one of the quick prompts below!"""
        }

        return AiResponse(
            answer = responseText,
            suggestedFollowups = listOf(
                if (isHindi) "इमरजेंसी में क्या करें?" else "What should my family do in an emergency?",
                if (isHindi) "बीमा क्लेम कैसे फाइल करें?" else "How to claim insurance securely?",
                if (isHindi) "बैंक खाता क्लेम प्रक्रिया" else "Bank account claim procedure",
                if (isHindi) "नॉमिनी बनाम वारिस का नियम" else "Nominee vs Legal Heir rules"
            )
        )
    }
}
