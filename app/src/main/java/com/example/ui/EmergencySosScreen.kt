package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ImportantContact
import com.example.data.VaultItem
import com.example.ui.theme.RedAlert
import com.example.ui.theme.SlatePrimary
import com.example.ui.theme.TealAccent
import com.example.util.DeviceGpsLocation
import com.example.util.EmergencyLocationManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencySosDialog(
    model: VaultViewModel,
    contacts: List<ImportantContact>,
    vaultItems: List<VaultItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(EmergencyLocationManager.hasLocationPermission(context))
    }
    var currentGpsLocation by remember {
        mutableStateOf(EmergencyLocationManager.getLastKnownLocation(context))
    }
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf("") }
    var customNote by remember { mutableStateOf("") }
    var showDossierDialog by remember { mutableStateOf(false) }

    // Runtime Permission Launcher for GPS Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasPermission = fineGranted || coarseGranted
        if (hasPermission) {
            isLocating = true
            EmergencyLocationManager.requestFreshLocation(
                context = context,
                onLocationReceived = { loc ->
                    currentGpsLocation = loc
                    isLocating = false
                    locationError = ""
                },
                onError = { err ->
                    locationError = err
                    isLocating = false
                }
            )
        } else {
            locationError = "Location permission denied. Cannot acquire offline satellite coordinates."
        }
    }

    // Auto-fetch location if permission is already granted
    LaunchedEffect(Unit) {
        if (hasPermission) {
            isLocating = true
            EmergencyLocationManager.requestFreshLocation(
                context = context,
                onLocationReceived = { loc ->
                    currentGpsLocation = loc
                    isLocating = false
                },
                onError = { err ->
                    locationError = err
                    isLocating = false
                }
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .testTag("emergency_sos_dialog"),
            color = Color(0xFF0F172A)
        ) {
            Scaffold(
                containerColor = Color(0xFF0F172A),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDC2626)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Emergency Alert",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (model.isHindiMode) "आपातकालीन सहायता व लाइव लोकेशन" else "Emergency SOS & Offline Location",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (model.isHindiMode) "1-टैप कॉल और ऑफ़लाइन SMS लोकेशन शेयर" else "1-Tap Call & Offline SMS Location Dispatch",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFCA5A5)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close SOS",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF1E293B)
                        ),
                        actions = {
                            // 1-Page Emergency Physical Dossier Export Button
                            IconButton(onClick = { showDossierDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Physical Dossier",
                                    tint = TealAccent
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. OFFLINE SATELLITE GPS CARD
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (currentGpsLocation != null) Color(0xFF22C55E).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("gps_location_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "GPS Location",
                                        tint = if (currentGpsLocation != null) Color(0xFF22C55E) else Color(0xFFF87171),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (model.isHindiMode) "ऑफ़लाइन GPS लोकेशन (सैटेलाइट)" else "Offline Satellite GPS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }

                                if (isLocating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = TealAccent
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            if (!hasPermission) {
                                                locationPermissionLauncher.launch(
                                                    arrayOf(
                                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            } else {
                                                isLocating = true
                                                EmergencyLocationManager.requestFreshLocation(
                                                    context = context,
                                                    onLocationReceived = { loc ->
                                                        currentGpsLocation = loc
                                                        isLocating = false
                                                        locationError = ""
                                                    },
                                                    onError = { err ->
                                                        locationError = err
                                                        isLocating = false
                                                    }
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh GPS",
                                            tint = TealAccent
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (!hasPermission) {
                                Text(
                                    text = if (model.isHindiMode)
                                        "आपातकाल में सटीक लोकेशन SMS द्वारा भेजने के लिए लोकेशन अनुमति आवश्यक है।"
                                    else
                                        "Location permission is required to acquire offline GPS coordinates for emergency SMS sharing.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFCA5A5)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_grant_location")
                                ) {
                                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (model.isHindiMode) "लोकेशन की अनुमति दें" else "Enable Hardware Location Access",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else if (currentGpsLocation != null) {
                                val loc = currentGpsLocation!!
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Coordinates: ${String.format(Locale.US, "%.5f", loc.latitude)}°, ${String.format(Locale.US, "%.5f", loc.longitude)}°",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Accuracy: ±${loc.accuracy.toInt()} meters • Provider: ${loc.provider}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = "Updated: ${loc.formattedTime}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Emergency Location", loc.mapsUrl)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Location URL copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF475569))
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy URL", fontSize = 11.sp, color = Color.White)
                                    }

                                    Button(
                                        onClick = {
                                            EmergencyLocationManager.openMapLocation(context, loc.latitude, loc.longitude)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("View Map", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            } else {
                                Text(
                                    text = if (locationError.isNotBlank()) locationError else "Waiting for GPS satellite signal...",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFCA5A5)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = TealAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "100% Offline: Direct satellite GPS lock without cloud tracking",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. QUICK SITUATIONAL NOTE (Optional message details)
                    OutlinedTextField(
                        value = customNote,
                        onValueChange = { customNote = it },
                        label = {
                            Text(
                                text = if (model.isHindiMode) "आपातकालीन स्थिति या आवश्यकता (वैकल्पिक)" else "Emergency Situation / Note (Optional)",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        placeholder = {
                            Text(
                                text = "e.g. Road accident, Medical pain, Need vehicle assistance...",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        },
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("sos_custom_note_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. IMMEDIATE EMERGENCY DISPATCH (PRIMARY NOMINEE / SPOUSE)
                    val primaryContact = contacts.firstOrNull {
                        it.priority.contains("Priority 1", ignoreCase = true) ||
                        it.category.contains("Spouse", ignoreCase = true) ||
                        it.category.contains("Parent", ignoreCase = true)
                    } ?: contacts.firstOrNull()

                    if (primaryContact != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "⚡ 1-TAP INSTANT SOS DISPATCH",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFFDC2626),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = primaryContact.category,
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Send live GPS coordinates & SOS distress SMS directly to ${primaryContact.contactName} (${primaryContact.phone}). Works even without internet.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFECACA),
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val msg = EmergencyLocationManager.buildEmergencySosMessage(
                                                senderName = model.registeredFullName.ifBlank { "Family Member" },
                                                location = currentGpsLocation,
                                                customNote = customNote,
                                                isHindi = model.isHindiMode
                                            )
                                            EmergencyLocationManager.sendEmergencySms(
                                                context = context,
                                                phoneNumber = primaryContact.phone,
                                                message = msg
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("btn_instant_sms_sos")
                                    ) {
                                        Icon(imageVector = Icons.Default.Sms, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SMS GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Button(
                                        onClick = {
                                            EmergencyLocationManager.dialEmergencyNumber(context, primaryContact.phone)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("btn_instant_call_sos")
                                    ) {
                                        Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Call Direct", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Button(
                                        onClick = {
                                            val msg = EmergencyLocationManager.buildEmergencySosMessage(
                                                senderName = model.registeredFullName.ifBlank { "Family Member" },
                                                location = currentGpsLocation,
                                                customNote = customNote,
                                                isHindi = model.isHindiMode
                                            )
                                            EmergencyLocationManager.sendEmergencyWhatsApp(
                                                context = context,
                                                phoneNumber = primaryContact.phone,
                                                message = msg
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("btn_instant_whatsapp_sos")
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. OFFICIAL NATIONAL EMERGENCY HELPLINES
                    Text(
                        text = if (model.isHindiMode) "राष्ट्रीय आपातकालीन हेल्पलाइन नंबर" else "National Emergency Services",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HelplinePill(
                            title = "112 SOS",
                            subtitle = "National Emergency",
                            color = Color(0xFFDC2626),
                            onClick = { EmergencyLocationManager.dialEmergencyNumber(context, "112") },
                            modifier = Modifier.weight(1f)
                        )
                        HelplinePill(
                            title = "108 / 102",
                            subtitle = "Ambulance",
                            color = Color(0xFFEA580C),
                            onClick = { EmergencyLocationManager.dialEmergencyNumber(context, "108") },
                            modifier = Modifier.weight(1f)
                        )
                        HelplinePill(
                            title = "100 Police",
                            subtitle = "Local Police",
                            color = Color(0xFF2563EB),
                            onClick = { EmergencyLocationManager.dialEmergencyNumber(context, "100") },
                            modifier = Modifier.weight(1f)
                        )
                        HelplinePill(
                            title = "101 Fire",
                            subtitle = "Fire Brigade",
                            color = Color(0xFFD97706),
                            onClick = { EmergencyLocationManager.dialEmergencyNumber(context, "101") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 5. REGISTERED EMERGENCY CONTACTS (ROOM DATABASE)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (model.isHindiMode) "आपके सुरक्षित आपातकालीन संपर्क" else "Your Emergency Contacts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${contacts.size} Registered",
                            fontSize = 11.sp,
                            color = TealAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (contacts.isEmpty()) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No emergency contacts registered yet. Add contacts in the 'Contacts' tab.",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        contacts.forEach { contact ->
                            EmergencyContactCard(
                                contact = contact,
                                onCall = { EmergencyLocationManager.dialEmergencyNumber(context, contact.phone) },
                                onSms = {
                                    val msg = EmergencyLocationManager.buildEmergencySosMessage(
                                        senderName = model.registeredFullName.ifBlank { "Family Member" },
                                        location = currentGpsLocation,
                                        customNote = customNote,
                                        isHindi = model.isHindiMode
                                    )
                                    EmergencyLocationManager.sendEmergencySms(context, contact.phone, msg)
                                },
                                onWhatsApp = {
                                    val msg = EmergencyLocationManager.buildEmergencySosMessage(
                                        senderName = model.registeredFullName.ifBlank { "Family Member" },
                                        location = currentGpsLocation,
                                        customNote = customNote,
                                        isHindi = model.isHindiMode
                                    )
                                    EmergencyLocationManager.sendEmergencyWhatsApp(context, contact.phone, msg)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showDossierDialog) {
        EmergencyPhysicalDossierDialog(
            model = model,
            contacts = contacts,
            vaultItems = vaultItems,
            currentLocation = currentGpsLocation,
            onDismiss = { showDossierDialog = false }
        )
    }
}

@Composable
fun HelplinePill(
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(text = subtitle, fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun EmergencyContactCard(
    contact: ImportantContact,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.contactName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = TealAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = contact.category,
                            fontSize = 9.sp,
                            color = TealAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contact.phone,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Call Button
                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16A34A))
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                // SMS with GPS Location Button
                IconButton(
                    onClick = onSms,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7))
                ) {
                    Icon(imageVector = Icons.Default.Sms, contentDescription = "SMS Location", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                // WhatsApp Button
                IconButton(
                    onClick = onWhatsApp,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ==================== 1-PAGE PHYSICAL EMERGENCY DOSSIER DIALOG ====================

@Composable
fun EmergencyPhysicalDossierDialog(
    model: VaultViewModel,
    contacts: List<ImportantContact>,
    vaultItems: List<VaultItem>,
    currentLocation: DeviceGpsLocation?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val primaryDoc = contacts.find { it.category.contains("Doctor", ignoreCase = true) }
    val primaryLawyer = contacts.find { it.category.contains("Lawyer", ignoreCase = true) }
    val primaryHealth = vaultItems.find { it.category.equals("INSURANCE", ignoreCase = true) && it.title.contains("Health", ignoreCase = true) }
        ?: vaultItems.find { it.category.equals("INSURANCE", ignoreCase = true) }
    val primaryBank = vaultItems.find { it.category.equals("BANK", ignoreCase = true) }
    val primaryLocker = vaultItems.find { it.category.equals("LOCKER", ignoreCase = true) }

    val dossierContent = buildString {
        appendLine("==================================================")
        appendLine("           FAMILY EMERGENCY DOSSIER (OFFLINE)      ")
        appendLine("==================================================")
        appendLine("Generated: ${java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())}")
        appendLine("Account Owner: ${model.registeredFullName.ifBlank { "Registered Family Head" }}")
        appendLine("Primary Emergency Nominee: ${model.registeredNomineeName.ifBlank { "Not Specified" }} (${model.registeredPhone})")
        appendLine("Primary Nominee Relation: ${model.registeredNomineeRelation.ifBlank { "Immediate Family" }}")
        appendLine("--------------------------------------------------")
        appendLine("CRITICAL EMERGENCY CONTACTS:")
        contacts.take(4).forEach { con ->
            appendLine("• ${con.contactName} (${con.category}): ${con.phone}")
        }
        if (primaryDoc != null) {
            appendLine("• Family Doctor: ${primaryDoc.contactName} (${primaryDoc.phone})")
        }
        appendLine("--------------------------------------------------")
        appendLine("KEY ASSETS & CONTINUITY LOCATIONS:")
        if (primaryHealth != null) {
            appendLine("• Health Insurance: ${primaryHealth.institution} | Policy #${primaryHealth.numberOrId}")
            if (primaryHealth.physicalLocation.isNotBlank()) appendLine("  Physical Policy Kit: ${primaryHealth.physicalLocation}")
        }
        if (primaryBank != null) {
            appendLine("• Primary Bank: ${primaryBank.institution} | A/C #${primaryBank.numberOrId}")
            if (primaryBank.physicalLocation.isNotBlank()) appendLine("  Chequebook/Passbook: ${primaryBank.physicalLocation}")
        }
        if (primaryLocker != null) {
            appendLine("• Safe Locker: ${primaryLocker.institution} | Locker #${primaryLocker.numberOrId}")
            appendLine("  Key Location: ${primaryLocker.physicalLocation}")
        }
        appendLine("--------------------------------------------------")
        if (currentLocation != null) {
            appendLine("GPS LOCATION AT EXPORT:")
            appendLine("• Coordinates: ${currentLocation.latitude}, ${currentLocation.longitude}")
            appendLine("• Map Link: ${currentLocation.mapsUrl}")
            appendLine("--------------------------------------------------")
        }
        appendLine("DISCLAIMER: Keep this physical printed record inside your home safe")
        appendLine("or confidential bank locker. Protected by Family Emergency Vault.")
        appendLine("==================================================")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1329))
                .padding(16.dp),
            color = Color(0xFF0B1329),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = TealAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1-Page Emergency Physical Dossier",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "A consolidated offline physical reference card for home lockers, hospital triage, or banking claims.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = dossierContent,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF0F172A),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Emergency Dossier", dossierContent)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Dossier text copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Text", fontSize = 12.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Confidential: Family Emergency Physical Dossier")
                                putExtra(Intent.EXTRA_TEXT, dossierContent)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Emergency Dossier / Print"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = SlatePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share / Print", fontSize = 12.sp, color = SlatePrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
