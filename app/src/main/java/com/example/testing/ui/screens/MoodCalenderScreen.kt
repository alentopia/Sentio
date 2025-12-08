package com.example.testing.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.testing.JurnalModel
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MoodCalendarScreen(
    navController: NavController,
    userId: String
) {
    val today = LocalDate.now()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // FIRESTORE DATA
    var journalList by remember { mutableStateOf(listOf<JurnalModel>()) }

    LaunchedEffect(userId) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("journals")
            .addSnapshotListener { snapshot, _ ->

                if (snapshot != null) {
                    journalList = snapshot.documents.mapNotNull { doc ->
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
                }
            }
    }

    fun JurnalModel.toLocalDate(): LocalDate? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)
            LocalDateTime.parse(date, formatter).toLocalDate()
        } catch (e: Exception) {
            null
        }
    }

    val moodByDate = journalList
        .mapNotNull { journal ->
            journal.toLocalDate()?.let { date ->
                date to journal.mood
            }
        }
        .groupBy({ it.first }, { it.second })

    val moodColors = mapOf(
        "Angry" to Color(0xFFFF9AA2),
        "Fear" to Color(0xFFB39DDB),
        "Sad" to Color(0xFFA5D8FF),
        "Happy" to Color(0xFFFFE29A),
        "Surprise" to Color(0xFFFFC4E4),
        "Neutral" to Color(0xFFCFC9FF)
    )

    val moodEmojis = mapOf(
        "Angry" to "😡",
        "Fear" to "😨",
        "Sad" to "😢",
        "Happy" to "😊",
        "Surprise" to "😲",
        "Neutral" to "😐"
    )

    val moodSummary = journalList.groupingBy { it.mood }.eachCount()
    val mostCommonMood = moodSummary.maxByOrNull { it.value }?.key ?: "Neutral"


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        item {
            Text(
                "Record of Your Mood",
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                color = Color(0xFF4B3A64),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {

                    // Month Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }

                        Text(
                            "${
                                currentMonth.month.getDisplayName(
                                    TextStyle.FULL,
                                    Locale.getDefault()
                                )
                            } ${currentMonth.year}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )

                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Weekday Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                            Text(it, fontSize = 13.sp, color = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(12.dp))


                    // Animated month grid
                    AnimatedContent(
                        targetState = currentMonth,
                        transitionSpec = {
                            val isForward = targetState.atDay(1).isAfter(initialState.atDay(1))

                            (slideInHorizontally(
                                initialOffsetX = { fullWidth ->
                                    if (isForward) fullWidth else -fullWidth
                                },
                                animationSpec = tween(350)
                            ) + fadeIn()) with

                                    (slideOutHorizontally(
                                        targetOffsetX = { fullWidth ->
                                            if (isForward) -fullWidth else fullWidth
                                        },
                                        animationSpec = tween(350)
                                    ) + fadeOut())
                        }
                    ) { month ->

                        val days = month.lengthOfMonth()
                        val offset = (month.atDay(1).dayOfWeek.value - 1).coerceAtLeast(0)

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false // NO SCROLL → NO CRASH
                        ) {

                            items(offset) {
                                Spacer(Modifier.size(36.dp))
                            }

                            items((1..days).toList()) { day ->

                                val date = month.atDay(day)
                                val moodsToday = moodByDate[date]

                                val dominantMood = moodsToday
                                    ?.groupingBy { it }?.eachCount()
                                    ?.maxByOrNull { it.value }?.key

                                val bg = dominantMood?.let { moodColors[it] } ?: Color(0xFFF3F3F3)

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        fontWeight = if (date == today) FontWeight.Bold else FontWeight.Medium,
                                        color = if (date == today) Color(0xFF8B4CFC) else Color(
                                            0xFF4B3A64
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        "Mood Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF4B3A64)
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {

                        moodColors.keys.forEach { mood ->

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(moodColors[mood]!!),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(moodEmojis[mood] ?: "?", fontSize = 20.sp)
                                }

                                Text(
                                    (moodSummary[mood] ?: 0).toString(),
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )

                                Text(
                                    mood,
                                    fontSize = 12.sp,
                                    color = Color(0xFF4B3A64)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Mostly you feel ${moodEmojis[mostCommonMood]} $mostCommonMood",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4B3A64),
                        fontSize = 14.sp
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
            MoodStabilityCard(journalList)
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

fun calculateMoodStability(journals: List<JurnalModel>): Int {
    // Kalau belum ada data → tampilkan 0
    if (journals.isEmpty()) return 0

    val moodScoreMap = mapOf(
        "Angry" to 1,
        "Fear" to 2,
        "Sad" to 3,
        "Neutral" to 4,
        "Surprise" to 5,
        "Happy" to 6
    )

    // Urutkan berdasarkan tanggal
    val sorted = journals
        .mapNotNull { journal ->
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)
            try {
                LocalDateTime.parse(journal.date, formatter) to journal
            } catch (e: Exception) {
                null
            }
        }
        .sortedBy { it.first }

    // Kalau cuma 1 data → 100 (dianggap stabil)
    if (sorted.size < 2) return 100

    // Hitung selisih antar mood
    var totalChange = 0
    var count = 0

    for (i in 1 until sorted.size) {
        val prevMood = moodScoreMap[sorted[i - 1].second.mood] ?: continue
        val currMood = moodScoreMap[sorted[i].second.mood] ?: continue

        totalChange += kotlin.math.abs(currMood - prevMood)
        count++
    }

    if (count == 0) return 0

    // Semakin besar perubahan → nilai stabilitas semakin kecil
    val avgChange = totalChange.toFloat() / count

    // Konversi ke skor 0–100
    val stability = (100 - (avgChange * 15)).coerceIn(0f, 100f)

    return stability.toInt()
}

@Composable
fun MoodStabilityCard(journalList: List<JurnalModel>) {
    val stabilityScore = calculateMoodStability(journalList)
    val targetValue = stabilityScore
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(stabilityScore) {
        animatedValue.animateTo(
            targetValue.toFloat(),
            animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(130.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        val sweepAngle = (animatedValue.value / 100f) * 360f

                        drawArc(
                            color = Color(0xFFE2D2FF),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 14f, cap = StrokeCap.Round)
                        )

                        drawArc(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFB37CFF), Color(0xFF8B4CFC))
                            ),
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 14f, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = animatedValue.value.toInt().toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFF8B4CFC)
                        )
                        Text("/ 100", fontSize = 13.sp, color = Color(0xFF7D7A8B))
                    }
                }

                Text(
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF4B3A64))
                        ) { append("Mood Stability ") }
                        withStyle(style = SpanStyle(color = Color(0xFF6D5A85))) {
                            append("— The higher the score, the more stable you are.")
                        }
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}