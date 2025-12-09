package com.example.testing.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testing.JurnalModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class TipsChatViewModel : ViewModel() {

    val messageList = mutableStateListOf<MessageModel>()

    var step by mutableStateOf(1)

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var selectedDate: String? = null
    var selectedJournal: JurnalModel? = null

    init {
        addBotMessage("Silakan pilih tanggal jurnal yang ingin kamu mintai tips")
    }

    fun addBotMessage(msg: String) {
        messageList.add(MessageModel(message = msg, role = "model"))
    }

    fun addUserMessage(msg: String) {
        messageList.add(MessageModel(message = msg, role = "user"))
    }

    fun resetToDateStep() {
        step = 1
        messageList.clear()
        addBotMessage("Silakan pilih tanggal jurnal yang ingin kamu mintai tips")
    }
    fun saveToHistory(text: String, role: String) {
        val dateKey = selectedDate ?: return
        if (uid.isEmpty()) return

        val data = mapOf(
            "message" to text,
            "role" to role,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("tipsHistory")
            .document(dateKey)
            .collection("messages")
            .add(data)
    }

    fun askAIWithJournal(journal: JurnalModel) {
        selectedJournal = journal

        val prompt = """
            Judul: ${journal.title}
            Mood: ${journal.mood}
            Isi jurnal: ${journal.content}

            Buatkan tips singkat, empatik, lembut, dan actionable.
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

        docRef.addSnapshotListener { snap, _ ->
            if (snap != null && snap.contains("response")) {
                val result = snap.getString("response") ?: "No response"

                // remove typing bubble
                if (messageList.isNotEmpty() && messageList.last().message == "Typing...") {
                    messageList.removeAt(messageList.lastIndex)
                }

                messageList.add(MessageModel(result, "model"))
                saveToHistory(result, "model")

                step = 3
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
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val listState = rememberLazyListState()

    var fullList by remember { mutableStateOf(listOf<JurnalModel>()) }
    var dateList by remember { mutableStateOf(listOf<String>()) }
    var journalsByDate by remember { mutableStateOf(listOf<JurnalModel>()) }

    val isOnline = rememberInternetStatus()

    LaunchedEffect(viewModel.messageList.size) {
        delay(100)
        if (viewModel.messageList.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messageList.lastIndex)
        }
    }
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("journals")
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.map {
                    JurnalModel(
                        id = it.id,
                        emoji = it.getString("emoji") ?: "",
                        mood = it.getString("mood") ?: "",
                        title = it.getString("title") ?: "",
                        content = it.getString("content") ?: "",
                        location = it.getString("location") ?: "",
                        date = it.getString("createdAt") ?: "",
                        isEdited = it.getBoolean("isEdited") ?: false
                    )
                }

                fullList = list
                dateList = list.map { it.date.substringBefore(",") }
                    .distinct()
                    .sortedDescending()
            }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Text(
                "AI Tips",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        /* INTERNET BANNER */
        if (!isOnline) {
            WarningBanner(
                "You're offline. History is available, but AI tips need internet."
            )
        } else {
            InfoBanner("AI Tips require an active internet connection.")
        }

        Spacer(Modifier.height(10.dp))

        /* CHAT LIST */
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState
        ) {
            items(viewModel.messageList) { msg ->
                if (msg.message == "Typing...") TypingBubble()
                else ChatBubble(msg)
            }
        }

        Spacer(Modifier.height(12.dp))

        /* OPTIONS */
        OptionsSection(
            step = viewModel.step,
            dateList = dateList,
            journalListByDate = journalsByDate,
            isOnline = isOnline,
            onDateSelect = { date ->
                journalsByDate = fullList.filter { it.date.startsWith(date) }
                viewModel.addUserMessage(date)
                viewModel.addBotMessage("Pilih jurnal pada tanggal $date:")
                viewModel.selectedDate = date
                viewModel.step = 2
            },
            onJournalSelect = { j ->
                if (isOnline) {
                    val title = j.title.ifBlank { "Tanpa Judul" }

                    viewModel.addUserMessage(title)
                    viewModel.saveToHistory(title, "user")
                    viewModel.askAIWithJournal(j)

                } else {
                    viewModel.addBotMessage("Cannot generate new tips while offline.")
                }
            },
            onReset = { viewModel.resetToDateStep() }
        )
    }
}


@Composable
fun ChatBubble(msg: MessageModel) {
    val isUser = msg.role == "user"

    val background = if (isUser) Color(0xFF7B4BFF) else Color(0xFFF2F0FF)
    val textColor = if (isUser) Color.White else Color.Black

    val appearAlpha by animateFloatAsState(1f, tween(250))
    val appearOffset by animateDpAsState(0.dp, tween(250))

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    alpha = appearAlpha
                    translationY = appearOffset.toPx()
                }
                .background(background, RoundedCornerShape(20.dp))
                .padding(14.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(msg.message, color = textColor)
        }
    }
}


@Composable
fun TypingBubble() {
    var frame by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            frame = (frame + 1) % 3
            delay(300)
        }
    }

    val dots = listOf(".", " ..", "...")

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            Modifier
                .background(Color(0xFFF2F0FF), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Text(dots[frame], color = Color(0xFF6A4DE0))
        }
    }
}

@Composable
fun OptionBubble(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(
            text,
            color = Color(0xFF0C0C0C),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun InfoBanner(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFDDE7FF), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color(0xFF204FDF))
    }
}

@Composable
fun WarningBanner(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE2E2), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color(0xFFB71C1C))
    }
}


@Composable
fun BackOptionBubble(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFFFFEEDB), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(
            text,
            color = Color(0xFFD6811A),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


@Composable
fun OptionsSection(
    step: Int,
    dateList: List<String>,
    journalListByDate: List<JurnalModel>,
    isOnline: Boolean,
    onDateSelect: (String) -> Unit,
    onJournalSelect: (JurnalModel) -> Unit,
    onReset: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8FF), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {

        if (!isOnline) {
            Text(
                "You’re offline. You can only view previous AI Tips history.",
                color = Color(0xFFB71C1C),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            BackOptionBubble("Retry") { onReset() }
            return   // ← sangat penting
        }

        when (step) {

            1 -> {
                dateList.forEach { date ->
                    OptionBubble(date) { onDateSelect(date) }
                }
            }

            2 -> {
                journalListByDate.forEach { j ->
                    OptionBubble(j.title.ifBlank { "Tanpa Judul" }) {
                        onJournalSelect(j)
                    }
                }
                BackOptionBubble("Kembali ke tanggal") { onReset() }
            }

            3 -> {
                BackOptionBubble("Coba pertanyaan lain") { onReset() }
            }
        }
    }
}

@Composable
fun rememberInternetStatus(): Boolean {
    val context = LocalContext.current
    val connectivityManager =
        context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager

    var isOnline by remember { mutableStateOf(false) }

    // Register callback with proper lifecycle
    DisposableEffect(Unit) {

        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                isOnline = true
            }
            override fun onLost(network: android.net.Network) {
                isOnline = false
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    return isOnline
}
