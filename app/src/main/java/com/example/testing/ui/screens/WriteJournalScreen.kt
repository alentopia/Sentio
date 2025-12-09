package com.example.testing.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume


@Composable
fun WriteJournalScreen(
    emoji: String,
    mood: String,
    date: String,
    onSave: (String, String, String, String) -> Unit,
    onSkip: (String) -> Unit,
    navController: NavController? = null
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    // Input fields
    var journalTitle by remember { mutableStateOf("") }
    var journalText by remember { mutableStateOf("") }
    var manualLocation by remember { mutableStateOf("") }
    var isFetchingLocation by remember { mutableStateOf(false) }

    LaunchedEffect(navController) {
        navController?.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<String>("selected_location")
            ?.observeForever { location ->
                manualLocation = location
            }
    }


    // Error states
    var titleError by remember { mutableStateOf(false) }
    var contentError by remember { mutableStateOf(false) }

    val currentDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

    // Location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) manualLocation = "Location permission not granted"
        }
    )


    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    navController?.navigate("mood_picker") {
                        popUpTo("mood_picker") { inclusive = true }
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }

                IconButton(onClick = {
                    navController?.navigate("journal_list") {
                        popUpTo("journal_list") { inclusive = true }
                    }
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                }
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {

                Text(text = emoji, fontSize = 48.sp)
                Text(
                    text = "I feel $mood",
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(date, color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(20.dp))

                // TITLE FIELD
                OutlinedTextField(
                    value = journalTitle,
                    onValueChange = {
                        journalTitle = it
                        titleError = false
                    },
                    placeholder = { Text("Write a title...", color = Color.Gray) },
                    singleLine = true,
                    isError = titleError,
                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = if (titleError) Color.Red else Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                )

                if (titleError) {
                    Text(
                        text = "Title must be filled",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CONTENT FIELD
                OutlinedTextField(
                    value = journalText,
                    onValueChange = {
                        journalText = it
                        contentError = false
                    },
                    placeholder = { Text("Write your thoughts here...", color = Color.Gray) },
                    textStyle = TextStyle(fontSize = 16.sp),
                    isError = contentError,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.White, RoundedCornerShape(16.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = if (contentError) Color.Red else Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                )

                if (contentError) {
                    Text(
                        text = "Content must be filled",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // LOCATION FIELD
                OutlinedTextField(
                    value = manualLocation,
                    onValueChange = { manualLocation = it },
                    placeholder = { Text("Enter Location", color = Color.Gray) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val permissionCheck = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                )
                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                    coroutineScope.launch {
                                        isFetchingLocation = true
                                        getCurrentLocation(context, fusedLocationClient) {
                                            manualLocation = it
                                        }
                                        isFetchingLocation = false
                                    }
                                } else {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            }
                        ) {
                            if (isFetchingLocation) {
                                CircularProgressIndicator(
                                    color = Color(0xFF8B4CFC),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Detect location",
                                    tint = Color(0xFF8B4CFC)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // SAVE BUTTON
                Button(
                    onClick = {
                        titleError = journalTitle.isBlank()
                        contentError = journalText.isBlank()

                        if (titleError || contentError) return@Button

                        val finalLocation =
                            if (manualLocation.isNotBlank()) manualLocation else "Unknown location"

                        onSave(journalTitle, journalText, finalLocation, currentDate)

                        navController?.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("added", true)

                        navController?.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4CFC)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Text("Save", color = Color.White, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CONTINUE WITHOUT SAVING
                Text(
                    text = "Continue Without Saving",
                    color = Color(0xFF8B4CFC),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        onSkip(currentDate)
                    }
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(
    context: android.content.Context,
    fusedClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocationFound: (String) -> Unit
) {
    suspendCancellableCoroutine<Unit> { cont ->
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses =
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val address = addresses?.firstOrNull()?.getAddressLine(0)
                    onLocationFound(address ?: "Lat: ${location.latitude}, Lng: ${location.longitude}")
                } else {
                    onLocationFound("Unable to get location")
                }
                cont.resume(Unit)
            }
            .addOnFailureListener {
                onLocationFound("Error getting location")
                cont.resume(Unit)
            }
    }
}
