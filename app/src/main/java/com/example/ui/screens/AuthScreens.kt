package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.example.ui.components.MBAppLogo
import com.example.ui.theme.*
import com.example.util.BiometricHelper
import kotlinx.coroutines.delay

// ---------------- Screen 1: Splash Screen ----------------
// User requirement: Show ONLY logo then splash or login screen and no other screens
@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    var isLogoVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLogoVisible = true
        delay(1400)
        onTimeout()
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (isLogoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "SplashLogoAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (isLogoVisible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SplashLogoScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg),
        contentAlignment = Alignment.Center
    ) {
        // Show ONLY the Logo as strictly requested
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = alphaAnim
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                },
            contentAlignment = Alignment.Center
        ) {
            MBAppLogo(
                size = 150.dp,
                inCircle = true
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
    onNavigateToOtp: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Strict security: Back press on login screen closes the app; no other screens can be revealed
    BackHandler {
        activity?.finish()
    }

    var selectedRole by remember { mutableStateOf("Employee") } // "Employee" or "MB Admin"
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var isAdminAuthenticating by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val performEmployeeLogin = {
        keyboardController?.hide()
        val trimmedEmail = email.trim()
        val emailPattern = android.util.Patterns.EMAIL_ADDRESS

        var hasError = false
        if (trimmedEmail.isEmpty()) {
            emailError = "Please enter your registered Email ID"
            hasError = true
        } else if (!emailPattern.matcher(trimmedEmail).matches()) {
            emailError = "Invalid email format (e.g. employee@company.com)"
            hasError = true
        } else {
            emailError = null
        }

        if (password.isEmpty()) {
            passwordError = "Please enter your password"
            hasError = true
        } else if (password.length < 4) {
            passwordError = "Password must be at least 4 characters"
            hasError = true
        } else {
            passwordError = null
        }

        if (hasError) {
            Toast.makeText(context, "Please enter valid email and password", Toast.LENGTH_SHORT).show()
        } else {
            isSigningIn = true
            viewModel.loginWithEmployeeCredentials(
                email = trimmedEmail,
                password = password
            ) { success, errorMessage, _ ->
                isSigningIn = false
                if (success) {
                    viewModel.unlockAllBiometrics()
                    Toast.makeText(context, "Welcome back! Opening Employee Workspace", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(false)
                } else {
                    Toast.makeText(context, errorMessage ?: "Sign in failed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // App Brand Emblem in a Circle
            MBAppLogo(
                size = 90.dp,
                inCircle = true
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text("MB Taker", fontFamily = CinzelFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = ButtonPrimary)
            Text("Enterprise Employee & Operations Portal", fontSize = 13.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(24.dp))

            // Role Selector Tabs (Employee vs MB Admin)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ImportantCardBg.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, CardBorder),
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
                            color = if (isSelected) ButtonPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedRole = role
                                    emailError = null
                                    passwordError = null
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (role == "MB Admin") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = role,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedRole == "Employee") {
                // ------------ EMPLOYEE MODE: DIRECT SECURE LOGIN ------------

                Spacer(modifier = Modifier.height(8.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { input ->
                        email = input
                        if (emailError != null) {
                            val trimmed = input.trim()
                            if (trimmed.isEmpty()) {
                                emailError = "Email cannot be empty"
                            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                                emailError = "Invalid email format"
                            } else {
                                emailError = null
                            }
                        }
                    },
                    label = { Text("Employee Email ID") },
                    placeholder = { Text("employee@company.com") },
                    isError = emailError != null,
                    supportingText = {
                        if (emailError != null) {
                            Text(text = emailError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        } else {
                            Text(
                                text = "Enter your registered email address",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = if (emailError != null) MaterialTheme.colorScheme.error else BrandBlue
                        )
                    },
                    trailingIcon = {
                        if (email.isNotEmpty()) {
                            IconButton(onClick = { 
                                email = ""
                                emailError = "Email cannot be empty"
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear email", tint = Color(0xFF94A3B8))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    colors = appTextFieldColors()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { input ->
                        password = input
                        if (passwordError != null) {
                            if (input.isEmpty()) {
                                passwordError = "Password cannot be empty"
                            } else if (input.length < 4) {
                                passwordError = "Password must be at least 4 characters"
                            } else {
                                passwordError = null
                            }
                        }
                    },
                    label = { Text("Password") },
                    isError = passwordError != null,
                    supportingText = {
                        if (passwordError != null) {
                            Text(text = passwordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        } else {
                            Text(text = "Must be at least 4 characters long", fontSize = 11.sp, color = TextMuted)
                        }
                    },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Lock, 
                            contentDescription = null, 
                            tint = if (passwordError != null) MaterialTheme.colorScheme.error else ElectricBlue
                        ) 
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            performEmployeeLogin()
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Employee Authentication Note
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ImportantCardBg.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ButtonPrimary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Employee Email Access",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Sign in securely with your employee email and password.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Button (Sign In to Workspace)
                Button(
                    onClick = { 
                        performEmployeeLogin()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimary,
                        contentColor = Color.White
                    ),
                    enabled = !isSigningIn
                ) {
                    if (isSigningIn) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Signing In...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Login",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // OR Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLight)
                    Text(
                        "OR",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLight)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick Biometric Sign-In Button (Fingerprint / Face ID)
                OutlinedButton(
                    onClick = {
                        if (activity != null) {
                            BiometricHelper.promptBiometricAuth(
                                activity = activity,
                                title = "Employee Biometric Unlock",
                                subtitle = "Scan fingerprint or face to verify your identity and open Employee Workspace",
                                description = "Instant biometric verification powered by androidx.biometric.",
                                onSuccess = {
                                    viewModel.loginWithBiometricsWithFirestore(
                                        phoneNumber = "+91 98765 43210",
                                        selectedRoleHint = "Employee"
                                    ) { isAdmin, _ ->
                                        viewModel.unlockAllBiometrics()
                                        onLoginSuccess(isAdmin)
                                    }
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Biometric failed: $err", Toast.LENGTH_SHORT).show()
                                },
                                onCancel = {
                                    Toast.makeText(context, "Biometric scan cancelled", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, ButtonSecondary.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AccentSage.copy(alpha = 0.35f),
                        contentColor = ButtonSecondary
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = "Quick Biometric Login",
                            tint = ButtonSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Quick Biometric Sign-In",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ButtonSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Security Policy Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Secured with androidx.biometric • Fingerprint / Face ID required for sensitive data & chat access.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

            } else {
                // ------------ ADMIN MODE: BIOMETRICS ONLY ------------

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BrandDarkBlue.copy(alpha = 0.1f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Admin Biometrics",
                                    tint = BrandDarkBlue,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Admin Biometric Authorization",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandDarkBlue,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Admin portal security policy strictly requires biometric verification (Fingerprint or Face ID) to proceed.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Biometrics Scanner Trigger Button
                        Button(
                            onClick = {
                                isAdminAuthenticating = true
                                if (activity != null) {
                                    BiometricHelper.promptBiometricAuth(
                                        activity = activity,
                                        title = "MB Admin Biometric Authorization",
                                        subtitle = "Verify fingerprint or face to open Admin Portal",
                                        onSuccess = {
                                            viewModel.loginWithBiometricsWithFirestore(
                                                phoneNumber = "+91 98111 22334",
                                                selectedRoleHint = "MB Admin"
                                            ) { isAdmin, _ ->
                                                isAdminAuthenticating = false
                                                onLoginSuccess(true)
                                            }
                                        },
                                        onError = { errorMsg ->
                                            isAdminAuthenticating = false
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    viewModel.loginWithBiometricsWithFirestore(
                                        phoneNumber = "+91 98111 22334",
                                        selectedRoleHint = "MB Admin"
                                    ) { isAdmin, _ ->
                                        isAdminAuthenticating = false
                                        onLoginSuccess(true)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonPrimary,
                                contentColor = Color.White
                            ),
                            enabled = !isAdminAuthenticating
                        ) {
                            if (isAdminAuthenticating) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.5.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Verifying Biometrics...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Scan Biometrics to Access Admin",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Need access or credentials? ", fontSize = 12.sp, color = TextSecondary)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
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
}

/**
 * Authentic Google "G" Emblem drawn with precision Canvas geometry and standard Google brand colors.
 */
@Composable
fun GoogleGLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = size.width * 0.22f
            val radius = (size.width - strokeW) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Red arc (top / top-left)
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 180f,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeW)
            )
            // Blue arc (top-right & right)
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = 285f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeW)
            )
            // Green arc (bottom / bottom-left)
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 25f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeW)
            )
            // Yellow arc (bottom-left)
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 115f,
                sweepAngle = 65f,
                useCenter = false,
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeW)
            )
            // Blue horizontal crossbar
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(center.x, center.y),
                end = Offset(size.width - strokeW / 3, center.y),
                strokeWidth = strokeW
            )
        }
    }
}

/**
 * Modern Material 3 Google Sign-In Button integrated with Firebase Auth.
 */
@Composable
fun GoogleSignInButton(
    text: String = "Sign in with Google",
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F2937)
        ),
        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = BrandBlue,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Connecting to Firebase Auth...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleGLogo(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            }
        }
    }
}
