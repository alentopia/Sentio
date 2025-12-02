package com.example.testing.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.testing.ml.TFLiteMoodClassifier
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


// ========================================================
// CAMERA PERMISSION HELPER
// ========================================================
fun requestCameraPermission(context: android.content.Context, activity: Activity): Boolean {
    return if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            1001
        )
        false
    } else {
        true
    }
}


// MOOD PICKER SCREEN
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MoodPickerScreen(
    navController: NavController? = null,
    onContinue: (String, String) -> Unit
) {
    val moods = listOf("Angry", "Fear", "Sad", "Happy", "Surprise", "Neutral")

    val emojis = listOf("😡", "😨", "😢", "😊", "😲", "😐")
    val colors = listOf(
        Color(0xFFFF9AA2), // Angry
        Color(0xFFB39DDB), // Fear
        Color(0xFFA5D8FF), // Sad
        Color(0xFFFFE29A), // Happy
        Color(0xFFFFC4E4), // Surprise
        Color(0xFFCFC9FF)  // Neutral
    )

    fun Float.toRad(): Float = (this * (PI / 180f)).toFloat()

    val sweep = 360f / moods.size
    val rotation = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    var selectedIndex by remember { mutableStateOf(2) }
    var showCameraPopup by remember { mutableStateOf(false) }
    var lastAngle by remember { mutableStateOf(0f) }

    val context = LocalContext.current
    val activity = context as Activity


    // UI MAIN
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {

        // Back Button
        IconButton(
            onClick = { navController?.navigate("journal_list") },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF8B4CFC))
        }


        // Wheel Gesture
        val gestureModifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    lastAngle = atan2(
                        offset.y - size.height / 2,
                        offset.x - size.width / 2
                    ) * (180f / PI).toFloat()
                },
                onDrag = { change, _ ->
                    val currentAngle = atan2(
                        change.position.y - size.height / 2,
                        change.position.x - size.width / 2
                    ) * (180f / PI).toFloat()
                    val delta = currentAngle - lastAngle
                    lastAngle = currentAngle
                    coroutineScope.launch {
                        rotation.snapTo(rotation.value + delta)
                    }
                },
                onDragEnd = {
                    coroutineScope.launch {
                        val normalized = ((rotation.value % 360) + 360) % 360
                        val index = ((normalized + sweep / 2) / sweep).toInt() % moods.size
                        selectedIndex = (moods.size - index) % moods.size
                    }
                }
            )
        }


        // Mood Wheel
        Canvas(
            modifier = Modifier
                .size(310.dp)
                .then(gestureModifier)
                .shadow(12.dp, CircleShape)
        ) {
            val radius = size.minDimension / 2.3f
            var startAngle = rotation.value - 90f

            moods.forEachIndexed { index, _ ->
                drawArc(
                    color = colors[index],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    style = Fill
                )

                val middle = startAngle + sweep / 2
                val rad = middle.toRad()

                val x = size.width / 2 + cos(rad) * radius * 0.7f
                val y = size.height / 2 + sin(rad) * radius * 0.7f

                drawContext.canvas.nativeCanvas.drawText(
                    emojis[index],
                    x,
                    y,
                    android.graphics.Paint().apply {
                        textSize = 65f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                )

                startAngle += sweep
            }
        }


        // Pointer
        Canvas(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.Center)
        ) {
            val cx = size.width / 2
            val ty = size.height / 2 - (size.minDimension / 2.15f) - 45f

            val path = Path().apply {
                moveTo(cx - 20f, ty)
                arcTo(Rect(cx - 20f, ty - 20f, cx + 20f, ty + 20f), 180f, 180f, false)
                lineTo(cx, ty + 50f)
                close()
            }
            drawPath(path, Color(0xFF8B4CFC))
        }


        // Mood text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            Text(text = emojis[selectedIndex], fontSize = 60.sp)
            Text(
                text = "You feel ${moods[selectedIndex]} today!",
                fontSize = 18.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
        }


        // Bottom Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            Button(
                onClick = {
                    if (requestCameraPermission(context, activity)) {
                        showCameraPopup = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(50.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF8B4CFC), Color(0xFFD58FFF))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Text("Scan Your Mood", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onContinue(emojis[selectedIndex], moods[selectedIndex]) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4CFC)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(50.dp)
            ) {
                Text("Continue ➜", color = Color.White)
            }
        }
    }



    // CAMERA POPUP
    CameraPopup(
        showCameraPopup = showCameraPopup,
        onClose = { showCameraPopup = false },
        onImageCaptured = { bitmap ->
            val classifier = TFLiteMoodClassifier(context)

            val detectedIndex = classifier.predict(bitmap)
            selectedIndex = detectedIndex

            coroutineScope.launch {
                rotation.animateTo(-detectedIndex * sweep, tween(900))
            }

            showCameraPopup = false
        }
    )
}

// CAMERA POPUP COMPOSABLE
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CameraPopup(
    showCameraPopup: Boolean,
    onClose: () -> Unit,
    onImageCaptured: (Bitmap) -> Unit
) {
    if (!showCameraPopup) return

    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(420.dp),
            shape = RoundedCornerShape(24.dp)
        ) {

            Box(modifier = Modifier.fillMaxSize()) {

                AndroidView(
                    factory = { context ->
                        val previewView = PreviewView(context)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageCapture = ImageCapture.Builder()
                                .setTargetRotation(previewView.display.rotation)
                                .build()

                            cameraProvider.unbindAll()

                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                preview,
                                imageCapture
                            )

                            previewView.setOnClickListener {
                                val output = ImageCapture.OutputFileOptions
                                    .Builder(context.cacheDir.resolve("photo.jpg"))
                                    .build()

                                imageCapture.takePicture(
                                    output,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                            val bmp = BitmapFactory.decodeFile(
                                                context.cacheDir.resolve("photo.jpg").path
                                            )
                                            onImageCaptured(bmp)
                                        }

                                        override fun onError(exc: ImageCaptureException) {
                                            exc.printStackTrace()
                                        }
                                    }
                                )
                            }

                        }, ContextCompat.getMainExecutor(context))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Close button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
