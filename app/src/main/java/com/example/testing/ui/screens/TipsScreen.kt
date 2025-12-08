package com.example.testing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testing.JurnalModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class TipsChatViewModel : ViewModel() {

    val messageList = mutableStateListOf<MessageModel>()

    var currentStep by mutableStateOf(1) // 1=tanggal, 2=jurnal, 3=AI
    var selectedDate: String? by mutableStateOf(null)
    var selectedJournal: JurnalModel? by mutableStateOf(null)

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
        messageList.add(
            MessageModel(
                "Silakan pilih tanggal jurnal yang ingin kamu mintai tips ✨",
                "model"
            )
        )
    }

    // Kirim ke Firestore → Firebase Extension akan generate response
    fun askTipsFromJournal(journal: JurnalModel) {
        selectedJournal = journal

        viewModelScope.launch {

            val prompt = """
                Judul: ${journal.title}
                Mood: ${journal.mood}
                Isi jurnal:
                ${journal.content}

                Berikan tips singkat, empatik, dan actionable.
            """.trimIndent()

            val docRef = db.collection("users")
                .document(uid)
                .collection("generate")
                .document()

            docRef.set(
                mapOf(
                    "prompt" to prompt,
                    "createTime" to com.google.firebase.Timestamp.now()
                )
            )

            messageList.add(MessageModel("Typing...", "model"))

            // LISTEN UNTUK RESPONSE DARI EXTENSION
            docRef.addSnapshotListener { snap, _ ->
                if (snap != null && snap.contains("response")) {
                    val resp = snap.getString("response") ?: "No response"

                    // Hapus "Typing..."
                    if (messageList.last().message == "Typing...") {
                        if (messageList.isNotEmpty()) {
                            messageList.removeAt(messageList.lastIndex)
                        }
                    }

                    // Tambahkan hasil AI
                    messageList.add(MessageModel(resp, "model"))

                    currentStep = 3
                }
            }
        }
    }
}

data class MessageModel(
    val message: String,
    val role: String // user / model
)


@Composable
fun TipsScreen(
    onBack: () -> Unit = {},
    viewModel: TipsChatViewModel = viewModel()
) {
    val messages = viewModel.messageList
    val step = viewModel.currentStep

    var fullJournalList by remember { mutableStateOf<List<JurnalModel>>(emptyList()) }
    var dateList by remember { mutableStateOf<List<String>>(emptyList()) }
    var journalsByDate by remember { mutableStateOf<List<JurnalModel>>(emptyList()) }

    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Load journaling data
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("journals")
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.map { doc ->
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
                }

                fullJournalList = list

                dateList = list
                    .map { it.date.substringBefore(",") }
                    .distinct()
                    .sortedDescending()
            }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "")
            }
            Text("AI Tips", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(messages.reversed()) {
                ChatBubble(it)
            }

            // STEP 1 → pilih tanggal
            if (step == 1) {
                items(dateList) { date ->
                    OptionBubble(text = date) {
                        viewModel.selectedDate = date
                        viewModel.currentStep = 2

                        journalsByDate = fullJournalList.filter {
                            it.date.startsWith(date)
                        }

                        viewModel.messageList.add(
                            MessageModel("Sekarang pilih jurnal pada tanggal $date:", "model")
                        )
                    }
                }
            }

            // STEP 2 → pilih jurnal
            if (step == 2) {
                items(journalsByDate) { journal ->
                    OptionBubble(text = journal.title.ifBlank { "Tanpa Judul" }) {
                        viewModel.askTipsFromJournal(journal)
                    }
                }
            }
        }
    }
}

/* ===========================
          CHAT BUBBLE
   =========================== */

@Composable
fun ChatBubble(msg: MessageModel) {
    val isUser = msg.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isUser) Color(0xFF8B4CFC) else Color(0xFFEDEAFF),
                    RoundedCornerShape(14.dp)
                )
                .padding(12.dp)
                .widthIn(max = 260.dp)
        ) {
            Text(
                msg.message,
                color = if (isUser) Color.White else Color.Black
            )
        }
    }

    Spacer(Modifier.height(8.dp))
}

/* ===========================
         OPTION BUBBLE
   =========================== */

@Composable
fun OptionBubble(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFFEFF2FF), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(text, color = Color(0xFF4C5AFF))
    }
}
