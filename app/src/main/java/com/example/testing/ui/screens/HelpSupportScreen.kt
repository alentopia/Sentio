package com.example.testing.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


// ===============================
// DATA MODEL
// ===============================
data class SupportTicket(
    val title: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now()
)


// ===============================
// FIRESTORE – SAVE SUPPORT TICKET
// ===============================
fun sendSupportTicket(
    userId: String,
    title: String,
    content: String,
    imageUrl: String?,
    onResult: (Boolean) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    val ticket = SupportTicket(
        title = title,
        content = content,
        imageUrl = imageUrl,
        createdAt = Timestamp.now()
    )

    db.collection("users")
        .document(userId)
        .collection("supportTickets")
        .add(ticket)
        .addOnSuccessListener { onResult(true) }
        .addOnFailureListener { onResult(false) }
}


// ===============================
// STORAGE – UPLOAD IMAGE
// ===============================
fun uploadSupportImage(uri: Uri, userId: String, onUploaded: (String?) -> Unit) {

    val storageRef = FirebaseStorage.getInstance().reference
        .child("supportImages/$userId/${System.currentTimeMillis()}.jpg")

    storageRef.putFile(uri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { url ->
                onUploaded(url.toString())
            }
        }
        .addOnFailureListener { onUploaded(null) }
}


// ===============================
// MAIN SCREEN
// ===============================
@Composable
fun HelpSupportScreen(navController: NavController) {

    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF8B4CFC)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Help & Support",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            Icon(
                imageVector = Icons.Default.Help,
                contentDescription = null,
                tint = Color(0xFF8B4CFC),
                modifier = Modifier.size(70.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text("Need some help?", fontWeight = FontWeight.Bold, fontSize = 20.sp)

            Text(
                "Find quick answers or contact our team.",
                color = Color.Gray,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(28.dp))

            // FAQ SECTION (KEMBALI MUNCUL)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text(
                        "Frequently Asked Questions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF8B4CFC)
                    )

                    Spacer(Modifier.height(16.dp))

                    FAQItem(
                        question = "Why can’t I use my microphone/location/camera?",
                        answer = "Please make sure you’ve granted the necessary permissions."
                    )

                    FAQItem(
                        question = "Why can’t I access the music?",
                        answer = "You need an active internet connection to stream the music."
                    )

                    FAQItem(
                        question = "How to delete the journal?",
                        answer = "Press and hold the journal you want to delete."
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // CONTACT SUPPORT BUTTON
            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4CFC)),
                modifier = Modifier.fillMaxWidth().height(55.dp)
            ) {
                Icon(Icons.Default.MailOutline, contentDescription = "Email", tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Contact Support", color = Color.White)
            }
        }
    }

    // DIALOG + BACKGROUND BLUR
    if (showDialog) {

        // BACKGROUND BLUR
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(16.dp)
                .background(Color(0x33000000))
        )

        ContactSupportDialog(
            userId = "YOUR_USER_ID",
            onDismiss = { showDialog = false }
        )
    }
}


// ===============================
// FAQ ITEM COMPONENT
// ===============================
@Composable
fun FAQItem(question: String, answer: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(question, fontWeight = FontWeight.SemiBold)
        Text(answer, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        Divider(color = Color(0xFFF0EAFD), thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
    }
}



// ALERT DIALOG (TETAP SAMA TAPI AESTHETIC)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSupportDialog(
    userId: String,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedImage = it
    }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = {
            Text("Contact Support", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        confirmButton = {},
        text = {
            Column(Modifier.fillMaxWidth()) {

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Describe your problem") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { pickImage.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4CFC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Attach Image", color = Color.White)
                }

                selectedImage?.let {
                    Spacer(Modifier.height(10.dp))
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (title.isNotEmpty() && content.isNotEmpty()) {

                            isSending = true

                            if (selectedImage != null) {
                                uploadSupportImage(selectedImage!!, userId) { url ->
                                    sendSupportTicket(userId, title, content, url) {
                                        isSending = false
                                        onDismiss()
                                    }
                                }
                            } else {
                                sendSupportTicket(userId, title, content, null) {
                                    isSending = false
                                    onDismiss()
                                }
                            }

                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4CFC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Send Ticket", color = Color.White)
                    }
                }
            }
        }
    )
}
