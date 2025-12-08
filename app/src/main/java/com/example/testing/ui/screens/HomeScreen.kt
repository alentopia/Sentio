package com.example.testing.ui.screens

import android.Manifest
import android.R.attr.progress
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.testing.JurnalModel
import com.example.testing.R
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun saveGoalToFirestore(goal: String, targetDays: Int) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    val data = mapOf(
        "goal" to goal,
        "targetDays" to targetDays,
        "daysCompleted" to 0,
        "isDone" to false,
        "startDate" to LocalDate.now().toString(),
        "totalGoalsCompleted" to 0

    )

    db.collection("users")
        .document(uid)
        .collection("weeklygoal")
        .document("current")
        .set(data)
}
fun incrementTotalGoals() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    val userDocRef = db.collection("users")
        .document(uid)
        .collection("weeklygoal")
        .document("current")

    db.runTransaction { transaction ->
        val snapshot = transaction.get(userDocRef)
        val currentTotal = snapshot.getLong("totalGoalsCompleted") ?: 0

        transaction.update(userDocRef, "totalGoalsCompleted", currentTotal + 1)
    }
}
fun resetWeeklyGoalInFirestore() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    val resetData = mapOf(
        "goal" to "",
        "targetDays" to 0,
        "daysCompleted" to 0,
        "isDone" to false
    )

    db.collection("users")
        .document(uid)
        .collection("weeklygoal")
        .document("current")
        .update(resetData)
}

fun saveGoalToHistory(goal: String, targetDays: Int, daysCompleted: Int) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    val historyData = mapOf(
        "goal" to goal,
        "targetDays" to targetDays,
        "daysCompleted" to daysCompleted,
        "completedAt" to LocalDate.now().toString()
    )

    db.collection("users")
        .document(uid)
        .collection("weeklygoal")
        .document("current")
        .collection("history")
        .add(historyData)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController? = null,
    saveJournal: (
        emoji: String,
        mood: String,
        title: String,
        content: String,
        location: String,
        date: String,
        onSuccess: () -> Unit
    ) -> Unit
) {

    val systemUiController = rememberSystemUiController()
    var showJournalSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    var latestMood by remember { mutableStateOf("") }

    val journals = remember { mutableStateListOf<JurnalModel>() }
    var userName by remember { mutableStateOf("User") }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()

    // WEEKLY GOAL STATE
    var goal by remember { mutableStateOf("") }
    var targetDays by remember { mutableStateOf(0) }
    var daysCompleted by remember { mutableStateOf(0) }
    var isDone by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Load nama user
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

    // Load journals
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
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
                }
            }
    }

    // Load weekly goal
    LaunchedEffect(currentUser) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        firestore.collection("users")
            .document(uid)
            .collection("weeklygoal")
            .document("current")
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    goal = doc.getString("goal") ?: ""
                    targetDays = doc.getLong("targetDays")?.toInt() ?: 0
                    daysCompleted = doc.getLong("daysCompleted")?.toInt() ?: 0
                    isDone = doc.getBoolean("isDone") ?: false
                }
            }
    }

    // 🧠 1) Ambil mood terakhir user dari Firestore
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            latestMood = ""
            return@LaunchedEffect
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(uid)
            .collection("journals")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    latestMood = ""
                    return@addOnSuccessListener
                }

                val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH)

                var bestTime = 0L
                var bestMood = ""

                snapshot.documents.forEach { doc ->
                    val dateStr = doc.getString("createdAt") ?: return@forEach
                    val mood = doc.getString("mood") ?: return@forEach

                    val parsedTime = try {
                        formatter.parse(dateStr)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }

                    if (parsedTime > bestTime) {
                        bestTime = parsedTime
                        bestMood = mood
                    }
                }

                latestMood = bestMood
            }
            .addOnFailureListener {
                latestMood = ""
            }
    }

    fun parseJournalDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null

        // format utama
        val formats = listOf(
            "dd MMM yyyy, HH:mm",
            "dd MMM yyyy HH:mm"
        )

        formats.forEach { pattern ->
            try {
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
                return LocalDateTime.parse(dateStr, formatter).toLocalDate()
            } catch (_: Exception) {
            }
        }

        return null
    }

    val today = LocalDate.now()

    val todaysJournal = journals
        .mapNotNull { journal ->
            val date = parseJournalDate(journal.date)
            if (date == today) journal else null
        }
        .maxByOrNull { journal ->
            parseJournalDate(journal.date)
                ?.toEpochDay()
                ?: Long.MIN_VALUE
        }

    // Snackbar
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFEED3F2), Color(0xFFD1E5FF))))
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            GreetingSection(userName)

            WeeklyGoalSection(
                goal = goal,
                daysCompleted = daysCompleted,
                targetDays = targetDays,
                isDone = isDone,
                onChooseGoal = { showGoalDialog = true },
                onMarkDayDone = {
                    val uid =
                        FirebaseAuth.getInstance().currentUser?.uid ?: return@WeeklyGoalSection
                    val db = FirebaseFirestore.getInstance()

                    val newDays = (daysCompleted + 1).coerceAtMost(targetDays)
                    val done = newDays >= targetDays

                    if (done) {
                        saveGoalToHistory(goal, targetDays, newDays)
                        incrementTotalGoals()
                    }

                    // update lokal biar UI langsung berubah
                    daysCompleted = newDays
                    isDone = done

                    // update Firestore
                    db.collection("users")
                        .document(uid)
                        .collection("weeklygoal")
                        .document("current")
                        .update(
                            mapOf(
                                "daysCompleted" to newDays,
                                "isDone" to done
                            )
                        )
                }
            )

            SpeciallyForYouSection(
                {
                    navController?.navigate("mood_picker")
                },
                onTips = { journalId ->
                    navController.navigate("tips/$journalId")
                }
            )

            ViewCalendarButton(onClick = {
                val uid = FirebaseAuth.getInstance().currentUser!!.uid
                navController?.navigate("mood_calendar/$uid")
            })

            LatestMoodSection(
                latestMood = latestMood,
                onStartJournal = {
                    navController?.navigate("mood_picker")
                }
            )
        }
        if (showGoalDialog) {
            GoalDialog(
                onSave = { selectedNum, allowNotif ->
                    saveGoalToFirestore(
                        goal = "Write a journal $selectedNum times this week",
                        targetDays = selectedNum
                    )

                    showGoalDialog = false
                },
                onDismiss = { showGoalDialog = false }
            )

        }
    }
}



