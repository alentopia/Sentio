package com.example.testing.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.testing.JurnalModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun SwipeRevealItem(
    revealWidth: Dp = 70.dp,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val revealPx = with(density) { revealWidth.toPx() }
    var offsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {

        // Tombol delete di kanan
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .width(revealWidth)
                    .fillMaxHeight()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFE04646),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Card yang digeser sedikit
        Box(
            modifier = Modifier
                .offset { IntOffset((-offsetX).toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = offsetX - dragAmount
                            offsetX = newOffset.coerceIn(0f, revealPx)
                        },
                        onDragEnd = {
                            offsetX =
                                if (offsetX > revealPx * 0.4f) revealPx else 0f
                        }
                    )
                }
        ) {
            content()
        }
    }
}

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
    var showMonthlyCalendar by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var searchQuery by remember { mutableStateOf("") }
    var sortType by remember { mutableStateOf("Newest") }
    var showSortDialog by remember { mutableStateOf(false) }
    var moodFilter by remember { mutableStateOf("All") }
    var isFirstLoadDone by remember { mutableStateOf(false) }
    val added = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<String>("added")
        ?.toBoolean() ?: false

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // buat ngeload data dari firestore
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(uid)
            .collection("journals")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    listJurnal.clear()

                    for (doc in snapshot.documents) {

                        val journal = JurnalModel(
                            id = doc.id,
                            emoji = doc.getString("emoji") ?: "",
                            mood = doc.getString("mood") ?: "",
                            title = doc.getString("title") ?: "",
                            content = doc.getString("content") ?: "",
                            location = doc.getString("location") ?: "",
                            date = doc.getString("createdAt") ?: "",
                            isEdited = doc.getBoolean("isEdited") ?: false
                        )

                        listJurnal.add(journal)
                    }
                    isFirstLoadDone = true
                }
            }
    }

    // Snackbar “added”
    LaunchedEffect(added, edited) {
        if (added && !edited) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Journal has been added")
            }
        }
    }

    //  Delete dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var journalToDelete by remember { mutableStateOf<JurnalModel?>(null) }

    fun deleteJournal(journal: JurnalModel) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(uid)
            .collection("journals")
            .document(journal.id)
            .delete()
            .addOnSuccessListener {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Your journal has been deleted")
                }
            }
            .addOnFailureListener { it.printStackTrace() }
    }

    //  Filter dan juga sort list
    val filteredList = listJurnal.filter { j ->

        //  Search Filter
        val matchesSearch =
            j.title.lowercase().contains(searchQuery.lowercase()) ||
                    j.content.lowercase().contains(searchQuery.lowercase()) ||
                    j.location.lowercase().contains(searchQuery.lowercase())

        //  Mood Filter
        val matchesMood =
            (moodFilter == "All") ||
                    j.mood.equals(moodFilter, ignoreCase = true)

        matchesSearch && matchesMood
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
                        .clickable { showMonthlyCalendar = !showMonthlyCalendar },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar Icon",
                        tint = Color(0xFF8B4CFC)
                    )
                }
            }

            // Calender
            if (showMonthlyCalendar) {
                MonthlyCalendarDialog(
                    onDateSelected = { selected ->
                        selectedDate = selected
                    },
                    onDismiss = {
                        showMonthlyCalendar = false
                    }
                )
            } else {
                CalendarJournal(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
            }


            // Bagian Search dan Sort
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
            //  Mood filter chip row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp)
            ) {
                val moods = listOf(
                    "All" to "All",
                    "Angry" to "😡",
                    "Fear" to "😨",
                    "Sad" to "😢",
                    "Happy" to "😊",
                    "Surprise" to "😲",
                    "Neutral" to "😐"
                )

                moods.forEach { (moodName, display) ->
                    val isSelected = moodFilter == moodName
                    //  Animasi scale
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        animationSpec = tween(durationMillis = 180),
                        label = ""
                    )

                    Surface(
                        shape = RoundedCornerShape(50), // FULL ROUND
                        color = if (isSelected) Color(0xFF8B4CFC) else Color.White,
                        border = BorderStroke(
                            width = if (isSelected) 0.dp else 1.dp,
                            color = Color(0xFFDCDCDC)
                        ),
                        shadowElevation = if (isSelected) 6.dp else 0.dp,
                        modifier = Modifier
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .clickable { moodFilter = moodName }
                    ) {
                        Text(
                            text = display,
                            fontSize = if (moodName == "All") 13.sp else 20.sp,
                            fontWeight = if (moodName == "All") FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF666666),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            val journalsForSelectedDate = sortedList.filter { j ->
                j.date.substringBefore(",") == selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            }

            // Journal list
            when {
                !isFirstLoadDone -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF8B4CFC))
                    }
                }

                journalsForSelectedDate.isEmpty() -> {

                    val message = when {
                        searchQuery.isNotEmpty() ->
                            "No results found."

                        selectedDate.isEqual(LocalDate.now()) ->
                            "No journals yet.\nTry writing one today!"

                        selectedDate.isBefore(LocalDate.now()) ->
                            "No journal was written on this day."

                        else -> "No journals found."
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = message,
                            color = Color(0xFF6A5ACD),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(journalsForSelectedDate) { index, journal ->

                            val isMoodOnly = journal.title.isBlank() && journal.content.isBlank()

                            if (isMoodOnly) {
                                SwipeRevealItem(
                                    onDelete = {
                                        journalToDelete = journal
                                        showDeleteDialog = true
                                    }
                                ) {
                                    MoodOnlyCard(journal)
                                }
                            } else {
                                SwipeRevealItem(
                                    onDelete = {
                                        journalToDelete = journal
                                        showDeleteDialog = true
                                    }
                                ) {
                                    JournalItemCard(
                                        journal = journal,
                                        onClick = {
                                            navController.navigate("journal_detail/${journal.id}")
                                        },
                                        onLongPress = {}
                                    )
                                }
                            }

                        }
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
                val isFutureWarning = message.contains("predict the future", ignoreCase = true)

                Surface(
                    //  warna beda kalau delete
                    color = if (isDelete) Color(0xFFFFE0E0).copy(alpha = 0.95f) else if (isFutureWarning) Color(
                        0xFFFFF4CC
                    ).copy(alpha = 0.95f)
                    else
                        Color(
                            0xFFE8F8F0
                        ).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = if (isDelete) Color(0xFFD32F2F) else if (isFutureWarning) Color(
                            0xFF8A6D3B
                        )
                        else Color(0xFF3C755F), // teks merah kalau delete
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
        if (showDeleteDialog && journalToDelete != null) {
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
                                "Are you sure you want to delete this?\nRemember that every feeling matters.",
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
                                        journalToDelete?.let { deleteJournal(it) }
                                        journalToDelete = null
                                        showDeleteDialog = false
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

    // 6 hari ke belakang + hari ini + 1 hari ke depan
    val startDate = today.minusDays(6)
    val endDate = today.plusDays(1)

    val visibleDates = remember(startDate) {
        (0..(endDate.toEpochDay() - startDate.toEpochDay())).map {
            startDate.plusDays(it)
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        val listState = rememberLazyListState()

        LaunchedEffect(Unit) {
            // scroll otomatis ke hari ini
            val todayIndex = visibleDates.indexOf(LocalDate.now())
            if (todayIndex != -1) {
                listState.scrollToItem(todayIndex)
            }
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        ) {
            items(visibleDates) { date ->

                val isSelected = date == selectedDate
                val isToday = date == today
                val isFuture = date.isAfter(today)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isSelected -> Color(0xFF8B4CFC)
                                isToday -> Color(0xFFE7DBFF)
                                else -> Color.Transparent
                            }
                        )
                        .clickable(enabled = !isFuture) { onDateSelected(date) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = when {
                            isSelected -> Color.White
                            isFuture -> Color.LightGray
                            else -> Color(0xFF2F195F)
                        },
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        color = when {
                            isSelected -> Color.White
                            isFuture -> Color.LightGray
                            else -> Color(0xFF6A6A8E)
                        },
                        fontSize = 12.sp
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
    val timeOnly = journal.date.substringAfter(", ").trim()

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
                        text = timeOnly,
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
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyCalendarDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val today = LocalDate.now()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        ) {

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .width(330.dp)
                        .wrapContentHeight(),
                    color = Color.White
                ) {

                    val state = rememberDatePickerState()

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        DatePicker(
                            state = state,
                            title = null,
                            headline = null,
                            showModeToggle = false
                        )

                        // TRIGGER WARNING
                        LaunchedEffect(state.selectedDateMillis) {
                            val millis = state.selectedDateMillis ?: return@LaunchedEffect
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()

                            if (date.isAfter(today)) {
                                snackbarHostState.showSnackbar(
                                    "You cannot predict the future, aren't you?"
                                )
                            } else {
                                onDateSelected(date)
                                onDismiss()
                            }
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp),
                snackbar = { snackbarData ->

                    Surface(
                        color = Color(0xFFFFF4CC).copy(alpha = 0.95f),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = snackbarData.visuals.message,
                            modifier = Modifier.padding(18.dp),
                            color = Color(0xFF7A5F1A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
        }
    }
}
@Composable
fun MoodOnlyCard(journal: JurnalModel) {

    val timeOnly = journal.date.substringAfter(", ").trim()

    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Emoji box
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFEDE4FF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(journal.emoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = journal.mood,
                    color = Color(0xFF000000),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Mood tracked — no journal written",
                    color = Color(0xFF777777),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Time
            Text(
                text = timeOnly,
                color = Color(0xFF9B8FBA),
                fontSize = 12.sp
            )
        }
    }
}


