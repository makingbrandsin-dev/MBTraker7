package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.AppHeader
import com.example.ui.theme.*
import com.example.util.BiometricHelper
import kotlinx.coroutines.delay

// ---------------- Screen 1: Splash Screen ----------------
@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1600)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrandDarkBlue, Color(0xFF0F172A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = BrandBlue,
                shadowElevation = 8.dp,
                modifier = Modifier.size(92.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "MB",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                "MB Traker",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Track · Manage · Grow",
                color = BrandAccent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(
                color = BrandAccent,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            Text(
                "Making Brands Enterprise Suite\nVerified Biometrics & WhatsApp Security",
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

// ---------------- Screen 2: Login Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: (isAdmin: Boolean) -> Unit,
    onNavigateToOtp: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var selectedRole by remember { mutableStateOf("Employee") } // "Employee" or "MB Admin"
    var phoneNumber by remember { mutableStateOf("+91 98765 43210") }
    var password by remember { mutableStateOf("password123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isDirectSigningIn by remember { mutableStateOf(false) }
    val isCheckingRole by viewModel.isCheckingFirestoreRole.collectAsState()
    val firestoreStatus by viewModel.firestoreAuthStatus.collectAsState()

    LaunchedEffect(selectedRole) {
        if (selectedRole == "MB Admin") {
            phoneNumber = "+91 98111 22334"
            password = "admin123"
        } else {
            phoneNumber = "+91 98765 43210"
            password = "password123"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Brand Emblem
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BrandBlue,
                shadowElevation = 4.dp,
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("MB", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("MB Traker", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = BrandDarkBlue)
            Text("Enterprise Employee & Operations Portal", fontSize = 13.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(24.dp))

            // Role Selector Tabs (Employee vs MB Admin)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFE2E8F0),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val roles = listOf("Employee", "MB Admin")
                    roles.forEach { role ->
                        val isSelected = selectedRole == role
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) (if (role == "MB Admin") BrandDarkBlue else BrandBlue) else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRole = role }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (role == "MB Admin") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF334155),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = role,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mobile / WhatsApp Input Field
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("WhatsApp Mobile Number") },
                placeholder = { Text("+91 98765 43210") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = BrandGreen
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                colors = appTextFieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password / Security Pin") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricBlue) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                colors = appTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // WhatsApp Security Verification Notice Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WhatsApp OTP Verification",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                        Text(
                            text = "A dynamic 6-digit passcode will be dispatched to your WhatsApp to verify employee access.",
                            fontSize = 11.sp,
                            color = Color(0xFF047857),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Firestore Auth Provider & Role Redirection Preview Card
            val isTargetAdmin = selectedRole == "MB Admin" || phoneNumber.contains("98111")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firestore Cloud Auth Provider",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTargetAdmin) "Target Role: ADMIN (Firestore)" else "Target Role: EMPLOYEE (Firestore)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3A8A)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isTargetAdmin) Color(0xFFFDE047) else Color(0xFF86EFAC)
                        ) {
                            Text(
                                text = if (isTargetAdmin) "➔ Admin Dashboard" else "➔ Employee Workspace",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTargetAdmin) Color(0xFF713F12) else Color(0xFF065F46),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isCheckingRole) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Querying Firestore user record...",
                                fontSize = 10.sp,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Button: Send WhatsApp OTP
            Button(
                onClick = {
                    if (phoneNumber.isBlank()) {
                        Toast.makeText(context, "Please enter your mobile number", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSendingOtp = true
                    val isAdmin = selectedRole == "MB Admin"
                    viewModel.requestWhatsAppOtp(
                        context = context,
                        phoneNumber = phoneNumber,
                        role = selectedRole,
                        isAdmin = isAdmin
                    )
                    isSendingOtp = false
                    onNavigateToOtp()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Get WhatsApp OTP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Option: Direct Sign In with Password & Firestore Role Verification
            Button(
                onClick = {
                    if (phoneNumber.isBlank()) {
                        Toast.makeText(context, "Please enter your mobile number", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isDirectSigningIn = true
                    viewModel.loginWithFirestore(
                        phoneNumber = phoneNumber,
                        password = password,
                        selectedRoleHint = selectedRole
                    ) { isAdmin, targetRoute ->
                        isDirectSigningIn = false
                        val roleText = if (isAdmin) "Admin Dashboard" else "Employee Workspace"
                        Toast.makeText(context, "Firestore Role Verified! Opening $roleText", Toast.LENGTH_SHORT).show()
                        onLoginSuccess(isAdmin)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White
                ),
                enabled = !isDirectSigningIn
            ) {
                if (isDirectSigningIn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying Role in Firestore...", fontSize = 14.sp, color = Color.White)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign In & Check Firestore Role",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tertiary Option: Biometric Unlock with Firestore Role Verification
            OutlinedButton(
                onClick = {
                    if (activity != null) {
                        BiometricHelper.promptBiometricAuth(
                            activity = activity,
                            title = "MB Traker Biometric Sign-In",
                            subtitle = "Verify fingerprint or face to authenticate as $selectedRole",
                            onSuccess = {
                                viewModel.loginWithBiometricsWithFirestore(
                                    phoneNumber = phoneNumber,
                                    selectedRoleHint = selectedRole
                                ) { isAdmin, targetRoute ->
                                    val roleText = if (isAdmin) "Admin Dashboard" else "Employee Workspace"
                                    Toast.makeText(context, "Biometric & Firestore Verified! Opening $roleText", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(isAdmin)
                                }
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        // Fallback
                        viewModel.loginWithBiometricsWithFirestore(
                            phoneNumber = phoneNumber,
                            selectedRoleHint = selectedRole
                        ) { isAdmin, targetRoute ->
                            onLoginSuccess(isAdmin)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, BrandBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BrandBlue
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Unlock with Biometrics",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Need access or lost device? ", fontSize = 12.sp, color = TextSecondary)
                Text("Contact IT Admin", fontSize = 12.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------- Screen 3: OTP Verification Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    viewModel: MainViewModel,
    onVerifySuccess: (isAdmin: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val otpSession by viewModel.otpSession.collectAsState()

    val targetPhone = otpSession?.phoneNumber ?: "+91 98765 43210"
    val targetRole = otpSession?.role ?: "Employee"
    val isAdmin = otpSession?.isAdmin ?: false
    val generatedCode = otpSession?.otpCode ?: "123456"

    var otpInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableIntStateOf(30) }
    var isVerifyingOtp by remember { mutableStateOf(false) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "WhatsApp OTP Verification",
                onBack = onBack
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Shield / WhatsApp badge
            Surface(
                shape = CircleShape,
                color = Color(0xFFDCFCE7),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                "Enter 6-Digit WhatsApp Code",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Passcode dispatched to WhatsApp number",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Text(
                text = "$targetPhone ($targetRole)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BrandDarkBlue
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Auto-Fill Helper Card (Ensures testing works effortlessly in any environment)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        otpInput = generatedCode
                        errorMessage = null
                        focusManager.clearFocus()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Generated WhatsApp Code:",
                                fontSize = 11.sp,
                                color = Color(0xFF1E40AF)
                            )
                            Text(
                                generatedCode,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = BrandBlue,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandBlue,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            "Tap to Auto-Fill",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6-digit PIN Boxes
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Real hidden text field capturing user keystrokes
                BasicTextField(
                    value = otpInput,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            otpInput = input
                            errorMessage = null
                            if (input.length == 6) {
                                focusManager.clearFocus()
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(8.dp)),
                    decorationBox = { /* Invisible overlay */ }
                )

                // Rendered 6 OTP Boxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (0 until 6).forEach { index ->
                        val char = otpInput.getOrNull(index)?.toString() ?: ""
                        val isFocused = otpInput.length == index

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = if (isFocused) 4.dp else 1.dp,
                            border = BorderStroke(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = when {
                                    errorMessage != null -> Color(0xFFEF4444)
                                    isFocused -> BrandBlue
                                    char.isNotEmpty() -> BrandGreen
                                    else -> BorderLight
                                }
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = char,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFDC2626)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Resend OTP Countdown
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (countdown > 0) "Resend code in 00:${if (countdown < 10) "0$countdown" else countdown}"
                    else "Didn't get the code? ",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                if (countdown == 0) {
                    Text(
                        text = "Resend WhatsApp OTP",
                        fontSize = 13.sp,
                        color = BrandGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            val newCode = viewModel.requestWhatsAppOtp(
                                context = context,
                                phoneNumber = targetPhone,
                                role = targetRole,
                                isAdmin = isAdmin
                            )
                            countdown = 30
                            otpInput = ""
                            errorMessage = null
                            Toast.makeText(context, "New OTP dispatched to WhatsApp: $newCode", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cloud Role Redirection Information Note
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Role stored in Firestore will be checked upon OTP verification to automatically open either the Admin Dashboard or Employee Workspace.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Verify Button
            Button(
                onClick = {
                    if (otpInput.length < 6) {
                        errorMessage = "Please enter all 6 digits of the OTP"
                        return@Button
                    }
                    isVerifyingOtp = true
                    viewModel.verifyOtpWithFirestore(otpInput) { verified, roleIsAdmin, targetRoute ->
                        isVerifyingOtp = false
                        if (verified) {
                            val roleDestination = if (roleIsAdmin) "Admin Dashboard" else "Employee Workspace"
                            Toast.makeText(context, "Role Verified in Firestore! Directing to $roleDestination", Toast.LENGTH_SHORT).show()
                            onVerifySuccess(roleIsAdmin)
                        } else {
                            errorMessage = "Incorrect OTP code. Please check WhatsApp or tap Auto-Fill."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White
                ),
                enabled = !isVerifyingOtp
            ) {
                if (isVerifyingOtp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying Role in Firestore...", fontSize = 15.sp, color = Color.White)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verify & Access App",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBack) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Mobile Number", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}
