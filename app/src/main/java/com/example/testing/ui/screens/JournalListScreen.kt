package com.example.testing.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.testing.JurnalModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun JournalListScreen(
    navController: NavController,
    listJurnal: MutableList<JurnalModel> = remember { mutableStateListOf() },
    onAddClick: () -> Unit = {},
    onDelete: (Int) -> Unit = {},
    added: Boolean = false,
    edited: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var searchQuery by remember { mutableStateOf("") }
    var sortType by remember { mutableStateOf("Newest") }
    var showSortDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(added, edited) {
        if (added && !edited) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Journal has been added")
            }
        }
    }

    //  Delete dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedDeleteIndex by remember { mutableStateOf(-1) }

    //  FILTER + SORT LIST
    val filteredList = listJurnal.filter { j ->
        j.title.lowercase().startsWith(searchQuery.lowercase()) ||
                j.content.lowercase().startsWith(searchQuery.lowercase()) ||
                j.mood.lowercase().startsWith(searchQuery.lowercase()) ||
                j.location.lowercase().startsWith(searchQuery.lowercase())
    }


    val sortedList = when (sortType) {
        "Newest" -> filteredList.sortedByDescending { it.date }
        "Oldest" -> filteredList.sortedBy { it.date }
        "A-Z" -> filteredList.sortedBy { it.title.lowercase() }
        "Z-A" -> filteredList.sortedByDescending { it.title.lowercase() }
        else -> filteredList
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
        ) {

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Journal",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF442D92)
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { navController.navigate("mood_calendar") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar Icon",
                        tint = Color(0xFF8B4CFC)
                    )
                }
            }

            // WEEK CALENDAR
            CalendarJournal(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it }
            )

            // SEARCH + SORT BUTTON
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search", color = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Sort Journals",
                                tint = Color(0xFF8B4CFC)
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {

                            DropdownMenuItem(
                                text = { Text("Newest") },
                                onClick = {
                                    sortType = "Newest"
                                    expanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Oldest") },
                                onClick = {
                                    sortType = "Oldest"
                                    expanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("A-Z") },
                                onClick = {
                                    sortType = "A-Z"
                                    expanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Z-A") },
                                onClick = {
                                    sortType = "Z-A"
                                    expanded = false
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF8B4CFC),
                    unfocusedBorderColor = Color(0xFFDADADA)
                )
            )

            // JOURNAL LIST
            if (sortedList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No journals found.\nTry writing one today! ✨",
                        color = Color(0xFF6A5ACD),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(sortedList) { index, journal ->
                        JournalItemCard(
                            journal = journal,
                            onClick = {
                                navController.navigate(
                                    "journal_detail/${journal.title}/${journal.content}/${journal.date}/${journal.emoji}/${journal.location}"
                                )
                            },
                            onLongPress = {
                                selectedDeleteIndex = index
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Floating Button
        FloatingActionButton(
            onClick = onAddClick,
            containerColor = Color(0xFF8B4CFC),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Journal",
                tint = Color.White
            )
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp),
            snackbar = { snackbarData ->
                val message = snackbarData.visuals.message
                val isDelete = message.contains("deleted", ignoreCase = true)

                Surface(
                    //  warna beda kalau delete
                    color = if (isDelete) Color(0xFFFFE0E0).copy(alpha = 0.95f) else Color(0xFFE8F8F0).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = if (isDelete) Color(0xFFD32F2F) else Color(0xFF3C755F), // teks merah kalau delete
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        )

        // SORT DIALOG
        if (showSortDialog) {
            AlertDialog(
                onDismissRequest = { showSortDialog = false },
                confirmButton = {},
                text = {
                    Column {
                        Text("Sort By", fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(12.dp))

                        val options = listOf("Newest", "Oldest", "A-Z", "Z-A")

                        options.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        sortType = option
                                        showSortDialog = false
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = sortType == option,
                                    onClick = {
                                        sortType = option
                                        showSortDialog = false
                                    }
                                )
                                Text(option, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            )
        }

        // DELETE DIALOG
        if (showDeleteDialog && selectedDeleteIndex != -1) {
            Dialog(
                onDismissRequest = { showDeleteDialog = false },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Delete Journal?",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A4458),
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Are you sure you want to delete this?\nRemember — every feeling matters. 💜",
                                color = Color(0xFF666666),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TextButton(
                                    onClick = { showDeleteDialog = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", color = Color.Gray)
                                }

                                Button(
                                    onClick = {
                                        val index = selectedDeleteIndex
                                        showDeleteDialog = false
                                        if (index in sortedList.indices) {
                                            val realIndex =
                                                listJurnal.indexOf(sortedList[index])
                                            if (realIndex != -1) onDelete(realIndex)
                                        }

                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Your journal has been deleted")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF8B4CFC)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Delete", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CalendarJournal(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    var currentStart by remember { mutableStateOf(today.minusDays(3)) }

    val visibleDates = remember(currentStart) {
        (0..13).map { currentStart.plusDays(it.toLong()) }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(visibleDates) { date: LocalDate ->
                val isSelected = date == selectedDate
                val isToday = date == today
                val dayNumber = date.dayOfMonth
                val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isSelected -> Color(0xFF8B4CFC)
                                isToday -> Color(0xFFE7DBFF)
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onDateSelected(date) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = dayNumber.toString(),
                        color = if (isSelected) Color.White else Color(0xFF2F195F),
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = dayName,
                        color = if (isSelected) Color.White else Color(0xFF6A6A8E),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalItemCard(
    journal: JurnalModel,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val tipsList = listOf(
        "Connect with nature" to "Spend time outdoors, surrounded by greenery and fresh air",
        "Take deep breaths" to "Pause for a moment and breathe deeply to clear your mind",
        "Write it down" to "Jot your thoughts to understand your emotions better",
        "Stay hydrated" to "Drink a glass of water and give yourself a quick reset",
        "Move your body" to "A short walk or stretch can improve your mood instantly"
    )
    val randomTip = remember { tipsList.random() }

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
                indication = rememberRipple(color = Color(0xFF8B4CFC)),
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFF4EEFF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = journal.emoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (journal.title.isNotBlank()) journal.title else "No Title",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333),
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (journal.content.isNotBlank()) {
                                val preview = journal.content.take(15)
                                preview + if (journal.content.length > 15) "..." else ""
                            } else {
                                "No Content"
                            },
                            color = Color(0xFF777777),
                            fontSize = 13.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = journal.date,
                        color = Color(0xFF999999),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // Label kecil "Edited"
                    if (journal.isEdited == true) {
                        Text(
                            text = "Edited",
                            color = Color(0xFF9E9E9E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF0E6FF), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "💡 Tip",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8B4CFC),
                fontSize = 14.sp
            )
            Text(
                text = randomTip.first,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = randomTip.second,
                color = Color(0xFF777777),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }
    }
}
