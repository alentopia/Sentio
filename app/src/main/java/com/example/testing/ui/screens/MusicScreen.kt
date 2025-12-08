package com.example.spotifydemo.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.spotifydemo.data.PlaylistResponse
import com.example.spotifydemo.data.SpotifyRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MusicScreen(
    clientId: String,
    clientSecret: String
) {
    val repo = remember { SpotifyRepository(clientId, clientSecret) }

    var playlist by remember { mutableStateOf<PlaylistResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var latestMood by remember { mutableStateOf<String?>(null) }
    var currentPlaylistId by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val moodPlaylistIds = mapOf(
        "Neutral" to "04vFVSHKvDSOVC16f2QDtH",   // Neutral
        "Surprise" to "6CTs31rdlPd7SZJWd4m1Gd",  // Surprise
        "Disgust" to "3mLahxBhWNgfKmX2YnaypM",   // Disgust
        "Fear" to "335YJCOlnOcZVtrnZUn521",      // Fear
        "Angry" to "5Cq98eFWaKTojXeCIA3CkY",     // Angry
        "Happy" to "0F6UlQiIjv4X2NGEJ36BjT",     // Happy
        "Sad" to "4fuUUt3DU96IYx75opqt8d"        // Sad
    )


    val moodEmojis = mapOf(
        "Sad" to "😢",
        "Happy" to "😊",
        "Angry" to "😡",
        "Fear" to "😨",
        "Surprise" to "😲",
        "Neutral" to "😐"
    )

    // 🧠 1) Ambil mood terakhir user dari Firestore
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            latestMood = "Neutral"
            return@LaunchedEffect
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(uid)
            .collection("journals")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    latestMood = "Neutral"
                    return@addOnSuccessListener
                }

                val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH)

                var bestTime = 0L
                var bestMood = "Neutral"

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
                latestMood = "Neutral"
            }
    }

    // 🎶 2) Kalau latestMood sudah ketahuan → fetch playlist sesuai mood
    LaunchedEffect(latestMood) {
        val mood = latestMood ?: return@LaunchedEffect

        val moodKey = if (mood.isBlank()) "Neutral" else mood
        val playlistId = moodPlaylistIds[moodKey] ?: moodPlaylistIds["Neutral"]!!

        currentPlaylistId = playlistId
        loading = true
        error = null

        scope.launch {
            try {
                val result = repo.getPlaylistById(playlistId)
                playlist = result
            } catch (e: Exception) {
                e.printStackTrace()
                error = "Cannot load music for your mood 😢"
            } finally {
                loading = false
            }
        }
    }

    // 🌈 Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFEED3F2),
                        Color(0xFFD1E5FF)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        when {
            loading || latestMood == null -> {
                // Loading mood / playlist
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B4CFC))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Finding songs that match your feelings...",
                        color = Color(0xFF4B3A64),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = "✖",
                        fontSize = 80.sp,
                        color = Color(0xFFE74C3C)
                    )

                    Spacer(Modifier.height(12.dp))

                    // OOPS!!
                    Text(
                        text = "OOPS!!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B3A64),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    // Error message
                    Text(
                        text = "We couldn't load your mood playlist.\nPlease try again.",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    // Try Again Button
                    Button(
                        onClick = {
                            error = null
                            loading = true

                            scope.launch {
                                try {
                                    val result = repo.getPlaylistById(currentPlaylistId!!)
                                    playlist = result
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    error = "Still unable to load playlist 😢"
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B4CFC)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Try Again",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }


            playlist != null -> {
                val track = playlist!!.tracks.items.firstOrNull()?.track
                val moodLabel = latestMood ?: "Neutral"
                val moodEmoji = moodEmojis[moodLabel] ?: "🎧"

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mood text di atas
                    Text(
                        text = "You're feeling $moodLabel $moodEmoji",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF4B3A64),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                    )

                    Text(
                        text = "Here’s a playlist just for that mood.",
                        fontSize = 14.sp,
                        color = Color(0xFF7D7A8B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // MINI MUSIC PREVIEW
                    if (track != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(top = 8.dp, bottom = 10.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.6f))
                                .padding(20.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(track.album.images.firstOrNull()?.url),
                                contentDescription = track.name,
                                modifier = Modifier
                                    .size(220.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        val url =
                                            "https://open.spotify.com/search/${track.name} ${track.artists.firstOrNull()?.name ?: ""}"
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(url)
                                            )
                                        )
                                    }
                            )

                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = track.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF2A2A2A),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = track.artists.joinToString { it.name },
                                fontSize = 14.sp,
                                color = Color(0xFF8B4CFC),
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(16.dp))
                            TextButton(
                                onClick = {
                                    currentPlaylistId?.let { id ->
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://open.spotify.com/playlist/$id")
                                        )
                                        context.startActivity(intent)
                                    }
                                }
                            ) {
                                Text(
                                    text = "Open full playlist in Spotify",
                                    color = Color(0xFF8B4CFC),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Judul playlist
                    Text(
                        text = playlist!!.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Color(0xFF2A2A2A),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Divider(
                        color = Color(0xFFDDCFF9),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Daftar lagu (mulai dari track ke-2)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playlist!!.tracks.items.drop(1)) { item ->
                            val trackItem = item.track

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.85f))
                                    .clickable {
                                        val url =
                                            "https://open.spotify.com/search/${trackItem.name} ${trackItem.artists.firstOrNull()?.name ?: ""}"
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(url)
                                            )
                                        )
                                    }
                                    .padding(10.dp)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(trackItem.album.images.firstOrNull()?.url),
                                    contentDescription = trackItem.name,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = trackItem.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF222222)
                                    )
                                    Text(
                                        text = trackItem.artists.joinToString { it.name },
                                        fontSize = 13.sp,
                                        color = Color(0xFF8B4CFC)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
