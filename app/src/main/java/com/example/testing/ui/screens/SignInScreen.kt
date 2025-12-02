package com.example.testing.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SignInScreen(navController: NavController, created: Boolean = false) {
    val firebaseAuth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    //  SharedPreferences untuk menyimpan data login
    val prefs = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)

    // State
    var email by remember { mutableStateOf(prefs.getString("email", "") ?: "") }
    var password by remember { mutableStateOf(prefs.getString("password", "") ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var stayLoggedIn by remember { mutableStateOf(prefs.getBoolean("stayLoggedIn", false)) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Snackbar setup
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    //  Kalau habis signup, tampilkan snackbar
    LaunchedEffect(created) {
        if (created) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Account created successfully!")
            }
        }
    }

    // Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7E63FF),
                        Color(0xFF9A7BFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Hello, There", color = Color.White.copy(alpha = 0.8f))
                Text(
                    "Welcome Back",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(30.dp))
                        .padding(4.dp)
                        .width(260.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Login", color = Color(0xFF7E63FF), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { navController.navigate("signup") },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Register", color = Color.White)
                    }
                }
            }

            //  Bottom Section
            Surface(
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Login to Your Account",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3A2E6E),
                        fontSize = 18.sp
                    )
                    Text(
                        "Make sure that you already have an account.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7E63FF),
                            unfocusedBorderColor = Color(0xFFD5CFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    //  Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7E63FF),
                            unfocusedBorderColor = Color(0xFFD5CFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stay Logged In + Forgot Password
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            //  Animated Checkbox
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (stayLoggedIn) Color(0xFF7E63FF)
                                        else Color.LightGray.copy(alpha = 0.4f)
                                    )
                                    .clickable { stayLoggedIn = !stayLoggedIn },
                                contentAlignment = Alignment.Center
                            ) {
                                //  AnimatedVisibility tetap dalam konteks @Composable
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = stayLoggedIn,
                                    enter = androidx.compose.animation.scaleIn(
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 200
                                        )
                                    ) + androidx.compose.animation.fadeIn(
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 200
                                        )
                                    ),
                                    exit = androidx.compose.animation.scaleOut(
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 200
                                        )
                                    ) + androidx.compose.animation.fadeOut(
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 200
                                        )
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stay Logged In", color = Color.Gray, fontSize = 13.sp)
                        }
                        Text(
                            text = "Forgot Password?",
                            color = Color(0xFF7E63FF),
                            fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                navController.navigate("forgotpassword")
                            }
                        )
                    }

                    //  Login Button
                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                scope.launch {
                                    firebaseAuth.signInWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            isLoading = false
                                            if (task.isSuccessful) {
                                                val userId = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener

                                                // Update lastLogin di Firestore
                                                val updates = mapOf(
                                                    "lastLogin" to com.google.firebase.Timestamp.now()
                                                )

                                                db.collection("users").document(userId).update(updates)

                                                if (stayLoggedIn) {
                                                    prefs.edit().apply {
                                                        putString("email", email)
                                                        putString("password", password)
                                                        putBoolean("stayLoggedIn", true)
                                                        apply()
                                                    }
                                                } else {
                                                    prefs.edit().clear().apply()
                                                }

                                                navController.navigate("home") {
                                                    popUpTo("signin") { inclusive = true }
                                                }
                                            } else {
                                                errorMessage = "Invalid credentials"
                                            }
                                        }
                                }
                            } else {
                                errorMessage = "Please fill all fields"
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E63FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Login", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Error
                    errorMessage?.let {
                        Text(it, color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        //  Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 270.dp),
            snackbar = { snackbarData ->
                Surface(
                    color = Color(0xFFE8F8F0).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = snackbarData.visuals.message,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        Color(0xFF3C755F),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        )
    }
}