package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

// ---------------- Screen 1: Splash Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandDarkBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BrandBlue,
                modifier = Modifier.size(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "MB",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Building Better Teams\nfor a Bigger Tomorrow",
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
    }
}

// ---------------- Screen 2: Login Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (isAdmin: Boolean) -> Unit,
    onNavigateToOtp: () -> Unit
) {
    var selectedRole by remember { mutableStateOf("Employee") } // "Employee" or "MB Admin"
    var emailOrPhone by remember { mutableStateOf("rahul@mbtraker.com") }
    var password by remember { mutableStateOf("password123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    LaunchedEffect(selectedRole) {
        if (selectedRole == "MB Admin") {
            emailOrPhone = "admin@mbtraker.com"
            password = "admin123"
        } else {
            emailOrPhone = "rahul@mbtraker.com"
            password = "password123"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // MB Logo
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BrandBlue,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("MB", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("MB Traker", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BrandDarkBlue)
            Spacer(modifier = Modifier.height(20.dp))

            Text("Welcome Back", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
            Text("Select Role & Sign in to your portal", fontSize = 13.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(16.dp))

            // Role Selector Tabs
            Surface(
                shape = RoundedCornerShape(12.dp),
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
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BrandBlue else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRole = role }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (role == "MB Admin") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF334155),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = role,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Inputs
            OutlinedTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                label = { Text("Mobile Number / Email") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                colors = appTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                    )
                    Text("Remember me", fontSize = 12.sp, color = TextSecondary)
                }

                Text(
                    "Forgot Password?",
                    fontSize = 12.sp,
                    color = BrandBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onLoginSuccess(selectedRole == "MB Admin") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRole == "MB Admin") Color(0xFF1E1B4B) else Color(0xFF0F172A),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (selectedRole == "MB Admin") "Login as MB Admin" else "Login as Employee",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Row {
                Text("Don't have an account? ", fontSize = 12.sp, color = TextSecondary)
                Text("Contact Admin", fontSize = 12.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------- Screen 3: OTP Verification Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    onVerifySuccess: () -> Unit,
    onBack: () -> Unit
) {
    var otpValues by remember { mutableStateOf(listOf("4", "9", "2", "8", "", "")) }
    var countdown by remember { mutableIntStateOf(30) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OTP Verification", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                shape = CircleShape,
                color = Color(0xFFDBEAFE),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(44.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("We have sent a 6 digit code to", fontSize = 13.sp, color = TextSecondary)
            Text("+91 98765 43210", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            Spacer(modifier = Modifier.height(30.dp))

            // 6-digit pin boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (0 until 6).forEach { index ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (otpValues[index].isNotEmpty()) BrandBlue else BorderLight),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = otpValues[index],
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                if (countdown > 0) "Resend OTP (00:${if (countdown < 10) "0$countdown" else countdown})"
                else "Resend OTP",
                fontSize = 13.sp,
                color = if (countdown > 0) TextMuted else BrandBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    if (countdown == 0) countdown = 30
                }
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onVerifySuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White
                )
            ) {
                Text("Verify", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBack) {
                Text("Back to Login", color = TextSecondary)
            }
        }
    }
}
