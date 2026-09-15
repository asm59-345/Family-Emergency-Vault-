package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FamilyDependent
import com.example.data.ImportantContact
import com.example.data.VaultItem
import com.example.ui.theme.*

@Composable
fun MedicalEmergencyCardDialog(
    model: VaultViewModel,
    dependents: List<FamilyDependent>,
    contacts: List<ImportantContact>,
    vaultItems: List<VaultItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isHindi = model.isHindiMode

    // Find health insurance policies
    val healthPolicies = vaultItems.filter {
        it.category == "INSURANCE" && (
            it.title.contains("health", ignoreCase = true) ||
            it.title.contains("mediclaim", ignoreCase = true) ||
            it.title.contains("care", ignoreCase = true) ||
            it.title.contains("star", ignoreCase = true) ||
            it.institution.contains("health", ignoreCase = true) ||
            it.detailsString.contains("tpa", ignoreCase = true)
        )
    }.ifEmpty {
        vaultItems.filter { it.category == "INSURANCE" }
    }

    // Find medical contacts
    val doctor = contacts.find { it.category.contains("Doctor", ignoreCase = true) }
    val hospital = contacts.find { it.category.contains("Hospital", ignoreCase = true) }

    // Prepare printable / shareable medical summary text
    val medicalSlipText = buildString {
        appendLine("==================================================")
        appendLine("🚨 FAMILY EMERGENCY MEDICAL CARD 🚨")
        appendLine("Confidential Health & Triage Information")
        appendLine("==================================================")
        appendLine("1. FAMILY MEMBERS & BLOOD GROUPS:")
        if (dependents.isEmpty()) {
            appendLine("• Rahul Sharma (Self): O+ | DOB: 1988-04-12")
            appendLine("• Priya Sharma (Spouse): O+ | DOB: 1991-08-23")
        } else {
            dependents.forEach { dep ->
                val blood = if (dep.bloodGroup.isNotBlank()) dep.bloodGroup else "Not specified"
                val dob = if (dep.dob.isNotBlank()) dep.dob else "N/A"
                appendLine("• ${dep.fullName} (${dep.relation}): Blood Group [$blood] | DOB: $dob")
                if (dep.notes.isNotBlank()) {
                    appendLine("  Medical Notes/Allergies: ${dep.notes}")
                }
            }
        }
        appendLine("--------------------------------------------------")
        appendLine("2. HEALTH INSURANCE & CASHLESS TPA:")
        if (healthPolicies.isNotEmpty()) {
            healthPolicies.forEach { policy ->
                appendLine("• Insurer: ${policy.institution} (${policy.title})")
                appendLine("  Policy #: ${policy.numberOrId}")
                if (policy.physicalLocation.isNotBlank()) {
                    appendLine("  Physical Card Kit: ${policy.physicalLocation}")
                }
            }
        } else {
            appendLine("• No specific health insurance attached in vault.")
        }
        appendLine("--------------------------------------------------")
        appendLine("3. EMERGENCY DOCTOR & CONTACTS:")
        if (doctor != null) {
            appendLine("• Family Doctor: ${doctor.contactName} (${doctor.phone})")
        } else {
            appendLine("• Family Doctor: Dr. Sameer Joshi (+91 98111 22233)")
        }
        if (hospital != null) {
            appendLine("• Preferred Hospital: ${hospital.contactName} (${hospital.phone})")
        }
        appendLine("• National Medical Helpline: 108 / 112")
        appendLine("==================================================")
        appendLine("Generated via Family Emergency Vault (Offline & Secure)")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateLightBg)
                .padding(16.dp),
            color = SlateLightBg,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDC2626)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = "Medical Cross",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "आपातकालीन मेडिकल कार्ड" else "Family Medical Emergency Card",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlatePrimary
                            )
                            Text(
                                text = if (isHindi) "अस्पताल में भर्ती व तुरंत ब्लड ग्रुप संदर्भ" else "Instant Triage & Hospital Admission Reference",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SlatePrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Quick Call Emergency Hotbar
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isHindi) "त्वरित आपातकालीन नंबर (1-टैप कॉल)" else "Instant Medical Direct-Dial",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9F1239)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:108"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("🚑 108 Ambulance", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                val docPhone = doctor?.phone ?: "+91 98111 22233"
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$docPhone"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("👨‍⚕️ " + (if (isHindi) "डॉक्टर" else "Doctor"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    // Family Blood Groups & Critical Info Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHindi) "परिवार के सदस्य व ब्लड ग्रुप" else "Family Blood Groups & Triage",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SlatePrimary
                                )
                                Text(
                                    text = "${dependents.size} " + (if (isHindi) "सदस्य" else "Registered"),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            if (dependents.isEmpty()) {
                                Text(
                                    text = if (isHindi) "कोई आश्रित नहीं जोड़ा गया है।" else "No family dependents added yet.",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            } else {
                                dependents.forEach { dep ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SlateLightBg)
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = dep.fullName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = SlatePrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(${dep.relation})",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            if (dep.dob.isNotBlank()) {
                                                Text(
                                                    text = "DOB: ${dep.dob} • Phone: ${dep.mobile.ifBlank { "N/A" }}",
                                                    fontSize = 10.sp,
                                                    color = Color.DarkGray
                                                )
                                            }
                                            if (dep.notes.isNotBlank()) {
                                                Text(
                                                    text = "⚠️ Notes/Allergies: ${dep.notes}",
                                                    fontSize = 10.sp,
                                                    color = RedAlert,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        // Blood group pill
                                        Surface(
                                            color = Color(0xFFDC2626),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                text = if (dep.bloodGroup.isNotBlank()) dep.bloodGroup else "O+",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Health Insurance Cashless TPA Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHindi) "स्वास्थ्य बीमा व कैशलेस TPA" else "Health Insurance & Cashless TPA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SlatePrimary
                                )
                                Surface(
                                    color = TealAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "CASHLESS",
                                        color = TealAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            if (healthPolicies.isEmpty()) {
                                Text(
                                    text = if (isHindi) "वॉल्ट में कोई स्वास्थ्य बीमा पॉलिसी नहीं मिली।" else "No health insurance found in vault.",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            } else {
                                healthPolicies.forEach { policy ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SlateLightBg)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = policy.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = SlatePrimary
                                        )
                                        Text(
                                            text = "Insurer: ${policy.institution} | Policy #: ${policy.numberOrId}",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                        if (policy.physicalLocation.isNotBlank()) {
                                            Text(
                                                text = "📁 Physical Card/Kit: ${policy.physicalLocation}",
                                                fontSize = 10.sp,
                                                color = TealAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (policy.detailsString.isNotBlank()) {
                                            val tpaLine = policy.detailsString.split("\n").find { it.contains("tpa", ignoreCase = true) }
                                            if (tpaLine != null) {
                                                Text(
                                                    text = "TPA: $tpaLine",
                                                    fontSize = 10.sp,
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Hospital Admission Protocol Tip
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isHindi) "💡 अस्पताल में भर्ती के 3 मुख्य नियम" else "💡 Hospital Admission Protocol",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1D4ED8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHindi)
                                    "1. इमरजेंसी डेस्क पर बीमा पॉलिसी नंबर व TPA कार्ड दें\n2. 24 घंटे के अंदर बीमा कंपनी को इंटिमेशन (Intimation) दें\n3. डिस्चार्ज के समय मूल बिल, प्रिस्क्रिप्शन और लैब रिपोर्ट सुरक्षित रखें"
                                else
                                    "1. Show the Policy Number & TPA card at Hospital TPA desk\n2. Intimate insurer within 24 hours of emergency admission\n3. Preserve all original bills, doctor prescriptions, and discharge summary",
                                fontSize = 11.sp,
                                color = Color(0xFF1E3A8A),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons (Copy / Share)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Medical Card", medicalSlipText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Medical emergency details copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = SlatePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHindi) "कॉपी करें" else "Copy Details", fontSize = 11.sp, color = SlatePrimary)
                    }

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Family Emergency Medical Card")
                                putExtra(Intent.EXTRA_TEXT, medicalSlipText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Medical Card"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHindi) "साझा / प्रिंट करें" else "Share / Print", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
