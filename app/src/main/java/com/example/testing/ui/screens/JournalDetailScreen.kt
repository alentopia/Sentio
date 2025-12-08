package com.example.testing.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.testing.JurnalModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalDetailScreen(
    journalId: String,
    edited: Boolean = false,
    navController: NavController
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    var journal by remember { mutableStateOf<JurnalModel?>(null) }

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // RANDOM QUOTE
    val quotes = listOf(
        "“Every emotion is valid — and every day is progress.”",
        "“You’re allowed to feel. You’re allowed to rest.”",
        "“Healing is not linear, and that’s okay.”",
        "“You did your best today — and that’s enough.”",
        "“Small steps still move you forward.”",
        "“Don’t rush the process. Growth takes time.”",
        "“Even on cloudy days, the sun is still there.”",
        "“Your feelings are temporary, but your strength is lasting.”"
    )
    val randomQuote = remember { quotes.random() }

    // LOAD DATA FIRESTORE
    LaunchedEffect(journalId) {
        db.collection("users")
            .document(uid)
            .collection("journals")
            .document(journalId)
            .get()
            .addOnSuccessListener { doc ->
                val item = doc.toObject(JurnalModel::class.java)

                if (item != null) {
                    journal = item.copy(
                        id = doc.id,
                        date = doc.getString("createdAt") ?: item.date   // ⭐ FIX DI SINI
                    )
                }
            }
    }

    // SNACKBAR WHEN EDITED
    LaunchedEffect(edited) {
        if (edited) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Journal has been edited")
            }
        }
    }

    val data = journal ?: return Text("Loading...")

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // TOP BUTTONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                navController.navigate("journal_list") {
                    popUpTo("journal_list") { inclusive = true }
                }
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF8B4CFC)
                )
            }

            IconButton(
                onClick = {
                    navController.navigate("edit_journal/${data.id}")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Journal",
                    tint = Color(0xFF8B4CFC)
                )
            }
        }

        // CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 56.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = data.emoji, fontSize = 70.sp)
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = data.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF44345C)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${data.location} • ${data.date}",
                fontSize = 13.sp,
                color = Color(0xFF7D7A8B)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // JOURNAL CONTENT BOX
            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp,
                color = Color.White.copy(alpha = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = data.content.ifBlank { "No journal content written." },
                        fontSize = 16.sp,
                        color = Color(0xFF444444),
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // RANDOM QUOTE
            Text(
                text = randomQuote,
                fontSize = 14.sp,
                color = Color(0xFF6B5FAE),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // SNACKBAR
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            snackbar = { snackbarData ->
                Surface(
                    color = Color(0xFFE6F2FF).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = snackbarData.visuals.message,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        )
    }
}