@Composable
fun WeeklyGoalSection(
    goal: String,
    daysCompleted: Int,
    targetDays: Int,
    isDone: Boolean,
    onChooseGoal: () -> Unit,
    onMarkDayDone: () -> Unit
) {
    LaunchedEffect(isDone) {
        if (isDone) {
            delay(3000)
            resetWeeklyGoalInFirestore()
        }
    }

    when {
        goal.isBlank() -> {
            GoalStartCard(onChooseGoal)
        }

        !isDone -> {
            GoalProgressCard(
                goal = goal,
                daysCompleted = daysCompleted,
                targetDays = targetDays,
                isDone = isDone,
                onMarkDayDone = onMarkDayDone
            )
        }

        else -> {
            GoalProgressCard(
                goal = goal,
                daysCompleted = daysCompleted,
                targetDays = targetDays,
                isDone = isDone,
                onMarkDayDone = { }
            )
        }
    }
}
@Composable
fun GoalStartCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Image(
                painter = painterResource(id = R.drawable.blob_goal),
                contentDescription = "Blob",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(1600.dp)
                    .offset(x = (-20).dp, y = 0.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pick your goal for the week",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color(0xFF4A3A6A)
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text("Choose goal", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun GoalProgressCard(
    goal: String,
    daysCompleted: Int,
    targetDays: Int,
    isDone: Boolean,
    onMarkDayDone: () -> Unit
) {
    // ANIMATED PROGRESS
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(daysCompleted) {
        val newValue = if (targetDays == 0) 0f else daysCompleted.toFloat() / targetDays
        animatedProgress.animateTo(
            newValue,
            animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
        )
    }

    // ANIMATED NUMBER
    val animatedNumber = animateFloatAsState(
        targetValue = daysCompleted.toFloat(),
        animationSpec = tween(600),
        label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {
                if (!isDone) {
                    Text(
                        text = "You're making progress!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF4A3A6A)
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = goal,
                        fontSize = 14.sp,
                        color = Color(0xFF6D5A85)
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = onMarkDayDone,
                        enabled = daysCompleted < targetDays,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Mark today as done")
                    }
                } else {
                    Text(
                        text = "Congratulations!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF4A3A6A)
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "You completed this week's goal!",
                        fontSize = 14.sp,
                        color = Color(0xFF6D5A85)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onMarkDayDone,
                    enabled = daysCompleted < targetDays,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Mark today as done")
                }
            }

            // RIGHT – ANIMATED CIRCLE + NUMBER
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(90.dp)) {
                    // Background circle
                    drawArc(
                        color = Color(0xFFE2D2FF),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )

                    // Animated progress
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFFB37CFF), Color(0xFF8B4CFC))
                        ),
                        startAngle = -90f,
                        sweepAngle = animatedProgress.value * 360f,
                        useCenter = false,
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                }

                // Animated number
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = animatedNumber.value.toInt().toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A3A6A)
                    )
                    Text("days", fontSize = 12.sp, color = Color(0xFF6D5A85))
                }
            }
        }
    }
}

