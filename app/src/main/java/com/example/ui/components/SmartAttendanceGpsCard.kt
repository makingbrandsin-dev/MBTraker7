package com.example.ui.components

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*
import com.example.util.GeofenceResult
import com.example.util.LocationHelper
import com.example.util.MiloHaptics
import java.text.SimpleDateFormat
import java.util.*

/**
 * 📍 Smart Attendance GPS & Real-time Location Card:
 * Replaces any static status text with an active, functional GPS capture engine
 * that queries high-precision device satellite coordinates, validates office geofence perimeter,
 * and streams the timestamped geotag log directly to Firebase Firestore.
 */
@Composable
fun SmartAttendanceGpsCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAttendance: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val latestAttendance by viewModel.latestAttendance.collectAsState()
    val isWorking = latestAttendance?.isWorking == true
    val isConnected by viewModel.isFirebaseConnected.collectAsState()

    var isCapturingGps by remember { mutableStateOf(false) }
    var lastCapturedResult by remember { mutableStateOf<GeofenceResult?>(null) }
    var capturedTimestampText by remember { mutableStateOf<String?>(null) }
    var showGpsDetailsDialog by remember { mutableStateOf(false) }
    var confirmationModalMessage by remember { mutableStateOf<String?>(null) }

    // Runtime Permission Launcher for GPS Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            triggerGpsCapture(
                context = context,
                viewModel = viewModel,
                onStart = { isCapturingGps = true },
                onComplete = { success, message, result, timeStr ->
                    isCapturingGps = false
                    lastCapturedResult = result
                    capturedTimestampText = timeStr
                    if (success) {
                        MiloHaptics.performButtonClick(context, haptic)
                        confirmationModalMessage = message
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else {
            isCapturingGps = false
            Toast.makeText(context, "Location permission is required to capture GPS attendance", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_attendance_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 🏷️ Top Header: Smart Attendance Title + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0D9488), Color(0xFF10B981))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Smart Attendance GPS",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Smart Attendance",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFCCFBF1)
                            ) {
                                Text(
                                    text = "GPS LIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F766E),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isConnected) "Firebase Realtime Synced" else "Local Persistent Cache",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isConnected) Color(0xFF059669) else Color(0xFFD97706)
                            )
                        }
                    }
                }

                // Shift Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isWorking) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isWorking) Color(0xFF86EFAC) else Color(0xFFE2E8F0)),
                    modifier = Modifier.testTag("attendance_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isWorking) Color(0xFF10B981) else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isWorking) "Shift Active" else "Off-Clock",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWorking) Color(0xFF065F46) else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 🗺️ Telemetry Summary Card (Location, Address, Coordinates)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGpsDetailsDialog = true }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Current GPS Location",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF334155)
                            )
                        }

                        Text(
                            text = if (lastCapturedResult != null) "Updated Just Now" else "Tap for Map Info",
                            fontSize = 10.sp,
                            color = BrandBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val displayAddress = lastCapturedResult?.locationName
                        ?: latestAttendance?.locationAddress
                        ?: LocationHelper.OFFICE_NAME

                    val displayLat = lastCapturedResult?.latitude ?: latestAttendance?.latitude ?: LocationHelper.OFFICE_LAT
                    val displayLng = lastCapturedResult?.longitude ?: latestAttendance?.longitude ?: LocationHelper.OFFICE_LNG

                    Text(
                        text = displayAddress,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coords: ${String.format(Locale.US, "%.4f° N, %.4f° E", displayLat, displayLng)}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.testTag("gps_coordinates_display")
                        )

                        Text(
                            text = if (latestAttendance?.isGeofenceVerified == true || lastCapturedResult?.isInsideGeofence == true) "✅ Inside Geofence" else "📍 Remote Geotag",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (latestAttendance?.isGeofenceVerified == true || lastCapturedResult?.isInsideGeofence == true) Color(0xFF059669) else Color(0xFFD97706),
                            modifier = Modifier.testTag("geofence_status_text")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 🚀 FUNCTIONAL ACTION BUTTON: Replaces static 'Check Status' link with GPS Location & Firebase Logger
            Button(
                onClick = {
                    MiloHaptics.performButtonClick(context, haptic)
                    if (LocationHelper.hasLocationPermission(context)) {
                        triggerGpsCapture(
                            context = context,
                            viewModel = viewModel,
                            onStart = { isCapturingGps = true },
                            onComplete = { success, message, result, timeStr ->
                                isCapturingGps = false
                                lastCapturedResult = result
                                capturedTimestampText = timeStr
                                if (success) {
                                    MiloHaptics.performButtonClick(context, haptic)
                                    confirmationModalMessage = message
                                } else {
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                enabled = !isCapturingGps,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("gps_location_capture_button")
            ) {
                if (isCapturingGps) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Acquiring GPS & Syncing Firebase...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Capture GPS",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Capture GPS & Log to Firebase",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Quick Jump
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (latestAttendance?.checkInTime != null) "Punched In at ${latestAttendance?.checkInTime}" else "Not punched in today",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                TextButton(
                    onClick = onNavigateToAttendance,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Attendance Hub", fontSize = 11.5.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(14.dp))
                }
            }
        }
    }

    // 📋 Success Confirmation Dialog
    confirmationModalMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { confirmationModalMessage = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text("GPS Geotag Recorded", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(msg, fontSize = 12.5.sp, color = Color(0xFF334155), lineHeight = 18.sp)
            },
            confirmButton = {
                Button(
                    onClick = { confirmationModalMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 🗺️ GPS Geofence Inspection Dialog
    if (showGpsDetailsDialog) {
        GpsGeofenceInfoDialog(
            result = lastCapturedResult ?: LocationHelper.verifyOfficeGeofence(context),
            timestamp = capturedTimestampText ?: SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date()),
            onDismiss = { showGpsDetailsDialog = false }
        )
    }
}

private fun triggerGpsCapture(
    context: Context,
    viewModel: MainViewModel,
    onStart: () -> Unit,
    onComplete: (Boolean, String, GeofenceResult, String) -> Unit
) {
    onStart()
    val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    val timeStr = timeFormat.format(Date())

    viewModel.captureGpsLocationAndLogToFirebase(context) { success, message, result ->
        onComplete(success, message, result, timeStr)
    }
}

@Composable
private fun GpsGeofenceInfoDialog(
    result: GeofenceResult,
    timestamp: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SatelliteAlt, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "GPS & Geofence Telemetry",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = "High-precision satellite coordinates verified for Making Brands HQ",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Latitude", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(String.format(Locale.US, "%.6f° N", result.latitude), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Divider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE2E8F0))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Longitude", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(String.format(Locale.US, "%.6f° E", result.longitude), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Divider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE2E8F0))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Distance to HQ", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text("${result.distanceMeters.toInt()} meters", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                        }
                        Divider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE2E8F0))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Geofence Verification", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(if (result.isInsideGeofence) "HQ Verified (<=250m)" else "Remote Geotag", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (result.isInsideGeofence) Color(0xFF16A34A) else Color(0xFFD97706))
                        }
                        Divider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE2E8F0))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Log Timestamp", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(timestamp, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
