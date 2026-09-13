package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceGpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val altitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String = "GPS",
    val isMock: Boolean = false
) {
    val mapsUrl: String
        get() = "https://maps.google.com/?q=$latitude,$longitude"

    val coordinatesFormatted: String
        get() = String.format(Locale.US, "%.5f° N/S, %.5f° E/W", latitude, longitude)

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

object EmergencyLocationManager {

    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(context: Context): DeviceGpsLocation? {
        if (!hasLocationPermission(context)) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || loc.accuracy < bestLocation.accuracy || loc.time > bestLocation.time) {
                            bestLocation = loc
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore provider error
            }
        }

        return bestLocation?.let {
            DeviceGpsLocation(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy,
                altitude = it.altitude,
                timestamp = it.time,
                provider = it.provider ?: "Hardware GPS"
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun requestFreshLocation(
        context: Context,
        onLocationReceived: (DeviceGpsLocation) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onError("Location permission not granted. Please allow location access.")
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onError("Location Manager hardware service not available.")
            return
        }

        // First check last known for immediate return
        val lastKnown = getLastKnownLocation(context)
        if (lastKnown != null) {
            onLocationReceived(lastKnown)
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                val gpsLoc = DeviceGpsLocation(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy,
                    altitude = loc.altitude,
                    timestamp = loc.time,
                    provider = loc.provider ?: "GPS Satellite"
                )
                onLocationReceived(gpsLoc)
                try {
                    locationManager.removeUpdates(this)
                } catch (_: Exception) {}
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
            } else {
                if (lastKnown == null) {
                    onError("GPS is disabled on device. Please enable Location in Android settings.")
                }
            }
        } catch (e: Exception) {
            if (lastKnown == null) {
                onError("Failed to acquire satellite fix: ${e.message}")
            }
        }
    }

    fun buildEmergencySosMessage(
        senderName: String,
        location: DeviceGpsLocation?,
        customNote: String = "",
        isHindi: Boolean = false
    ): String {
        val baseMsg = if (isHindi) {
            "🚨 आपातकालीन सहायता संदेश (EMERGENCY SOS) 🚨\n" +
            "मैं $senderName हूँ। मुझे तुरंत सहायता की आवश्यकता है!"
        } else {
            "🚨 EMERGENCY SOS ALERT 🚨\n" +
            "This is $senderName. I need urgent assistance!"
        }

        val locMsg = if (location != null) {
            "\n\n📍 वर्तमान स्थान (Live GPS Location):\n" +
            "Google Maps: ${location.mapsUrl}\n" +
            "Coordinates: ${String.format(Locale.US, "%.5f", location.latitude)}, ${String.format(Locale.US, "%.5f", location.longitude)}\n" +
            "Accuracy: ~${location.accuracy.toInt()}m | Time: ${location.formattedTime}"
        } else {
            "\n\n(Note: GPS coordinates pending satellite fix)"
        }

        val noteMsg = if (customNote.isNotBlank()) {
            "\n\nDetails: $customNote"
        } else {
            ""
        }

        val footer = "\n\nSent via Family Emergency Vault (Offline Continuity Hub)."
        return baseMsg + locMsg + noteMsg + footer
    }

    fun dialEmergencyNumber(context: Context, phoneNumber: String) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isBlank()) {
            Toast.makeText(context, "Emergency phone number is empty.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmergencySms(context: Context, phoneNumber: String, message: String) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        try {
            val uri = if (cleanNumber.isNotBlank()) Uri.parse("smsto:$cleanNumber") else Uri.parse("smsto:")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic ACTION_SEND
            try {
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(genericIntent, "Send Emergency SOS"))
            } catch (err: Exception) {
                Toast.makeText(context, "Could not open messaging app: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendEmergencyWhatsApp(context: Context, phoneNumber: String, message: String) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        try {
            val url = if (cleanNumber.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}"
            } else {
                "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to regular SMS
            sendEmergencySms(context, phoneNumber, message)
        }
    }

    fun openMapLocation(context: Context, latitude: Double, longitude: Double) {
        try {
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Emergency+Location)")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            // Browser fallback
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$latitude,$longitude")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to view map on device.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
