package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTermsConsentScreen(model: VaultViewModel) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var termsScrolledToEnd by remember { mutableStateOf(true) }
    var userScrolledStatusMsg by remember { mutableStateOf("🔒 Click the acceptance indicator below to proceed.") }
    var isChecked by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val outerScrollState = rememberScrollState()

    // Detect when terms screen has been scrolled
    LaunchedEffect(scrollState.value) {
        if (scrollState.value > 0) {
            userScrolledStatusMsg = "🔒 Scroll verified: Legally read receipt confirmed."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlatePrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(outerScrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Legal Shield Header Icon
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(TealAccent.copy(alpha = 0.15f))
                .border(2.dp, TealAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Gavel,
                contentDescription = "Gavel Court Legal icon",
                tint = TealAccent,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "DECRYPTION & COMPLIANCE AGREEMENT",
            fontSize = 11.sp,
            color = TealAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Text(
            text = "Emergency Vault Terms of Service",
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        // Indication of sandboxing
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Shield Guard",
                    tint = TealAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "End-to-End Sandbox Encryption: Your keys and contact coordinates never touch our remote clouds.",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Active Legal Content
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Text(
                    text = "READ AND SCROLL TO ACKNOWLEDGE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlatePrimary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Scrollable legal text segment
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(SlateLightBg, RoundedCornerShape(8.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .padding(end = 4.dp)
                    ) {
                        Text(
                            text = "Welcome to the Family Emergency Vault software suite. Before activation, review our strict terms of cryptographic use, sandboxing conditions, and administrative liabilities:\n\n" +
                                    "1. PHYSICAL DECOUPLING AND DECRYPTION\n" +
                                    "This system operates on client-side sandboxed local preferences. All data marked with the '🔐 Secure Local' protection standard is ciphered via AES-GCM (equivalent to the Web Cryptography SubtleCrypto API specification). Without your Master MPIN code, the device local decryption sandbox is mathematically impenetrable.\n\n" +
                                    "2. THE RECOVERY PARADOX (NO CLOUD PASSWORD RESETS)\n" +
                                    "Because this system operates fully decentralized, there is no physical, centralized administrative server, database, or technical operator capable of recovering your master password or MPIN code on your behalf. If you lose your security authentication parameters, your family coordinates and vault assets will remain corrupted forever. You must back up your keys offline physically.\n\n" +
                                    "3. CHOSEN ROLES & LIABILITY DECLARATION\n" +
                                    "By appointing heirs, executors, or external trusted advisors, you explicitly authorize those roles to trigger access requests. Approved requestors can read unmasked assets subject to system delays (e.g. 48-hour dead-man verification timer). It is your responsibility as the owner to configure appropriate timers to prevent unauthorized release.\n\n" +
                                    "4. EXPERIMENTAL CHAT CONTINUITY ASSISTANT DISCLOSURE\n" +
                                    "The integrated AI Continuity Assistant uses Gemini. It is a simulation for emergency planning guidelines. Never submit, type, or feed raw physical security coordinates, bank PINs, passwords, or transaction PINs directly into the chat assistant input field.\n\n" +
                                    "5. GEOGRAPHIC SANDBOX COMPLIANCE\n" +
                                    "This product conforms strictly with regional guidelines on local data insulation, utilizing hard keys generated dynamically at setup. We exclude physical trackers. No diagnostic analytics or performance packages are broadcast.\n\n" +
                                    "By ticking the compliance checkbox below and registering your legal credentials, you consent to these parameters and trigger the sandboxed keys creation.",
                            fontSize = 11.sp,
                            color = SlatePrimary,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Justify
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "--- End of Compliance Terms & Specifications Doc ---",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Scroll Confirmed banner
                Text(
                    text = userScrolledStatusMsg,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (termsScrolledToEnd) TealAccent else Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Verification details input Form
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "STEP 2: REGISTER DECRYPTION CERTIFICATE",
                    fontSize = 10.sp,
                    color = TealAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Legal Full Name", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = TealAccent,
                            unfocusedLabelColor = Color.LightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Vault Owner Email", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = TealAccent,
                            unfocusedLabelColor = Color.LightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it },
                        colors = CheckboxDefaults.colors(checkedColor = TealAccent)
                    )
                    Text(
                        text = "I accept and acknowledge the decryption guidelines, liability clauses, and local offline custody regulations.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (validationError.isNotEmpty()) {
                    Text(
                        text = validationError,
                        color = RedAlert,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (name.trim().isEmpty() || email.trim().isEmpty()) {
                            validationError = "Both Name and Email are required for registration stamp."
                        } else if (!email.contains("@")) {
                            validationError = "Please enter a valid owner email address."
                        } else if (!isChecked) {
                            validationError = "You must select the acknowledgment check indicator."
                        } else {
                            validationError = ""
                            model.acceptTerms(name.trim(), email.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalActivity,
                        contentDescription = "Consent button Icon",
                        tint = SlatePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accept Terms & Decrypt Secure Sandbox", color = SlatePrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
