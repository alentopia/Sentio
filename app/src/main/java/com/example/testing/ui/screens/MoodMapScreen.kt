package com.example.testing.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.testing.JurnalModel
import java.util.Locale

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MoodMapScreen(
    navController: NavController? = null,
    listJurnal: List<JurnalModel>
) {
    val context = LocalContext.current
    val geocoder = Geocoder(context, Locale.getDefault())

    // Default camera position
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(-6.256, 106.618), // UMN area
            14f
        )
    }

    var selectedJournal by remember { mutableStateOf<JurnalModel?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            listJurnal.forEach { journal ->

                if (journal.location.isBlank()) return@forEach

                val result = try {
                    geocoder.getFromLocationName(journal.location, 1)
                } catch (e: Exception) {
                    null
                }

                val address = result?.firstOrNull() ?: return@forEach

                val latLng = LatLng(address.latitude, address.longitude)

                Marker(
                    state = MarkerState(position = latLng),
                    title = "${journal.emoji} ${journal.mood}",
                    snippet = journal.title,
                    onClick = {
                        selectedJournal = journal
                        true
                    }
                )
            }
        }

        // BOTTOM POPUP CARD
        selectedJournal?.let { journal ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text(
                        text = "${journal.emoji}  ${journal.mood}",
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(journal.title, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(journal.date, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            navController?.navigate(
                                "journal_detail/${journal.title}/${journal.content}/${journal.date}/${journal.emoji}/${journal.location}"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(Color(0xFF8B4CFC))
                    ) {
                        Text("View Journal", color = Color.White)
                    }
                }
            }
        }
    }
}
