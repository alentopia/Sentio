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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.testing.JurnalModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProfileScreen(navController: NavController, edited: Boolean = false) {

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(Color.Transparent, darkIcons = true)
        systemUiController.setNavigationBarColor(Color.Transparent, darkIcons = true)
    }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("Unknown User") }

    val journals = remember { mutableStateListOf<JurnalModel>() }

    var journalCount by remember { mutableStateOf(0) }
    var totalMood by remember { mutableStateOf(0) }
    var goalsCompleted by remember { mutableStateOf(0) }

    // Load user name
    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    userName = doc.getString("name") ?: "Unknown User"
                }
        }
    }

    // Load Journals EXACT same logic as HomeScreen
    LaunchedEffect(Unit) {
        val uid = currentUser?.uid ?: return@LaunchedEffect

        firestore.collection("users")
            .document(uid)
            .collection("journals")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    journals.clear()

                    snapshot.documents.forEach { doc ->
                        journals.add(
                            JurnalModel(
                                id = doc.id,
                                emoji = doc.getString("emoji") ?: "",
                                mood = doc.getString("mood") ?: "",
                                title = doc.getString("title") ?: "",
                                content = doc.getString("content") ?: "",
                                location = doc.getString("location") ?: "",
                                date = doc.getString("createdAt") ?: "",
                                isEdited = doc.getBoolean("isEdited") ?: false
                            )
                        )
                    }

                    journalCount = journals.count { it.title.isNotBlank() || it.content.isNotBlank() }
                    totalMood = journals.size
                }
            }
    }

    // Load Goals Completed
    LaunchedEffect(currentUser) {
        val uid = currentUser?.uid ?: return@LaunchedEffect

        firestore.collection("users")
            .document(uid)
            .collection("weeklygoal")
            .document("current")
            .collection("history")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    var total = 0
                    snapshot.documents.forEach { doc ->
                        val daysCompleted = doc.getLong("daysCompleted") ?: 0
                        val targetDays = doc.getLong("targetDays") ?: 0

                        if (daysCompleted >= targetDays && targetDays > 0) {
                            total += 1
                        }
                    }
                        goalsCompleted = total
                }
            }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            item {
                val user = FirebaseAuth.getInstance().currentUser

                Image(
                    painter = rememberAsyncImagePainter(
                        model = user?.photoUrl
                            ?: "https://cdn-icons-png.flaticon.com/512/847/847969.png"
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF2C2C2C)
                )

                Spacer(Modifier.height(20.dp))
            }

            // === STATS ===
            item {
                ProfileStatsRow(
                    journalCount = journalCount,
                    totalMood = totalMood,
                    goalsCompleted = goalsCompleted
                )
            }

            item {
                ProfileCard(
                    title = "Profile Settings",
                    options = listOf(
                        SettingItem(Icons.Default.Edit, "Edit Profile") {
                            navController.navigate("edit_profile")
                        },
                        SettingItem(Icons.Default.Lock, "Password & Email Settings") {
                            navController.navigate("security_settings")
                        }
                    )
                )
            }

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

            /** LOGOUT **/
            item {
                Button(
                    onClick = { showLogoutDialog = true },
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A5A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate("signin") {
                    popUpTo("home") { inclusive = true }
                }
            }
        )
    }
}

@Composable
fun ProfileStatsRow(journalCount: Int, totalMood: Int, goalsCompleted: Int){
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(Icons.Default.MenuBook, journalCount.toString(), "Journals Written", Modifier.weight(1f))
        StatCard(Icons.Default.Mood, totalMood.toString(), "Total Mood", Modifier.weight(1f))
        StatCard(Icons.Default.TaskAlt, goalsCompleted.toString(), "Goals Done", Modifier.weight(1f))
    }
}

@Composable
fun StatCard(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .fillMaxSize(),
            verticalArrangement  = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Color(0xFF8B4CFC), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF44345C))
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ProfileCard(title: String, options: List<SettingItem>) {
    Column {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF222222),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                options.forEachIndexed { idx, opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { opt.onClick() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(opt.icon, null, tint = Color(0xFF8B4CFC))
                        Spacer(Modifier.width(12.dp))
                        Text(opt.title, color = Color(0xFF333333))
                    }
                    if (idx < options.lastIndex) Divider(color = Color(0xFFECE6FF))
                }
            }
        }
    }
}

data class SettingItem(val icon: ImageVector, val title: String, val onClick: () -> Unit)

/** ---------------- LOGOUT DIALOG ---------------- **/
@Composable
fun LogoutDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(12.dp),
                modifier = Modifier.fillMaxWidth(0.82f)
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Logout Confirmation", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Are you sure you want to log out?", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4CFC))
                        ) {
                            Text("Yes", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