@Composable
fun GreetingSection(userName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Hey, $userName!",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF44345C)
        )

        val today = remember {
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(java.util.Date())
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = today,
            fontSize = 15.sp,
            color = Color(0xFF7D7A8B)
        )
    }
}

@Composable
fun GoalDialog(
    onSave: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedNumber by remember { mutableStateOf(1) }
    var allowNotif by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        ) {

            // CENTER CARD
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.85f),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        "Weekly Journal Goal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A3A6A)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "How many times would you like to journal this week?",
                        fontSize = 15.sp,
                        color = Color(0xFF5E4A7C),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    val animatedScale = remember { mutableStateOf(1f) }

                    LaunchedEffect(selectedNumber) {
                        animatedScale.value = 1.3f
                        kotlinx.coroutines.delay(120)
                        animatedScale.value = 1f
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedNumber > 1) selectedNumber--
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Minus",
                                tint = Color(0xFF8B4CFC),
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(Modifier.width(16.dp))


                        Text(
                            "$selectedNumber",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A3A6A),
                            modifier = Modifier.scale(animatedScale.value)
                        )

                        Spacer(Modifier.width(16.dp))

                        IconButton(
                            onClick = {
                                if (selectedNumber < 7) selectedNumber++
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Plus",
                                tint = Color(0xFF8B4CFC),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }

                        Spacer(Modifier.width(6.dp))

                        Button(
                            onClick = {
                                onSave(selectedNumber, allowNotif)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Goal")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LatestMoodSection(
    latestMood: String,
    onStartJournal: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ============================
            // CASE 1 → TIDAK ADA MOOD SAMA SEKALI
            // ============================
            if (latestMood.isBlank()) {

                Text(
                    text = "You haven't written any journal yet.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4B3A64),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Start your first one today! 💫",
                    fontSize = 14.sp,
                    color = Color(0xFF8B4CFC)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onStartJournal,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4CFC))
                ) {
                    Text("Write Journal", color = Color.White)
                }

                return@Column
            }

            Text(
                text = "Your current mood is:",
                fontSize = 15.sp,
                color = Color(0xFF6A5A95),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            val emoji = when (latestMood) {
                "Happy" -> "😊"
                "Sad" -> "😢"
                "Angry" -> "😡"
                "Fear" -> "😨"
                "Surprise" -> "😲"
                "Neutral" -> "😐"
                else -> "🙂"
            }

            Text(text = emoji, fontSize = 50.sp)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = latestMood,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A3AFF)
            )
        }
    }
}


@Composable
fun ViewCalendarButton(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = Color(0xFF8B4CFC),
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        "Mood Insights",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF4A3AFF)
                    )
                    Text(
                        "See your emotional journey",
                        fontSize = 13.sp,
                        color = Color(0xFF7D7A8B)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Go to Calendar",
                tint = Color(0xFF8B4CFC)
            )
        }
    }
}
@Composable
fun SpeciallyForYouSection(
    onQuickJournal: () -> Unit,
    onTips: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        Text(
            text = "Specially for you",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color(0xFF3A2C5F),
            modifier = Modifier.padding(start = 7.dp, bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // CARD 1 — Quick Journaling
            SpecialCard(
                title = "Quick Journal",
                emoji = "✍️",
                bgColor = Color(0xFFFFFFFF),
                onClick = onQuickJournal
            )

            // CARD 2 — Tips for you
            SpecialCard(
                title = "Tips for you",
                emoji = "💡",
                bgColor = Color(0xFFFFFFFF),
                onClick = onTips
            )
        }
    }
}
@Composable
fun SpecialCard(
    title: String,
    emoji: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = emoji,
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // TITLE
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF3A2C5F)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF8B4CFC)
                )
            }
        }
    }
}