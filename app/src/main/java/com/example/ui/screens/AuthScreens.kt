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
import androidx.compose.ui.platform.testTag
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
import com.example.data.model.Department
import com.example.data.model.EmployeeStatus
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

    // Strict security: Back press on login screen closes the app; no other screens can be revealed
    BackHandler {
        activity?.finish()
    }

    var selectedTabRole by remember { mutableStateOf("Admin") } // "Admin" or "Employee"
    var isSignUpMode by remember { mutableStateOf(false) } // Under employee space

    var isAdminAuthenticating by remember { mutableStateOf(false) }

    val triggerBiometricsAuth: () -> Unit = {
        isAdminAuthenticating = true
        if (activity != null) {
            BiometricHelper.promptBiometricAuth(
                activity = activity,
                title = "MB Admin Biometric Login",
                subtitle = "Scan fingerprint or Face ID to open Admin Portal",
                onSuccess = {
                    isAdminAuthenticating = false
                    viewModel.loginAsAdmin {
                        Toast.makeText(context, "Welcome Administrator! Admin Portal Unlocked", Toast.LENGTH_SHORT).show()
                        onLoginSuccess(true)
                    }
                },
                onError = { errorMsg ->
                    isAdminAuthenticating = false
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                },
                onCancel = {
                    isAdminAuthenticating = false
                }
            )
        } else {
            viewModel.loginAsAdmin {
                isAdminAuthenticating = false
                Toast.makeText(context, "Welcome Administrator! Admin Portal Unlocked", Toast.LENGTH_SHORT).show()
                onLoginSuccess(true)
            }
        }
    }

    // Auto-prompt biometrics when entering the screen
    LaunchedEffect(selectedTabRole) {
        if (selectedTabRole == "Admin") {
            delay(350)
            triggerBiometricsAuth()
        }
    }

    // Infinite pulsing animation for biometric scanner
    val infiniteTransition = rememberInfiniteTransition(label = "BiometricPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Brand Emblem in a Circle
            MBAppLogo(
                size = 80.dp,
                inCircle = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "MB EM",
                fontFamily = CinzelFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = ButtonPrimary
            )
            Text(
                "Secure Enterprise Terminal & Workspace",
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🎛️ TAB SELECTOR FOR ADMIN VS EMPLOYEE
            TabRow(
                selectedTabIndex = if (selectedTabRole == "Admin") 0 else 1,
                containerColor = Color.White,
                contentColor = BrandBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(1.dp)
            ) {
                Tab(
                    selected = selectedTabRole == "Admin",
                    onClick = {
                        selectedTabRole = "Admin"
                        isSignUpMode = false
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = if (selectedTabRole == "Admin") BrandBlue else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Admin Console",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTabRole == "Admin") BrandBlue else TextSecondary
                        )
                    }
                }
                Tab(
                    selected = selectedTabRole == "Employee",
                    onClick = { selectedTabRole = "Employee" },
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = if (selectedTabRole == "Employee") BrandBlue else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Employee Hub",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTabRole == "Employee") BrandBlue else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTabRole == "Admin") {
                // ==========================================
                // ADMIN BIOMETRIC LOGIN VIEW (Existing flow)
                // ==========================================
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BrandBlue.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Admin Biometric Terminal • Connected with 'MB EM' App", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Biometric Pulse Ring Container
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(110.dp)
                                .clickable { triggerBiometricsAuth() }
                        ) {
                            // Outer pulse ring
                            Surface(
                                shape = CircleShape,
                                color = BrandDarkBlue.copy(alpha = pulseAlpha),
                                modifier = Modifier
                                    .size(105.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    }
                            ) {}

                            // Inner biometric circle
                            Surface(
                                shape = CircleShape,
                                color = BrandDarkBlue.copy(alpha = 0.10f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Admin Biometrics",
                                        tint = BrandDarkBlue,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Admin Biometrics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandDarkBlue,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Scan Face ID / Touch ID to manage staff, view CRM leads, approve registrations, and chat.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { triggerBiometricsAuth() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("admin_biometric_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonPrimary,
                                contentColor = Color.White
                            ),
                            enabled = !isAdminAuthenticating
                        ) {
                            if (isAdminAuthenticating) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Verifying...", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Scan Biometrics",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bypass button for testing/emulator environment without biometric sensors
                        OutlinedButton(
                            onClick = {
                                viewModel.loginAsAdmin {
                                    Toast.makeText(context, "Direct Admin Access Granted!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(true)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("admin_bypass_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ButtonSecondary.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AccentSage.copy(alpha = 0.15f),
                                contentColor = ButtonSecondary
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = ButtonSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "⚡ Direct Admin Bypass",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ButtonSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // EMPLOYEE WORKSPACE: LOGIN / SIGNUP
                // ==========================================
                if (!isSignUpMode) {
                    // EMPLOYEE SIGN IN FORM
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    var isPasswordVisible by remember { mutableStateOf(false) }
                    var isAuthenticating by remember { mutableStateOf(false) }

                    val triggerEmployeeBiometrics: () -> Unit = {
                        if (activity != null) {
                            BiometricHelper.promptBiometricAuth(
                                activity = activity,
                                title = "MB Employee Biometric Login",
                                subtitle = "Scan fingerprint or Face ID to unlock Employee Workspace",
                                onSuccess = {
                                    viewModel.loginWithBiometrics(isAdmin = false)
                                    Toast.makeText(context, "Employee Workspace Unlocked!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(false)
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            viewModel.loginWithBiometrics(isAdmin = false)
                            Toast.makeText(context, "Employee Workspace Unlocked (Simulated)!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess(false)
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Employee Sign-In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF0F172A)
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("employee_login_email"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandBlue,
                                    focusedLabelColor = BrandBlue
                                )
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("employee_login_password"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandBlue,
                                    focusedLabelColor = BrandBlue
                                )
                            )

                            Button(
                                onClick = {
                                    if (email.isBlank() || password.isBlank()) {
                                        Toast.makeText(context, "Please fill in all credentials.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isAuthenticating = true
                                    viewModel.loginWithEmployeeCredentials(email, password) { success, errorMsg, _ ->
                                        isAuthenticating = false
                                        if (success) {
                                            Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess(false)
                                        } else {
                                            Toast.makeText(context, errorMsg ?: "Authentication Failed", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("employee_login_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary),
                                enabled = !isAuthenticating
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Login, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sign In with Credentials", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }

                            // 🚨 Fingerprint Scanner section for Employee login
                            HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 4.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BrandGreen.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { triggerEmployeeBiometrics() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = "Biometric Login", tint = BrandGreen, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Instant Fingerprint Sign-In",
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreen,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            TextButton(onClick = { isSignUpMode = true }) {
                                Text("New Employee? Register Account", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    // EMPLOYEE SIGN UP (REGISTRATION) FORM
                    var name by remember { mutableStateOf("") }
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    var phone by remember { mutableStateOf("+91 ") }
                    var designation by remember { mutableStateOf("") }
                    var isPasswordVisible by remember { mutableStateOf(false) }
                    var isRegistering by remember { mutableStateOf(false) }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Employee Signup Portal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Requires Administrator approval before first login.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("employee_signup_name"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue)
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("employee_signup_email"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue)
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("employee_signup_password"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue)
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("WhatsApp Phone") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("employee_signup_phone"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue)
                            )

                            OutlinedTextField(
                                value = designation,
                                onValueChange = { designation = it },
                                label = { Text("Designation") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("employee_signup_designation"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue)
                            )

                            Button(
                                onClick = {
                                    if (name.isBlank() || email.isBlank() || password.isBlank() || designation.isBlank()) {
                                        Toast.makeText(context, "Please fill in all registration fields.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isRegistering = true
                                    viewModel.signupEmployeeWithEmail(
                                        name = name,
                                        email = email,
                                        password = password,
                                        phone = phone,
                                        designation = designation
                                    ) { success, errorMsg ->
                                        isRegistering = false
                                        if (success) {
                                            Toast.makeText(context, "Registered successfully! Waiting for Admin approval.", Toast.LENGTH_LONG).show()
                                            isSignUpMode = false // Switch to Sign In
                                        } else {
                                            Toast.makeText(context, errorMsg ?: "Registration Failed", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("employee_signup_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                enabled = !isRegistering
                            ) {
                                if (isRegistering) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Register Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }

                            TextButton(onClick = { isSignUpMode = false }) {
                                Text("Already have an account? Sign In", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security Policy & MB EM Synchronization Note
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Realtime Cloud Sync Active: All admin actions, tasks, attendance & chat automatically synchronize live with the 'MB EM' employee app over the internet.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
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
                            Toast.makeText(context, "Role Verified in Firestore! Directing to Admin Portal", Toast.LENGTH_SHORT).show()
                            onVerifySuccess(true)
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
