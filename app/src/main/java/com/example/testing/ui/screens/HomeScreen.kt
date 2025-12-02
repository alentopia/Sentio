package com.example.testing.ui.screens

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController? = null,
    listJurnal: List<JurnalModel> = emptyList()
) {
    val scrollState = rememberScrollState()

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
            // Greeting
            GreetingSection()

            // Statistik (3 kotak)
            StatsSection()

            // Mood Stability
            MoodStabilityCard()

            // Tombol lihat kalender
            ViewCalendarButton(onClick = { navController?.navigate("mood_calendar") })
            // Latest Mood
            LatestMoodSection(listJurnal = listJurnal)
            // Peta mood
            //ViewMoodMapButton(onClick = { navController?.navigate("mood_map") })

        }
    }
}

@Composable
fun GreetingSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Hey, User!",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF44345C)
        )

        val today = remember {
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = today,
            fontSize = 15.sp,
            color = Color(0xFF7D7A8B)
        )
    }
}

// LATEST MOOD SECTION
@Composable
fun LatestMoodSection(listJurnal: List<JurnalModel>) {
    val latestJournal = listJurnal.lastOrNull()

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
            if (latestJournal == null) {
                // Belum ada mood
                Text(
                    text = "You haven’t written any journal yet.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4B3A64),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start your first one today! 💫",
                    fontSize = 14.sp,
                    color = Color(0xFF8B4CFC),
                    textAlign = TextAlign.Center
                )
            } else {
                // Sudah ada mood
                Text(
                    text = "Your current mood is:",
                    fontSize = 15.sp,
                    color = Color(0xFF6A5A95),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = latestJournal.emoji,
                    fontSize = 50.sp, // emoji besar
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = latestJournal.mood,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A3AFF)
                )
            }
        }
    }
}

// --- Statistik Section ---
@Composable
fun StatsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard("📔", "24", "Journals Written", Modifier.weight(1f))
        StatCard("🔥", "58", "Current streak", Modifier.weight(1f))
        StatCard("📅", "80", "Lifetime days", Modifier.weight(1f))
    }
}

@Composable
fun StatCard(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF44345C))
            Text(text = label, fontSize = 12.sp, color = Color(0xFF5E5E5E))
        }
    }
}

// Mood Stability Card
@Composable
fun MoodStabilityCard() {
    val targetValue = 64
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
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
                // Circular progress
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

                // Text
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
                Text(text = "🗓️", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "View Full Mood Calendar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF4A3AFF)
                    )
                    Text(
                        text = "See your emotional journey 💫",
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
fun ViewMoodMapButton(onClick: () -> Unit) {
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
                Text(text = "📍", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "View Mood Map",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF4A3AFF)
                    )
                    Text(
                        text = "See where your emotions happened 🌍",
                        fontSize = 13.sp,
                        color = Color(0xFF7D7A8B)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Go to Map",
                tint = Color(0xFF8B4CFC)
            )
        }
    }
}
