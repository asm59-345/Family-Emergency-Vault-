package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.*

@Composable
fun LegalAndAboutHubDialog(
    model: VaultViewModel,
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(18.dp))
                .testTag("dialog_legal_and_about_hub"),
            colors = CardDefaults.cardColors(containerColor = SlateLightBg),
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = SlatePrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TealAccent.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = TealAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (model.isHindiMode) "कानूनी नीतियां, Q&A और ऐप परिचय" else "Legal, Governance, Q&A & Creator",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (model.isHindiMode) "पारदर्शिता • गोपनीयता • अशमित गौतम द्वारा निर्मित" else "Transparency • Privacy by Design • Built by Ashmit Gautam",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("btn_close_legal_hub")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Navigation Tabs Scrollable
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = SlatePrimary,
                    edgePadding = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = listOf(
                        Triple("T&C", Icons.Default.Gavel, if (model.isHindiMode) "नियम व शर्तें" else "T&C"),
                        Triple("Q&A", Icons.Default.HelpOutline, if (model.isHindiMode) "प्रश्नोत्तरी (Q&A)" else "Q&A (FAQ)"),
                        Triple("POLICIES", Icons.Default.Policy, if (model.isHindiMode) "गोपनीयता नीतियां" else "Policies"),
                        Triple("APP", Icons.Default.Apps, if (model.isHindiMode) "ऐप परिचय" else "About App"),
                        Triple("SECURITY", Icons.Default.Security, if (model.isHindiMode) "सुरक्षा शील्ड" else "About Security"),
                        Triple("CREATOR", Icons.Default.Person, if (model.isHindiMode) "अशमित गौतम (निर्माता)" else "Ashmit Gautam")
                    )

                    tabs.forEachIndexed { index, (key, icon, label) ->
                        val isSelected = selectedTab == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TealAccent else SlatePrimary
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = key,
                                    tint = if (isSelected) TealAccent else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                // Tab Contents
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> TermsAndConditionsTab(model, clipboardManager, context)
                        1 -> QuestionsAndAnswersTab(model)
                        2 -> AboutPoliciesTab(model, clipboardManager, context)
                        3 -> AboutApplicationTab(model)
                        4 -> AboutSecurityTab(model)
                        5 -> AboutAshmitGautamTab(model, clipboardManager, context)
                    }
                }
            }
        }
    }
}

