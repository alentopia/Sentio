package com.example.testing.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun EditProfileScreen(navController: NavController) {
    val firebaseAuth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val scope = rememberCoroutineScope()

    // State untuk foto profil
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf("") }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            profileImageUri = uri
        }
    }
    // State data profile
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load user profile saat screen dibuka
    LaunchedEffect(Unit) {
        val userId = firebaseAuth.currentUser?.uid ?: return@LaunchedEffect
        try {
            val document = db.collection("users").document(userId).get().await()
            if (document.exists()) {
                // Ambil data profil dari Firestore dan set ke state
                name = document.getString("name") ?: ""
                email = document.getString("email") ?: ""
                phone = document.getString("phone") ?: ""
                profileImageUrl = document.getString("profileImage") ?: ""
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load profile"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF5E4AE3))
                }
                Text(
                    text = "Edit Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Foto profil
            Box(contentAlignment = Alignment.BottomEnd) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = profileImageUri ?: (profileImageUrl.takeIf { it.isNotEmpty() }
                            ?: "https://cdn-icons-png.flaticon.com/512/847/847969.png")
                    ),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCD6F7))
                )
                IconButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B4CFC))
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Photo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card Input
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        "Profile Information",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6C4BCC),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Full Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF8B4CFC)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B4CFC),
                            focusedLabelColor = Color(0xFF8B4CFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))  // Add spacing between fields

                    // Email Address
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF8B4CFC)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B4CFC),
                            focusedLabelColor = Color(0xFF8B4CFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))  // Add spacing between fields

                    // Phone Number
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF8B4CFC)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B4CFC),
                            focusedLabelColor = Color(0xFF8B4CFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))  // Add spacing between fields

                    //  Ganti Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("New Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8B4CFC)) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color(0xFF8B4CFC)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B4CFC),
                            focusedLabelColor = Color(0xFF8B4CFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))  // Add spacing between fields

                    // Error Message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            errorMessage ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tombol Simpan
            Button(
                onClick = { showDialog = true },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4CFC)),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Dialog Konfirmasi
    if (showDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Confirm Changes",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B4453),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Are you sure you want to save the changes to your profile?",
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { showDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel", color = Color.Gray)
                        }

                        Button(
                            onClick = {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val userId = firebaseAuth.currentUser?.uid ?: return@launch

                                        var finalImageUrl = profileImageUrl

                                        // Upload image jika ada gambar baru
                                        if (profileImageUri != null) {
                                            val storageRef = storage.reference.child("profile_images/$userId.jpg")
                                            storageRef.putFile(profileImageUri!!).await()
                                            finalImageUrl = storageRef.downloadUrl.await().toString()

                                            profileImageUrl = finalImageUrl      // <<< FIX DI SINI
                                        }

                                        // Update profile data ke Firestore
                                        val updates = mapOf(
                                            "name" to name,
                                            "email" to email,
                                            "phone" to phone,
                                            "profileImage" to finalImageUrl,
                                            "updatedAt" to com.google.firebase.Timestamp.now()
                                        )

                                        db.collection("users")
                                            .document(userId)
                                            .set(updates, SetOptions.merge())
                                            .await()

                                        // Update password jika ada
                                        if (password.isNotBlank()) {
                                            firebaseAuth.currentUser?.updatePassword(password)?.await()
                                        }

                                        isLoading = false
                                        showDialog = false
                                        navController.navigate("profile") {
                                            popUpTo("editprofile") { inclusive = true }
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        errorMessage = e.message ?: "Failed to save profile"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4CFC)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Yes, Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
