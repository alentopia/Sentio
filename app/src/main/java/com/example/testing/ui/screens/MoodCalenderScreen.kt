package com.example.testing.ui.screens

import android.R.attr.label
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MoodCalendarScreen(navController: NavController) {
    val today = remember { LocalDate.now() }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1) // ini biar dia gak ada angka 0 karena calender itu calender sendiri bukan bawaan
    val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value - 1).coerceAtLeast(0) // ini tuh biar dia gak ada 0 nya gitu sih

    val moodColors = listOf(
        Color(0xFFFF9AA2), // Angry
        Color(0xFFB39DDB), // Fear
        Color(0xFFA5D8FF), // Sad
        Color(0xFFFFE29A), // Happy
        Color(0xFFFFC4E4), // Surprise
        Color(0xFFCFC9FF)  // Neutral
    )
    val emojis = listOf("😡", "😨", "😢", "😊", "😲", "😐")
    val moodLabels = listOf("Angry", "Fear", "Sad", "Happy", "Surprise", "Neutral")
    val moodCounts = listOf(60, 28, 55, 113, 86, 30)

    val randomJournalDays = remember(currentMonth) { (1..daysInMonth).shuffled().take(20) } //ini buat ngerandom mood colorsny

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Record of Your Mood",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5E4B9A),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))


            //  Calendar Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header bulan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Previous",
                                tint = Color(0xFF7A4CE0)
                            )
                        }

                        Text(
                            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF442D92)
                        )

                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Next",
                                tint = Color(0xFF7A4CE0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hari
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                            Text(it, color = Color(0xFF7D7A8B), fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    //  Grid Kalender
                    AnimatedContent(
                        targetState = currentMonth,
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInHorizontally(
                                initialOffsetX = { it * direction },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) + fadeIn()) togetherWith
                                    (slideOutHorizontally(
                                        targetOffsetX = { -it * direction },
                                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                                    ) + fadeOut())
                        },
                        label = "MonthChange"
                    ) { month ->
                        val days = month.lengthOfMonth()
                        val offset = (month.atDay(1).dayOfWeek.value - 1).coerceAtLeast(0)

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            userScrollEnabled = false
                        ) {
                            items(offset) { Spacer(modifier = Modifier.size(34.dp)) }

                            items((1..days).toList()) { day ->
                                val isToday = today.dayOfMonth == day && month == YearMonth.now()
                                val hasJournal = randomJournalDays.contains(day)
                                val moodColor = if (hasJournal) moodColors.random() else Color(0xFFF6F6F6)

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(moodColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        color = if (isToday) Color(0xFF7A4CE0)
                                        else Color(0xFF4B3A64),
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mood Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "Mood Summary",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF442D92),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        emojis.indices.forEach { index ->
                            val emoji = emojis[index]
                            val label = moodLabels[index]
                            val count = moodCounts[index]
                            val color = moodColors[index]

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 22.sp)
                                }
                                Text(count.toString(), fontSize = 13.sp, color = Color.Gray)
                                Text(label, fontSize = 12.sp, color = Color(0xFF4B3A64))
                            }
                        }
                    }


                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mostly you feel 😊 Happy! Keep glowing!",
                        color = Color(0xFF4B3A64),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