// ==================== 1. TERMS & CONDITIONS (T&C) TAB ====================
@Composable
fun TermsAndConditionsTab(
    model: VaultViewModel,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Acceptance Status Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Consent active",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (model.isHindiMode) "नियम व शर्तें स्वीकृत स्थिति: सक्रिय" else "Terms of Service Status: Legally Active",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14532D)
                        )
                        Text(
                            text = if (model.isHindiMode)
                                "आपकी डिजिटल सहमति ऑन-डिवाइस सुरक्षा ऑडिट लॉग में सुरक्षित रूप से दर्ज है।"
                            else
                                "On-device cryptographic consent record timestamped in local secure preferences.",
                            fontSize = 10.sp,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }
        }

        // Section 1: Scope of Service & Data Sovereignty
        item {
            LegalClauseCard(
                clauseNumber = "1.0",
                title = if (model.isHindiMode) "सेवा का दायरा और डेटा संप्रभुता (Data Sovereignty)" else "Scope of Service & Local Data Sovereignty",
                content = if (model.isHindiMode)
                    "फैमिली इमरजेंसी वॉल्ट एक 100% ऑन-डिवाइस क्लाइंट एप्लिकेशन है। उपयोगकर्ता के सभी वित्तीय, कानूनी, स्वास्थ्य और नॉमिनी रिकॉर्ड केवल और केवल उपयोगकर्ता के अपने उपकरण के निजी SQLite डेटाबेस में संग्रहीत होते हैं। एप्लिकेशन का कोई भी रिमोट बैकएंड, क्लाउड सर्वर, या बाहरी डेटा सिंक नहीं है। उपयोगकर्ता अपने डेटा का एकमात्र और पूर्ण स्वामी (Sole Data Sovereign) है।"
                else
                    "Family Emergency Vault is provided exclusively as an on-device local continuity software solution. All financial, medical, succession, and nominee data resides strictly within the internal Linux application sandbox on this device. The developer and the application operate zero remote servers or telemetry pipelines. The user retains complete, uncompromised sovereignty over their records."
            )
        }

        // Section 2: MPIN & Secret Recovery Responsibility
        item {
            LegalClauseCard(
                clauseNumber = "2.0",
                title = if (model.isHindiMode) "मास्टर MPIN और बैकअप की जिम्मेदारी" else "User Responsibility for MPIN & Vault Backups",
                content = if (model.isHindiMode)
                    "क्योंकि ऐप कोई ऑनलाइन पासवर्ड-रीसेट सर्वर संचालित नहीं करता, इसलिए 4-अंकों का मास्टर MPIN याद रखना या सुरक्षित रखना पूरी तरह उपयोगकर्ता की जिम्मेदारी है। उपयोगकर्ता को सलाह दी जाती है कि वे सेटिंग्स से 'एन्क्रिप्टेड बैकअप (JSON)' समय-समय पर एक्सपोर्ट करके किसी सुरक्षित पेनड्राइव या विश्वस्त नॉमिनी के साथ रखें।"
                else
                    "Because the architecture operates without an online backend, the developer cannot recover lost Master MPINs. The user is solely responsible for remembering their 4-digit access PIN and maintaining offline encrypted JSON backups to avoid unrecoverable hardware failure or device loss."
            )
        }

        // Section 3: Legal Distinction: Nominee vs Legal Heir
        item {
            LegalClauseCard(
                clauseNumber = "3.0",
                title = if (model.isHindiMode) "महत्वपूर्ण कानूनी अंतर: नॉमिनी बनाम कानूनी उत्तराधिकारी (Nominee vs Legal Heir)" else "Legal Distinction: Nominee vs Legal Heir (Indian Law)",
                content = if (model.isHindiMode)
                    "भारतीय कानून (हिंदू उत्तराधिकार अधिनियम 1956, भारतीय उत्तराधिकार अधिनियम 1925, और सर्वोच्च न्यायालय के ऐतिहासिक निर्णय जैसे राम चंदर तलवार बनाम देवेंद्र कुमार तलवार) के अनुसार, बैंक या बीमा में नॉमिनी केवल एक 'ट्रस्टी/कस्टोडियन' होता है, जो संपत्ति प्राप्त करने का हकदार है ताकि वह उसे मृतक के वास्तविक कानूनी उत्तराधिकारियों (वसीयत या उत्तराधिकार नियमों के अनुसार) को सौंप सके। नॉमिनी स्वचालित रूप से संपत्ति का अंतिम मालिक नहीं बन जाता जब तक कि उसके पक्ष में वैध वसीयत न हो।"
                else
                    "Under Indian succession laws (including the Hindu Succession Act 1956, Indian Succession Act 1925, and landmark Supreme Court rulings such as Ram Chander Talwar v. Devinder Kumar Talwar), a nominee is legally recognized as a 'trustee/custodian' empowered to receive funds from banks/insurers to prevent procedural deadlock. A nominee does not automatically supersede legal heirs entitled under statutory succession or a valid Will."
            )
        }

        // Section 4: Emergency Protocols & Disclaimer
        item {
            LegalClauseCard(
                clauseNumber = "4.0",
                title = if (model.isHindiMode) "आपातकालीन प्रोटोकॉल, लोकेशन और कॉल डिस्क्लेमर" else "Emergency SOS, Location & Dialer Disclaimers",
                content = if (model.isHindiMode)
                    "1-टैप आपातकालीन SOS और लोकेशन एसएमएस सुविधा आपके स्थानीय सेल्युलर नेटवर्क (GSM SMS) और डिवाइस जीपीएस पर निर्भर करती है। ऐप कोई प्रत्यक्ष टेलीकॉम सेवा प्रदान नहीं करता। आपातकालीन स्थिति में परिवार के सदस्यों, डॉक्टरों या एम्बुलेंस को कॉल करना उपयोगकर्ता की डिवाइस डायलर और नेटवर्क कवरेज पर आधारित है।"
                else
                    "1-Tap Emergency SOS coordinates dispatch functions via standard offline GSM SMS and on-device GPS hardware. The software does not provide cellular carrier services. Calling doctors, lawyers, or emergency dispatchers triggers the system dialer subject to local carrier signal availability."
            )
        }

        // Section 5: Limitation of Liability
        item {
            LegalClauseCard(
                clauseNumber = "5.0",
                title = if (model.isHindiMode) "दायित्व की सीमा (Limitation of Liability)" else "Limitation of Liability & As-Is Provision",
                content = if (model.isHindiMode)
                    "यह एप्लिकेशन 'जैसा है' (AS-IS) आधार पर बिना किसी वारंटी के प्रदान किया गया है। डेवलपर किसी भी डेटा हानि, हार्डवेयर क्षति, या तृतीय-पक्ष विवाद के लिए उत्तरदायी नहीं होगा। कानूनी और वित्तीय निर्णय लेने से पहले किसी योग्य चार्टर्ड एकाउंटेंट (CA) या कानूनी सलाहकार से परामर्श करें।"
                else
                    "This software is provided 'AS-IS' without warranty of any kind, express or implied. The developer shall not be liable for device damage, forgotten credentials, or disputes arising between family claimants. Users are encouraged to consult qualified legal counsel and chartered accountants for formal estate planning."
            )
        }

        // Copy Button
        item {
            Button(
                onClick = {
                    val termsText = "FAMILY EMERGENCY VAULT - TERMS OF SERVICE\n\n" +
                            "1.0 Scope of Service: 100% On-Device Local Data Sovereignty.\n" +
                            "2.0 User Responsibility: Master MPIN and encrypted offline backup management.\n" +
                            "3.0 Legal Precedents: Nominee as trustee vs Legal Heir succession rights under Indian Succession Act 1925.\n" +
                            "4.0 Emergency SOS: Local GSM & GPS dispatch.\n" +
                            "5.0 Limitation of Liability: Provided as-is for family emergency continuity."
                    clipboardManager.setText(AnnotatedString(termsText))
                    Toast.makeText(context, "Terms & Conditions copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (model.isHindiMode) "पूर्ण नियम व शर्तें कॉपी करें" else "Copy Complete T&C to Clipboard",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==================== 2. QUESTIONS & ANSWERS (Q&A / FAQ) TAB ====================
@Composable
fun QuestionsAndAnswersTab(model: VaultViewModel) {
    var searchQuery by remember { mutableStateOf("") }

    val allFaqs = remember(model.isHindiMode) {
        if (model.isHindiMode) listOf(
            FaqItem(
                question = "मेरा सारा डेटा कहाँ स्टोर होता है? क्या यह किसी सर्वर पर जाता है?",
                answer = "आपका सारा डेटा केवल आपके फोन की आंतरिक मेमोरी (Private SQLite Database) में रहता है। कोई भी बाहरी सर्वर, इंटरनेट डेटाबेस या क्लाउड कंपनी आपका डेटा नहीं देखती। इंटरनेट बंद होने पर भी सब कुछ सुचारू रूप से चलता है।",
                category = "सुरक्षा व डेटा"
            ),
            FaqItem(
                question = "अगर मेरा फोन खो जाए या खराब हो जाए, तो क्या डेटा वापस मिलेगा?",
                answer = "हाँ! इसके लिए आपको सेटिंग्स में जाकर 'एन्क्रिप्टेड बैकअप (JSON)' को पहले से एक्सपोर्ट करके अपने ईमेल, पेनड्राइव या परिवार के किसी सदस्य को सुरक्षित रखना होगा। नए फोन में ऐप डाउनलोड करके 'Restore Vault from Backup' पर टैप करने से सारा डेटा तुरंत रीस्टोर हो जाएगा।",
                category = "बैकअप व रीस्टोर"
            ),
            FaqItem(
                question = "क्या नॉमिनी (Nominee) बैंक खाते या पैसे का अंतिम मालिक बन जाता है?",
                answer = "नहीं! भारतीय कानून और सुप्रीम कोर्ट के अनुसार, नॉमिनी सिर्फ एक 'ट्रस्टी' (देखभालकर्ता) होता है। बैंक बिना देरी के नॉमिनी को पैसे सौंप देता है, लेकिन नॉमिनी को वह राशि मृतक के वैध कानूनी उत्तराधिकारियों (पति/पत्नी, बच्चों आदि) को वसीयत या कानून के अनुसार देनी होती है।",
                category = "कानूनी नियम"
            ),
            FaqItem(
                question = "आरबीआई (RBI) के अनुसार मृत्यु के बाद बैंक से क्लेम कितने दिन में मिलता है?",
                answer = "आरबीआई के दिशानिर्देशों के तहत, मृत्यु प्रमाण पत्र (Death Certificate) और आवश्यक केवाईसी जमा करने के 15 दिनों के भीतर बैंक को नॉमिनी के क्लेम का निपटारा करना अनिवार्य है। इसके लिए किसी कोर्ट सक्सेशन सर्टिफिकेट की जरूरत नहीं होती यदि नॉमिनी पंजीकृत है।",
                category = "बैंक क्लेम"
            ),
            FaqItem(
                question = "अगर मैं अपना 4-अंकों का मास्टर MPIN भूल जाऊं तो क्या होगा?",
                answer = "यदि आपने बायोमेट्रिक (फिंगरप्रिंट/फेस) चालू किया हुआ है, तो आप बायोमेट्रिक्स से तुरंत अनलॉक करके नया MPIN सेट कर सकते हैं। सुरक्षा कारणों से कोई रिमोट रिसेट नहीं है, इसलिए MPIN को सुरक्षित याद रखें।",
                category = "लॉगिन व पिन"
            ),
            FaqItem(
                question = "1-टैप आपातकालीन SOS बिना इंटरनेट के कैसे काम करता है?",
                answer = "यह आपके फोन के आंतरिक जीपीएस सेंसर और ऑफलाइन सेल्युलर एसएमएस (GSM Network) का उपयोग करता है। यह तुरंत आपके वर्तमान सटीक अक्षांश-देशांतर (Latitude/Longitude) और गूगल मैप्स लिंक को आपातकालीन संपर्कों को सीधे संदेश भेज देता है।",
                category = "आपातकाल SOS"
            ),
            FaqItem(
                question = "घर में असली कागजात (Original Documents) ढूंढने में यह ऐप कैसे मदद करता है?",
                answer = "हर संपत्ति और पॉलिसी में हमने 'भौतिक दस्तावेज स्थान (Physical Document Location)' का विकल्प दिया है। जैसे 'मास्टर बेडरूम की अलमारी - नीली फाइल'। आपातकाल में परिवार को बिना किसी उलझन के कागजात तुरंत मिल जाते हैं।",
                category = "दस्तावेज प्रबंधन"
            ),
            FaqItem(
                question = "क्या यह ऐप कोई सदस्यता शुल्क या मासिक चार्ज लेता है?",
                answer = "बिल्कुल नहीं! यह 100% मुफ्त, ओपन और ऑफलाइन परिवार सहायता ऐप है। इसमें कोई विज्ञापन, कोई ट्रैकर और कोई सब्सक्रिप्शन नहीं है।",
                category = "सामान्य"
            )
        ) else listOf(
            FaqItem(
                question = "Where is my data stored? Does any information go to external servers?",
                answer = "Your data is stored 100% locally in your phone's sandboxed private SQLite database. Zero bytes of your financial, family, or credential data are transmitted to the cloud or third-party servers. It works completely offline.",
                category = "Storage & Privacy"
            ),
            FaqItem(
                question = "What if my phone gets lost, stolen, or broken?",
                answer = "Because there is no remote cloud server, you should regularly export an 'Encrypted JSON Backup' from the Settings screen. Store this backup file on a secure USB drive or share it with your primary nominee. On a new device, simply select 'Restore Vault from Backup'.",
                category = "Backup & Recovery"
            ),
            FaqItem(
                question = "Does a Nominee legally own the money in bank accounts and insurance?",
                answer = "No. Under Indian succession laws and Supreme Court rulings, a nominee acts only as a designated 'custodian/trustee'. The financial institution releases the money to the nominee to prevent delays, but the nominee must distribute the proceeds to the rightful legal heirs under the Will or succession act.",
                category = "Legal & Nominee"
            ),
            FaqItem(
                question = "How quickly do banks settle deceased depositor claims under RBI rules?",
                answer = "Under RBI circular DBOD.No.Leg.BC.95/09.07.005/2004-05 (and subsequent revisions), banks must settle deceased depositor claims to the registered nominee within 15 calendar days of receiving the death certificate and claimant KYC, without demanding a succession certificate.",
                category = "RBI Guidelines"
            ),
            FaqItem(
                question = "What happens if I forget my 4-digit Master MPIN?",
                answer = "If you have enabled Biometric Authentication (fingerprint or face unlock), you can decrypt the vault instantly with your biometric scan and update your MPIN in Settings. Otherwise, keep your master PIN noted in a safe offline location.",
                category = "Security & PIN"
            ),
            FaqItem(
                question = "How does the 1-Tap Emergency SOS dispatch work without internet?",
                answer = "The emergency dispatcher uses your phone's native GPS chip to calculate real-time coordinates and sends a standardized offline GSM SMS with Google Maps coordinates directly to your designated emergency contacts.",
                category = "Emergency SOS"
            ),
            FaqItem(
                question = "How does the Physical Document Locator prevent family chaos?",
                answer = "For every asset, policy, and locker, you can specify the exact real-world storage location (e.g., 'Bedroom Cupboard, Shelf 2, Blue Binder'). During medical or legal crises, family members can pinpoint paper documents instantly.",
                category = "Document Tracking"
            ),
            FaqItem(
                question = "Does this application charge monthly subscriptions or display advertisements?",
                answer = "No. Family Emergency Vault is completely free, non-commercial, and ad-free. It was created with pure dedication to privacy, public service, and family resilience.",
                category = "General"
            )
        )
    }

    val filteredFaqs = remember(allFaqs, searchQuery) {
        if (searchQuery.isBlank()) allFaqs
        else allFaqs.filter {
            it.question.contains(searchQuery, ignoreCase = true) ||
            it.answer.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (model.isHindiMode) "प्रश्नों में खोजें..." else "Search questions & answers...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealAccent,
                unfocusedBorderColor = SlateBorder
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredFaqs) { faq ->
                FaqAccordionCard(faq)
            }
        }
    }
}

data class FaqItem(
    val question: String,
    val answer: String,
    val category: String
)

@Composable
fun FaqAccordionCard(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.category,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlatePrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.question,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = SlatePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = SlateBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.answer,
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

// ==================== 3. ABOUT POLICIES TAB ====================
@Composable
fun AboutPoliciesTab(
    model: VaultViewModel,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Zero-Data Privacy Policy
        item {
            PolicySectionCard(
                icon = Icons.Default.Shield,
                title = if (model.isHindiMode) "1. शून्य-डेटा संग्रह गोपनीयता नीति (Strict Privacy Policy)" else "1. Zero-Collection Privacy Policy",
                badge = "NO TRACKERS",
                badgeColor = Color(0xFF059669),
                content = if (model.isHindiMode)
                    "हम आपके बारे में कोई भी व्यक्तिगत जानकारी एकत्र नहीं करते हैं। कोई विज्ञापन आईडी, डिवाइस फिंगरप्रिंटिंग, या एनालिटिक्स एसडीके (Google Analytics, Firebase Crashlytics, आदि) इस एप्लिकेशन में शामिल नहीं हैं। आपकी निजी वित्तीय जानकारी केवल आपके फोन पर एन्क्रिप्टेड रहती है।"
                else
                    "We collect zero personal information. No advertising identifiers, analytics SDKs (such as Firebase Analytics or Meta Pixel), or remote logging endpoints exist within this build. Your financial details never leave your hardware."
            )
        }

        // Data Retention & Deletion Policy
        item {
            PolicySectionCard(
                icon = Icons.Default.DeleteSweep,
                title = if (model.isHindiMode) "2. डेटा प्रतिधारण और पूर्ण निष्कासन नीति (Data Deletion)" else "2. Data Retention & Full Purge Policy",
                badge = "100% USER CONTROL",
                badgeColor = Color(0xFF0284C7),
                content = if (model.isHindiMode)
                    "जब तक आप ऐप का उपयोग करते हैं, डेटा आपके डिवाइस पर रहता है। यदि आप ऐप को अनइंस्टॉल करते हैं या ऐप सेटिंग्स से 'Clear Data' करते हैं, तो डेटाबेस पूरी तरह और अपरिवर्तनीय रूप से हमेशा के लिए नष्ट हो जाता है। कोई शैडो कॉपी या बैकअप सर्वर मौजूद नहीं है।"
                else
                    "Data exists only for as long as you maintain the application on your device. Clearing app storage in Android OS settings or resetting defaults permanently purges the SQLite database without lingering cloud residues."
            )
        }

        // RBI Deceased Claims Circular Compliance Policy
        item {
            PolicySectionCard(
                icon = Icons.Default.AccountBalance,
                title = if (model.isHindiMode) "3. आरबीआई मृतक जमाकर्ता परिपत्र अनुपालन (RBI Guidelines)" else "3. RBI Deceased Depositor Circular Compliance",
                badge = "RBI MANDATE",
                badgeColor = Color(0xFF7C3AED),
                content = if (model.isHindiMode)
                    "भारतीय रिज़र्व बैंक (RBI Master Circular No. DBOD.No.Leg.BC.95/09.07.005/2004-05) के अनुसार, यदि बैंक खाते में वैध नॉमिनी दर्ज है, तो मृत्यु प्रमाण पत्र और पहचान पत्र प्राप्त होने के 15 दिनों के भीतर बैंक को राशि जारी करनी होती है। हमारा ऐप इसी प्रक्रिया के अनुसार कागजात व्यवस्थित रखने में मार्गदर्शन करता है।"
                else
                    "In alignment with Reserve Bank of India Master Circulars on settlement of claims in respect of deceased depositors, banks are directed to settle claims to registered nominees within 15 days upon receipt of death certificate and claimant KYC. This app prepares families to meet these exact compliance requirements."
            )
        }

        // IRDAI & SEBI Alignment Policy
        item {
            PolicySectionCard(
                icon = Icons.Default.Verified,
                title = if (model.isHindiMode) "4. बीमा (IRDAI) और सेबी (SEBI) अनुपालन नीति" else "4. IRDAI Insurance & SEBI Demat Alignment",
                badge = "REGULATORY",
                badgeColor = Color(0xFFD97706),
                content = if (model.isHindiMode)
                    "बीमा नियामक (IRDAI) के 30-दिवसीय डेथ क्लेम निपटान नियम और सेबी (SEBI) के म्यूचुअल फंड/डीमैट ट्रांसफर नियमों के तहत, पॉलिसी नंबर और नॉमिनी विवरण सही होने पर परिवार को क्लेम प्राप्त करने में कोई कानूनी बाधा नहीं आती।"
                else
                    "Formulated in accordance with IRDAI protection of policyholders' interests (mandatory death claim settlement timelines) and SEBI guidelines for seamless transmission of mutual fund units and demat shares directly to verified nominees."
            )
        }
    }
}

// ==================== 4. ABOUT APPLICATION TAB ====================
@Composable
fun AboutApplicationTab(model: VaultViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Mission Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlatePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TealAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = TealAccent, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Family Emergency & Continuity Vault",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Version 1.0.4 • 100% Offline Emergency Ledger",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (model.isHindiMode)
                            "भारत में ₹78,000 करोड़ से अधिक की वित्तीय संपत्तियां (बैंक खाते, LIC पॉलिसियां, शेयर, PF) लावारिस पड़ी हैं क्योंकि परिजनों को यह पता ही नहीं था कि उनके कमाने वाले सदस्य के खाते कहाँ हैं। फैमिली इमरजेंसी वॉल्ट इसी गंभीर सामाजिक और पारिवारिक संकट का स्थायी और सुरक्षित समाधान है।"
                        else
                            "Across India, over ₹78,000 Crores in unclaimed bank balances, life insurance policies, shares, and provident funds lie dormant simply because families were unaware of account numbers or physical documents. Family Emergency Vault was built to permanently solve this tragic information asymmetry.",
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // App Mobile Showcase Visual Banner
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_mobile_showcase_1789194560982),
                        contentDescription = "Family Vault Mobile App Showcase",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (model.isHindiMode) "🛡️ 100% ऑन-डिवाइस सुरक्षित मोबाइल इंटरफ़ेस" else "🛡️ Secure 100% On-Device Mobile Interface Showcase",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlatePrimary
                        )
                    }
                }
            }
        }

        // Core Modules Breakdown
        item {
            Text(
                text = if (model.isHindiMode) "एप्लिकेशन के प्रमुख सुरक्षा व निरंतरता मॉड्यूल" else "Key Application Architecture Modules",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlatePrimary
            )
        }

        val features = listOf(
            Triple(Icons.Default.Lock, "Encrypted Asset Ledger", "Secure records for Bank Accounts, Term Insurance, Health Mediclaim, Mutual Funds, Demat, and Safe Lockers."),
            Triple(Icons.Default.FolderOpen, "Physical Document Locator", "Pinpoints the exact cupboard, drawer, and binder shelf for paper deeds and locker master keys."),
            Triple(Icons.Default.FamilyRestroom, "Nominee & Dependent Matrix", "Maps verified nominee registration status and emergency guardianship for minors and elderly parents."),
            Triple(Icons.Default.Checklist, "24h / 7d / 30d Action Playbook", "Step-by-step crisis workflow guiding grieving families through death certificates, frozen accounts, and claims."),
            Triple(Icons.Default.SmartToy, "Offline AI Continuity Advisor", "On-device rulebook advisor for instant legal, RBI, and insurance settlement protocols without internet."),
            Triple(Icons.Default.Emergency, "1-Tap Offline GPS SOS Dispatcher", "Broadcasts real-time emergency coordinates via native GSM SMS directly to loved ones.")
        )

        features.forEach { (icon, title, desc) ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SlateLightBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = SlatePrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                        Text(text = desc, fontSize = 10.sp, color = Color.Gray, lineHeight = 13.sp)
                    }
                }
            }
        }
    }
}

