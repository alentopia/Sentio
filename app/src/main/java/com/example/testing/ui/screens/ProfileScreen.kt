package com.example.testing.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(navController: NavController, edited: Boolean = false) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(Color.Transparent, darkIcons = true)
        systemUiController.setNavigationBarColor(Color.Transparent, darkIcons = true)
    }

    val firebaseAuth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = firebaseAuth.currentUser

    var showLogoutDialog by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("Unknown User") }

    // Load nama user dari Firestore
    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name") ?: ""
                    userName = if (name.isNotBlank()) name else "Unknown User"
                }
        }
    }

    // Snackbar setup
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Show snackbar after edit profile
    LaunchedEffect(edited) {
        if (edited) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Your profile has been edited")
                navController.navigate("profile") {
                    popUpTo("profile") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    // MAIN CONTENT
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 64.dp)
        ) {

            // ⭐ AVATAR SECTION FINAL
            item {
                val user = FirebaseAuth.getInstance().currentUser

                // Foto profil besar
                Image(
                    painter = rememberAsyncImagePainter(
                        model = user?.photoUrl
                            ?: "https://cdn-icons-png.flaticon.com/512/847/847969.png"
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Nama (dari Firestore)
                Text(
                    text = userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF2C2C2C)
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Profile Settings
            item {
                ProfileCard(
                    title = "Profile Settings",
                    options = listOf(
                        SettingItem(Icons.Default.Edit, "Edit Profile") {
                            navController.navigate("edit_profile")
                        }
                    )
                )
            }

            // Support Section
            item {
                ProfileCard(
                    title = "Support",
                    options = listOf(
                        SettingItem(Icons.Default.Help, "Help & Support") {
                            navController.navigate("help_support")
                        }
                    )
                )
            }

            // Logout Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A5A)),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp),
            snackbar = { snackbarData ->
                Surface(
                    color = Color(0xFFB3E5FC).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = snackbarData.visuals.message,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = Color(0xFF01579B),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        )
    }

    // Logout Dialog
    if (showLogoutDialog) {
        Dialog(
            onDismissRequest = { showLogoutDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Logout Confirmation",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A4458),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Are you sure you want to log out?",
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(
                                onClick = { showLogoutDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = Color.Gray)
                            }
                            Button(
                                onClick = {
                                    firebaseAuth.signOut()
                                    navController.navigate("signin") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8B4CFC)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Yes", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Profile Option Card
@Composable
fun ProfileCard(
    title: String,
    options: List<SettingItem>,
) {
    Column {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF222222),
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                options.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { item.onClick() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = Color(0xFF8B4CFC),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(item.title, color = Color(0xFF333333), fontSize = 15.sp)
                        }
                    }
                    if (index < options.lastIndex) Divider(color = Color(0xFFECE6FF))
                }
            }
        }
    }
}

data class SettingItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val onClick: () -> Unit
)
