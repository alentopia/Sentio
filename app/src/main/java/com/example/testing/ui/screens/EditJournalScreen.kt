package com.example.testing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.testing.JurnalModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditJournalScreen(
    journalId: String,
    navController: NavController
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    var journal by remember { mutableStateOf<JurnalModel?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Load data dari Firebase
    LaunchedEffect(journalId) {
        db.collection("users")
            .document(uid)
            .collection("journals")
            .document(journalId)
            .get()
            .addOnSuccessListener { snap ->
                journal = snap.toObject(JurnalModel::class.java)?.copy(id = journalId)
            }
    }

    val data = journal ?: return Text("Loading...")

    // Editable fields
    var newTitle by remember { mutableStateOf(data.title) }
    var newContent by remember { mutableStateOf(data.content) }

    val mood = data.mood
    val location = data.location


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFEED3F2), Color(0xFFD1E5FF))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                SmallTopAppBar(
                    title = {
                        Text(
                            text = "Edit Journal",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A4458)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF8B4CFC)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 4.dp)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        OutlinedTextField(
                            value = mood,
                            onValueChange = {},
                            label = { Text("Mood") },
                            readOnly = true,
                            enabled = false,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Gray,
                                disabledBorderColor = Color.LightGray,
                                disabledLabelColor = Color(0xFF7D7A8B),
                                disabledContainerColor = Color(0xFFF9F9F9)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Title") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF8B4CFC),
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newContent,
                            onValueChange = { newContent = it },
                            label = { Text("Content") },
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 17.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF8B4CFC),
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = {},
                            label = { Text("Location") },
                            readOnly = true,
                            enabled = false,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Gray,
                                disabledBorderColor = Color.LightGray,
                                disabledLabelColor = Color(0xFF7D7A8B),
                                disabledContainerColor = Color(0xFFF9F9F9)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { showConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF8B4CFC),
                                RoundedCornerShape(24.dp)
                            )
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save Changes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Confirm dialog — tetap UI kamu
        if (showConfirmDialog) {
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
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Confirm Changes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Are you sure you want to save the changes?",
                            fontSize = 14.sp,
                            color = Color(0xFF555555),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { showConfirmDialog = false },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel", color = Color.Gray) }

                            Button(
                                onClick = {
                                    showConfirmDialog = false

                                    val updateMap = mapOf(
                                        "title" to newTitle,
                                        "content" to newContent,
                                        "isEdited" to true
                                    )

                                    db.collection("users")
                                        .document(uid)
                                        .collection("journals")
                                        .document(journalId)
                                        .update(updateMap)
                                        .addOnSuccessListener {
                                            navController.navigate("journal_detail/$journalId?edited=true") {
                                                popUpTo("journal_detail/$journalId") { inclusive = true }
                                            }
                                        }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(Color(0xFF8B4CFC))
                            ) {
                                Text("Save", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