// ==================== 5. ABOUT SECURITY TAB ====================
@Composable
fun AboutSecurityTab(model: VaultViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.img_family_continuity_shield_1789320119569),
                        contentDescription = "Family Protection & Cryptographic Shield",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (model.isHindiMode) "🛡️ परिवार की अखंड सुरक्षा और शून्य डेटा रिसाव" else "🛡️ Uncompromising Family Protection • Zero Data Leakage",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlatePrimary
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (model.isHindiMode) "सैन्य-ग्रेड ऑन-डिवाइस सुरक्षा आर्किटेक्चर" else "Military-Grade Cryptographic & Defense Architecture",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlatePrimary
            )
        }

        val securityPillars = listOf(
            SecurityPillarItem(
                title = "Android Linux Process Sandbox Isolation",
                badge = "KERNEL LEVEL",
                badgeColor = Color(0xFF059669),
                desc = "The operating system allocates a dedicated Linux UID to this app. External apps, web browsers, and background services cannot inspect or cross into the private directory."
            ),
            SecurityPillarItem(
                title = "Hardware Biometric Authentication (KeyStore)",
                badge = "TEE / STRONGBOX",
                badgeColor = Color(0xFF0284C7),
                desc = "Master cryptographic key is verified using Android KeyStore and BiometricPrompt (Fingerprint / Face ID), ensuring uncompromised physical security."
            ),
            SecurityPillarItem(
                title = "Salted Cryptographic MPIN Security",
                badge = "SHA-256 HASHED",
                badgeColor = Color(0xFF7C3AED),
                desc = "Your 4-digit Master MPIN is never stored in plaintext. It is cryptographically hashed with local salts to prevent brute-force attacks."
            ),
            SecurityPillarItem(
                title = "Zero WebViews & Zero-Phishing Immunity",
                badge = "0% PHISHING RISK",
                badgeColor = Color(0xFF0D9488),
                desc = "The application contains zero remote webviews, eliminate credential harvesting or phishing redirects entirely by design."
            ),
            SecurityPillarItem(
                title = "5-Minute Idle Auto-Lock Sentinel",
                badge = "SESSION GUARD",
                badgeColor = Color(0xFFD97706),
                desc = "Active background timer locks the interface immediately after 5 minutes of idle status, protecting against unattended device exposure."
            ),
            SecurityPillarItem(
                title = "Role-Based Field Redaction",
                badge = "ROLE MASKING",
                badgeColor = Color(0xFF475569),
                desc = "Granular roles (Owner, Spouse, Nominee, Guest) ensure that confidential coordinates, locker combinations, and passwords remain masked for restricted viewers."
            )
        )

        items(securityPillars) { pillar ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pillar.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlatePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = pillar.badgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = pillar.badge,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = pillar.badgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pillar.desc,
                        fontSize = 10.sp,
                        color = Color(0xFF475569),
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

data class SecurityPillarItem(
    val title: String,
    val badge: String,
    val badgeColor: Color,
    val desc: String
)

// ==================== 6. ABOUT ASHMIT GAUTAM TAB ====================
@Composable
fun AboutAshmitGautamTab(
    model: VaultViewModel,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card with Avatar & Spotlight
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlatePrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Image with Glow Ring
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(3.dp, TealAccent, CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_creator_ashmit_1789235489297),
                            contentDescription = "Ashmit Gautam - Creator & Architect",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Ashmit Gautam",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Surface(
                        color = TealAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "CREATOR & LEAD SOFTWARE ARCHITECT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Crafted with dedication for family continuity, emergency resilience, and uncompromised on-device privacy.",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Contact & Email Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:ashmit4933@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Family Emergency Vault Inquiry / Feedback - Ashmit Gautam")
                                }
                                try {
                                    context.startActivity(emailIntent)
                                } catch (e: Exception) {
                                    clipboardManager.setText(AnnotatedString("ashmit4933@gmail.com"))
                                    Toast.makeText(context, "Email copied: ashmit4933@gmail.com", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = SlatePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Email Ashmit", color = SlatePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("ashmit4933@gmail.com"))
                                Toast.makeText(context, "Ashmit Gautam's email copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, Color(0xFF64748B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Copy Details", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Creator's Mission & Vision
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatQuote, contentDescription = null, tint = TealAccent, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (model.isHindiMode) "निर्माता की दृष्टि (Architect's Vision)" else "The Vision Behind This Build",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlatePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (model.isHindiMode)
                            "\"मैंने फैमिली इमरजेंसी वॉल्ट का निर्माण इसलिए किया ताकि किसी भी अप्रत्याशित संकट, गंभीर बीमारी या आकस्मिक दुर्घटना के समय किसी भी परिवार को वित्तीय लाचारी या कानूनी उलझनों का सामना न करना पड़े। हर परिवार के पास एक ऐसा सुरक्षित, 100% निजी और गोपनीय साधन होना चाहिए जो उनकी मेहनत की कमाई और कागजातों को बिना किसी क्लाउड लीक के उनके अपनों तक सुरक्षित पहुंचा सके।\"\n\n— अशमित गौतम (Ashmit Gautam)"
                        else
                            "\"I architected the Family Emergency & Continuity Vault to solve a deeply personal and systemic crisis that millions of families face during medical emergencies or sudden loss. In an era where financial lives are fragmented across dozens of portals and physical lockers, grieving families shouldn't have to battle bureaucratic confusion to secure their rightful inheritance. Every line of code was crafted under the uncompromised standard that family privacy and continuity are sacred.\"\n\n— Ashmit Gautam",
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Architectural Innovations by Ashmit Gautam
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (model.isHindiMode) "अशमित गौतम द्वारा विकसित तकनीकी नवाचार" else "Key Technical Innovations by Ashmit Gautam",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary
                    )

                    InnovationRow(
                        number = "01",
                        title = "Zero-Cloud Local Sovereignty",
                        desc = "Eliminated all remote backend vulnerabilities, ensuring zero cloud breaches and zero recurring subscription fees."
                    )
                    InnovationRow(
                        number = "02",
                        title = "Offline AI Continuity Advisor",
                        desc = "Built a zero-cloud advisory engine explaining RBI claims, nominee mandates, and succession acts anywhere, anytime."
                    )
                    InnovationRow(
                        number = "03",
                        title = "1-Tap GSM GPS Emergency Dispatcher",
                        desc = "Architected a life-saving offline SMS alert broadcasting real-time coordinates to emergency family contacts."
                    )
                    InnovationRow(
                        number = "04",
                        title = "Self-Healing SQLite WAL Observability",
                        desc = "Implemented sub-millisecond query telemetry with 1-tap SQLite VACUUM page defragmentation and checkpointing."
                    )
                }
            }
        }

        // Developer Profile Details Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateLightBg),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Lead Architect", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "Ashmit Gautam", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                    }
                    Divider(color = SlateBorder.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Official Email", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "ashmit4933@gmail.com", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                    }
                    Divider(color = SlateBorder.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Engineering Framework", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "Android Kotlin • Jetpack Compose M3", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                    }
                    Divider(color = SlateBorder.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Database Engine", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "Room SQLite WAL • Zero Cloud Egress", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                    }
                    Divider(color = SlateBorder.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Build License", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "Proprietary • Family Protection Public Service", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }
                }
            }
        }
    }
}

// Helper Composables
@Composable
fun LegalClauseCard(clauseNumber: String, title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = SlatePrimary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = clauseNumber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlatePrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                fontSize = 10.sp,
                color = Color(0xFF334155),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun PolicySectionCard(
    icon: ImageVector,
    title: String,
    badge: String,
    badgeColor: Color,
    content: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary
                    )
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                fontSize = 10.sp,
                color = Color(0xFF334155),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun InnovationRow(number: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateLightBg, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = number,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = TealAccent,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
            Text(text = desc, fontSize = 10.sp, color = Color.Gray, lineHeight = 13.sp)
        }
    }
}
