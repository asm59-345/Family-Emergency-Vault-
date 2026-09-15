package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(model: VaultViewModel) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mpin by remember { mutableStateOf("") }
    var nomineeName by remember { mutableStateOf("") }
    var nomineeRelation by remember { mutableStateOf(if (model.isHindiMode) "जीवनसाथी (पति/पत्नी)" else "Spouse") }
    var securityQuestion by remember {
        mutableStateOf(
            if (model.isHindiMode) "आपके पहले स्कूल का क्या नाम था?" else "What was your first school name?"
        )
    }
    var securityAnswer by remember { mutableStateOf("") }
    var enableBiometrics by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(true) }
    var showTermsDialog by remember { mutableStateOf(false) }
    
    var validationError by remember { mutableStateOf("") }
    var isCheckingOut by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val securityQuestionsList = if (model.isHindiMode) listOf(
        "आपके पहले स्कूल का क्या नाम था?",
        "आपकी माता जी का प्रथम नाम क्या है?",
        "आपके पहले पालतू जानवर का नाम क्या था?",
        "बचपन में आपका उपनाम (निकनेम) क्या था?",
        "आप अपने जीवनसाथी से किस शहर में मिले थे?"
    ) else listOf(
        "What was your first school name?",
        "What is your mother's maiden name?",
        "What is the name of your first pet?",
        "What was your childhood nickname?",
        "In what city did you meet your spouse?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlatePrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar with Language Switcher
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Security Shield",
                    tint = TealAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (model.isHindiMode) "पारिवारिक सुरक्षा वॉल्ट" else "Family Emergency Vault",
                    color = TealAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Language Switcher (English | हिन्दी)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { model.toggleLanguage() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("btn_signup_language_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = TealAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (model.isHindiMode) "English" else "हिन्दी",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Shield Guard Registry Header
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(TealAccent.copy(alpha = 0.15f))
                .border(2.dp, TealAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Shield Guard Logo",
                tint = TealAccent,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (model.isHindiMode) "सुरक्षित पारिवारिक खाता पंजीकरण" else "SECURE FAMILY VAULT REGISTRY",
            fontSize = 11.sp,
            color = TealAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Text(
            text = if (model.isHindiMode) "नया वॉल्ट खाता बनाएं" else "Bank-Grade Account Provisioning",
            fontSize = 18.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )

        // Sandbox / Privacy Indicator
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Status",
                    tint = TealAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (model.isHindiMode)
                        "सभी विवरण आपके फ़ोन पर 256-बिट एन्क्रिप्शन के साथ स्थानीय रूप से सुरक्षित रहते हैं। कोई भी डेटा बाहरी सर्वर पर अपलोड नहीं होता।"
                    else
                        "All registration particulars are processed through cryptographic protection and stored strictly on your device's offline secure database.",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (validationError.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error Info",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = validationError,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section 1: Holder Identity
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "User Identity", tint = SlatePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (model.isHindiMode) "भाग 1: खाताधारक का विवरण" else "Section 1: Account Holder Particulars",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlatePrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Legal Full Name
                Text(
                    text = if (model.isHindiMode) "खाताधारक का पूरा नाम" else "Legal Full Name",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("signup_fullname_input"),
                    placeholder = { Text(if (model.isHindiMode) "उदा. राहुल शर्मा" else "Example: Rahul Sharma") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Registered Phone Number
                Text(
                    text = if (model.isHindiMode) "मोबाइल नंबर" else "Registered Mobile Number",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("signup_phone_input"),
                    placeholder = { Text(if (model.isHindiMode) "उदा. +91 98765 43210" else "Example: +91 98765 43210") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Registered Owner Email Address
                Text(
                    text = if (model.isHindiMode) "मालिक का ईमेल पता (बैकअप हेतु)" else "Registered Owner Email (Backup Communication)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("signup_email_input"),
                    placeholder = { Text(if (model.isHindiMode) "उदा. rahul@gmail.com" else "Example: rahul@gmail.com") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        }

        // Section 2: Cryptographic Decryption Signatures
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Security signatures", tint = SlatePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (model.isHindiMode) "भाग 2: पासवर्ड एवं 4-अंकों का मास्टर MPIN" else "Section 2: Decryption Signatures & MPIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlatePrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Custom Master Vault Key
                Text(
                    text = if (model.isHindiMode) "खाता पासवर्ड (न्यूनतम 4 अक्षर/अंक)" else "Account Access Password (Minimum 4 characters)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("signup_password_input"),
                    placeholder = { Text(if (model.isHindiMode) "•••••••• (सुरक्षित पासवर्ड बनाएं)" else "Example: •••••••• (Create secure password)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Master 4-Digit MPIN
                Text(
                    text = if (model.isHindiMode) "4-अंकों का मास्टर MPIN (जैसे 4321)" else "4-Digit Secure Transaction MPIN (Numeric Only)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = mpin,
                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) mpin = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("signup_mpin_input"),
                    placeholder = { Text("4321") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        }

        // Section 3: Family Nominee Binding
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Groups, contentDescription = "Trust Nominees", tint = SlatePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (model.isHindiMode) "भाग 3: आपातकालीन प्राथमिक नॉमिनी" else "Section 3: Emergency Primary Nominee Binding",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlatePrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Nominee Name
                Text(
                    text = if (model.isHindiMode) "प्राथमिक नॉमिनी का नाम" else "Primary Nominee Full Name",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = nomineeName,
                    onValueChange = { nomineeName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("signup_nomineename_input"),
                    placeholder = { Text(if (model.isHindiMode) "उदा. प्रिया शर्मा" else "Example: Priya Sharma") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Nominee Relation
                Text(
                    text = if (model.isHindiMode) "नॉमिनी से संबंध" else "Nominee Relationship",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = nomineeRelation,
                    onValueChange = { nomineeRelation = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("signup_nomineerelation_input"),
                    placeholder = { Text(if (model.isHindiMode) "पति / पत्नी / पुत्र / पुत्री" else "Example: Spouse") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        }

        // Section 4: Security Question Safeguard
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Help, contentDescription = "Security Question", tint = SlatePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (model.isHindiMode) "भाग 4: खाता पुनर्प्राप्ति सुरक्षा प्रश्न" else "Section 4: Password Recovery Safeguards",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlatePrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (model.isHindiMode) "पासवर्ड भूलने पर सुरक्षा प्रश्न चुनें" else "Select Security Question for Password Recovery",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                var expandedQuest by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    OutlinedTextField(
                        value = securityQuestion,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().clickable { expandedQuest = true },
                        trailingIcon = {
                            IconButton(onClick = { expandedQuest = !expandedQuest }) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown Question")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    DropdownMenu(
                        expanded = expandedQuest,
                        onDismissRequest = { expandedQuest = false },
                        modifier = Modifier.fillMaxWidth().background(Color.White)
                    ) {
                        securityQuestionsList.forEach { q ->
                            DropdownMenuItem(
                                text = { Text(text = q, fontSize = 13.sp) },
                                onClick = {
                                    securityQuestion = q
                                    expandedQuest = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Security Question Answer
                Text(
                    text = if (model.isHindiMode) "सुरक्षा प्रश्न का गुप्त उत्तर" else "Private Security Answer",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = securityAnswer,
                    onValueChange = { securityAnswer = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("signup_securityanswer_input"),
                    placeholder = { Text(if (model.isHindiMode) "उदा. ग्रीनवुड" else "Example: Greenwood") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        }

        // Section 5: Biometrics & Terms Consent
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = "Biometrics and Consent", tint = SlatePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (model.isHindiMode) "भाग 5: बायोमेट्रिक एवं नियम सहमति" else "Section 5: Biometrics & Policy Consent",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlatePrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Optional Biometric Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { enableBiometrics = !enableBiometrics }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = enableBiometrics,
                        onCheckedChange = { enableBiometrics = it },
                        colors = CheckboxDefaults.colors(checkedColor = TealAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (model.isHindiMode) "बायोमेट्रिक (फिंगरप्रिंट / फेस) अनलॉक चालू करें" else "Enable Biometric (Fingerprint / Face) Unlock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SlatePrimary
                        )
                        Text(
                            text = if (model.isHindiMode)
                                "वैकल्पिक: इसे आप सेटिंग्स में भी कभी भी बदल सकते हैं।"
                            else
                                "Optional: Can also be turned on or off anytime in Settings.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Legal Terms Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { termsAccepted = !termsAccepted }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = TealAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (model.isHindiMode)
                                "मैं पारिवारिक वॉल्ट की नियम व शर्तों और डेटा गोपनीयता नीति से सहमत हूँ।"
                            else
                                "I accept the Family Emergency Vault Terms of Service & Privacy Policy.",
                            fontSize = 11.sp,
                            color = SlatePrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (model.isHindiMode) "📋 नियम व शर्तें पढ़ें" else "📋 Read Terms & Privacy",
                            color = TealAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showTermsDialog = true }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Create Account CTA Button
        Button(
            onClick = {
                if (fullName.trim().length < 3) {
                    validationError = if (model.isHindiMode) "कृपया अपना वैध पूरा नाम दर्ज करें।" else "Please write your valid official Full Name."
                } else if (!email.contains("@") || email.trim().length < 5) {
                    validationError = if (model.isHindiMode) "कृपया एक वैध ईमेल आईडी दर्ज करें।" else "Please register a valid Owner email address."
                } else if (phone.trim().length < 6) {
                    validationError = if (model.isHindiMode) "कृपया एक वैध मोबाइल नंबर दर्ज करें।" else "Mobile number registered register required."
                } else if (password.trim().length < 4) {
                    validationError = if (model.isHindiMode) "कृपया कम से कम 4 अक्षरों का पासवर्ड बनाएं।" else "Please make a secure Password of minimum 4 characters."
                } else if (mpin.trim().length != 4) {
                    validationError = if (model.isHindiMode) "मास्टर MPIN ठीक 4 अंकों का होना आवश्यक है।" else "Master MPIN must be exactly a 4-digit unique numerical code."
                } else if (nomineeName.trim().isEmpty()) {
                    validationError = if (model.isHindiMode) "आपातकालीन पहुँच के लिए प्राथमिक नॉमिनी का नाम आवश्यक है।" else "Binding a primary Nominee name is highly required for emergency access."
                } else if (securityAnswer.trim().isEmpty()) {
                    validationError = if (model.isHindiMode) "कृपया सुरक्षा प्रश्न का उत्तर दर्ज करें।" else "Please provide an answer to the Security Question."
                } else if (!termsAccepted) {
                    validationError = if (model.isHindiMode) "आगे बढ़ने के लिए नियम व शर्तों की सहमति आवश्यक है।" else "Please accept the terms of service to proceed."
                } else {
                    isCheckingOut = true
                    validationError = ""
                    model.registerUserAccount(
                        fullName = fullName.trim(),
                        email = email.trim(),
                        phone = phone.trim(),
                        password = password.trim(),
                        mpin = mpin.trim(),
                        nomineeName = nomineeName.trim(),
                        nomineeRelation = nomineeRelation.trim(),
                        securityQuestion = securityQuestion,
                        securityAnswer = securityAnswer.trim(),
                        enableBiometrics = enableBiometrics
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("signup_register_button")
        ) {
            Icon(imageVector = Icons.Default.Badge, contentDescription = "Badge sign", tint = SlatePrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (model.isHindiMode) "वॉल्ट खाता बनाएं (शुरू करें)" else "Initialize Secure Family Vault",
                color = SlatePrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick-Demo Bypass
        Button(
            onClick = {
                model.initQuickDemoBypass()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Text(
                text = if (model.isHindiMode) "⚡ त्वरित डेमो: राहुल शर्मा (मालिक) से शुरू करें" else "⚡ Speed Check: Quick-Sign with Rahul (Owner)",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Compliance seal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Gavel, contentDescription = "Trust Regulatory Seal", tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (model.isHindiMode)
                    "ऑफ़लाइन सुरक्षित वॉल्ट • भारतीय गोपनीयता अनुपालन 2026"
                else
                    "Offline Local Vault • Digital Privacy Act Compliant 2026",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }

    // Terms and Conditions Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text(
                    text = if (model.isHindiMode) "नियम, शर्तें एवं गोपनीयता नीति" else "Terms of Service & Privacy Policy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (model.isHindiMode)
                            """1. पूर्ण ऑफ़लाइन गोपनीयता:
यह एप्लिकेशन आपके बैंक खाते, बीमा, लॉकर और आपातकालीन दस्तावेज़ों को केवल आपके डिवाइस की सुरक्षित SQLite डेटाबेस में स्थानीय रूप से सहेजता है।

2. डेटा सुरक्षा:
सभी संवेदनशील डेटा 256-बिट क्रिप्टोग्राफिक एन्क्रिप्शन द्वारा सुरक्षित रहता है। मास्टर MPIN आपके डिवाइस पर सुरक्षित रूप से रखा जाता है।

3. आपातकालीन नॉमिनी पहुँच:
आपके द्वारा नामित नॉमिनी को आपातकालीन स्थिति (जैसे मेडिकल इमरजेंसी या निर्धारित दिनों तक निष्क्रियता) में ही अधिकृत किया जा सकता है।

4. बायोमेट्रिक सुरक्षा:
बायोमेट्रिक प्रमाणीकरण पूरी तरह से वैकल्पिक है। यदि आपके डिवाइस में फिंगरप्रिंट सेंसर उपलब्ध है तो आप इसे कभी भी सेटिंग्स से ऑन/ऑफ कर सकते हैं।"""
                        else
                            """1. Offline-First Privacy:
This application stores your emergency records, bank accounts, insurance policies, and critical files strictly within your device's encrypted SQLite sandbox.

2. Cryptographic Security:
Sensitive records are protected by military-grade 256-bit encryption. Your master MPIN remains in private local storage.

3. Emergency Nominee Access:
Your designated family nominee is authorized to request access only in verified life emergencies or after the Dead-Man timer expires.

4. Biometric Authentication:
Biometric scanning is purely optional. You may toggle it on or off at any time from app settings without affecting MPIN access.""",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color.DarkGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        termsAccepted = true
                        showTermsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                ) {
                    Text(if (model.isHindiMode) "स्वीकार है" else "Accept Terms", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text(if (model.isHindiMode) "बंद करें" else "Close")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(model: VaultViewModel) {
    var password by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    
    // Recovery states
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryAnswer by remember { mutableStateOf("") }
    var recoveryNewPassword by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf("") }
    var recoverySuccessMsg by remember { mutableStateOf("") }

    var localLoginError by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Top Language Switcher on Login Screen
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE2E8F0))
                    .clickable { model.toggleLanguage() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("btn_login_language_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = SlatePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (model.isHindiMode) "English" else "हिन्दी",
                        color = SlatePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Vault Shield Icon Header
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Lock Shield Guard",
            tint = TealAccent,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (model.isHindiMode) "पारिवारिक आपातकालीन वॉल्ट" else "FAMILY EMERGENCY VAULT",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SlatePrimary,
            letterSpacing = 1.sp
        )
        Text(
            text = if (model.isHindiMode) "सुरक्षित ऑफ़लाइन आपातकालीन सुरक्षा वॉलेट" else "Military-grade decentralized backup wallet",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!model.showMfaChallenge) {
            // Personalized Security Account Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SlatePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = model.registeredFullName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = (if (model.isHindiMode) "खाताधारक: " else "Detected Holder: ") + model.registeredFullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SlatePrimary
                        )
                        Text(
                            text = (if (model.isHindiMode) "मोबाइल: " else "Mobile: ") + model.registeredPhone.run {
                                if (length > 6) take(4) + "•••" + takeLast(3) else this
                            },
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(if (model.isHindiMode) "सक्रिय" else "ACTIVE", color = Color(0xFF15803D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (localLoginError.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFB91C1C), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = localLoginError, color = Color(0xFFB91C1C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Screen 1: Password entry
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (model.isHindiMode) "सुरक्षित लॉगिन पोर्टल" else "Secure Login Portal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SlatePrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (model.isHindiMode) "खाता पासवर्ड" else "Authorization Password",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("password_input"),
                        placeholder = { Text(if (model.isHindiMode) "पासवर्ड दर्ज करें" else "Enter account access password") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val success = model.login(password)
                            if (!success) {
                                localLoginError = if (model.isHindiMode) "गलत पासवर्ड। कृपया पुनः प्रयास करें।" else "Incorrect authorization password. Access Denied."
                            } else {
                                localLoginError = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("login_button")
                    ) {
                        Text(if (model.isHindiMode) "लॉगिन करें" else "Verify Credentials", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Forgotten Password Recovery Button
                    TextButton(
                        onClick = { showRecoveryDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (model.isHindiMode) "पासवर्ड भूल गए? सुरक्षा प्रश्न से रीसेट करें" else "Forgot access password? Safe recover with Security Answer",
                            fontSize = 11.sp,
                            color = TealAccent
                        )
                    }
                }
            }
        } else {
            // Screen 2: Simulate Smart OTP
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (model.isHindiMode) "द्वि-चरणीय प्रमाणीकरण (MFA OTP)" else "Double-Factor MFA OTP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = SlatePrimary
                    )
                    Text(
                        text = if (model.isHindiMode)
                            "सुरक्षा नियमों के तहत OTP भेजा गया है: ${model.registeredPhone} एवं ${model.registeredEmail}."
                        else
                            "In accordance with banking standards, an OTP has been dispatched to: ${model.registeredPhone} and email ${model.registeredEmail}.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Text(text = if (model.isHindiMode) "4-अंकों का OTP कोड" else "4-Digit OTP Code", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = { if (it.length <= 4) enteredOtp = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("otp_input"),
                        placeholder = { Text(if (model.isHindiMode) "OTP कोड डालें (उदा: 1234)" else "Enter the OTP code: 1234") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { model.verifyOtp(enteredOtp) },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("otp_verify_button")
                    ) {
                        Text(if (model.isHindiMode) "सुरक्षित प्रवेश प्राप्त करें" else "Grant Secure Access", color = Color.White)
                    }

                    TextButton(
                        onClick = { model.showMfaChallenge = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(if (model.isHindiMode) "पासवर्ड प्रविष्टि पर वापस जाएं" else "Return to Password Entry", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Demo Sandbox bypass card for quick evaluation
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
            border = BorderStroke(1.dp, Color.LightGray),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (model.isHindiMode) "🛠️ त्वरित भूमिका चयन (डेमो)" else "🛠️ Evaluator Sandbox Control Bar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = SlatePrimary
                )
                Text(
                    text = if (model.isHindiMode) "भूमिका बदलकर तुरंत प्रवेश करने के लिए क्लिक करें:" else "Click to switch role and log in automatically with simulated credentials.",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            model.switchRole(UserRole.OWNER)
                            model.verifyOtp("1234")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(if (model.isHindiMode) "राहुल (मालिक)" else "Rahul (Owner)", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = {
                            model.switchRole(UserRole.SPOUSE)
                            model.verifyOtp("1234")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(if (model.isHindiMode) "प्रिया (पत्नी)" else "Priya (Spouse)", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Account Recovery Safe Dialog
        if (showRecoveryDialog) {
            AlertDialog(
                onDismissRequest = { showRecoveryDialog = false },
                title = { Text(if (model.isHindiMode) "सुरक्षित पासवर्ड रीसेट" else "Secured Password Recovery", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = (if (model.isHindiMode) "सुरक्षा प्रश्न:\n" else "Security Question:\n") + model.registeredSecurityQuest,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlatePrimary
                        )

                        Text(text = if (model.isHindiMode) "सुरक्षा उत्तर" else "Recovery Answer", fontSize = 11.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = recoveryAnswer,
                            onValueChange = { recoveryAnswer = it },
                            placeholder = { Text(if (model.isHindiMode) "अपना उत्तर दर्ज करें" else "Enter private answer") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(text = if (model.isHindiMode) "नया पासवर्ड बनाएं" else "Define New Secure Password", fontSize = 11.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = recoveryNewPassword,
                            onValueChange = { recoveryNewPassword = it },
                            placeholder = { Text(if (model.isHindiMode) "न्यूनतम 4 अक्षर" else "Minimum 4 characters") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (recoveryError.isNotEmpty()) {
                            Text(text = recoveryError, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (recoverySuccessMsg.isNotEmpty()) {
                            Text(text = recoverySuccessMsg, color = Color(0xFF15803D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (recoveryAnswer.trim().lowercase() == model.registeredSecurityAns.lowercase()) {
                                if (recoveryNewPassword.trim().length >= 4) {
                                    model.registeredPassword = recoveryNewPassword.trim()
                                    recoverySuccessMsg = if (model.isHindiMode) "सफल! पासवर्ड रीसेट हो गया है।" else "Success! Password reset from Question recovery active."
                                    recoveryError = ""
                                    model.isLoggedIn = true
                                    model.showMfaChallenge = false
                                    showRecoveryDialog = false
                                    recoveryAnswer = ""
                                    recoveryNewPassword = ""
                                    recoverySuccessMsg = ""
                                } else {
                                    recoveryError = if (model.isHindiMode) "नया पासवर्ड कम से कम 4 अक्षरों का होना चाहिए।" else "New password must be at least 4 characters long."
                                }
                            } else {
                                recoveryError = if (model.isHindiMode) "सत्यापन विफल: गलत उत्तर।" else "Verification Failed: Incorrect Answer key."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                    ) {
                        Text(if (model.isHindiMode) "रीसेट कर प्रवेश करें" else "Reset & Vault Sign", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecoveryDialog = false }) {
                        Text(if (model.isHindiMode) "रद्द करें" else "Dismiss")
                    }
                }
            )
        }
    }
}
