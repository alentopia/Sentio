package com.example.testing.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.spotifydemo.ui.MusicScreen
import com.example.testing.JurnalModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainNavHost() {

    val navController = rememberNavController()
    val listJurnal = remember { mutableStateListOf<JurnalModel>() }

    //  FIREBASE FUNCTIONS
    fun saveJournalToFirestore(
        emoji: String,
        mood: String,
        title: String,
        content: String,
        location: String,
        date: String,
        onSuccess: () -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val data = hashMapOf(
            "emoji" to emoji,
            "mood" to mood,
            "title" to title,
            "content" to content,
            "location" to location,
            "createdAt" to date
        )

        db.collection("users")
            .document(uid)
            .collection("journals")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { it.printStackTrace() }
    }

    fun deleteJournalFromFirestore(journalId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(uid)
            .collection("journals")
            .document(journalId)
            .delete()
    }

    // SYSTEM UI CONTROLLER
    val systemUiController = rememberSystemUiController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val isAuthScreen = currentRoute?.startsWith("signin") == true ||
            currentRoute?.startsWith("signup") == true ||
            currentRoute?.startsWith("forgotpassword") == true

    SideEffect {
        if (isAuthScreen) {
            systemUiController.setStatusBarColor(Color(0xFF7E63FF), darkIcons = false)
            systemUiController.setNavigationBarColor(Color.White, darkIcons = true)
        } else {
            systemUiController.setStatusBarColor(Color.Transparent, darkIcons = true)
            systemUiController.setNavigationBarColor(Color.Transparent, darkIcons = true)
        }
    }

    val gradientColors = if (isAuthScreen) {
        listOf(Color(0xFF7E63FF), Color.White)
    } else {
        listOf(Color(0xFFEED3F2), Color(0xFFD1E5FF))
    }

    // UI ROOT
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradientColors))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val showBottomNav =
                    currentRoute?.startsWith("journal_list") == true ||
                            currentRoute?.startsWith("profile") == true ||
                            currentRoute?.startsWith("home") == true ||
                            currentRoute?.startsWith("music") == true ||
                            currentRoute?.startsWith("mood_calendar") == true
                if (showBottomNav) BottomNavBar(navController)
            }
        ) { innerPadding ->

            NavHost(
                navController = navController,
                startDestination = "splash",
                modifier = Modifier.padding(innerPadding)
            ) {

                // AUTH SCREENS
                composable("splash") { SplashScreen(navController) }

                composable(
                    "signin?created={created}",
                    arguments = listOf(navArgument("created") { defaultValue = "false" })
                ) { back ->
                    val created = back.arguments?.getString("created")?.toBoolean() ?: false
                    SignInScreen(navController, created)
                }

                composable("signup") { SignUpScreen(navController) }
                composable("forgotpassword") { ForgotPasswordScreen(navController) }

                composable("home") {
                    HomeScreen(
                        navController = navController,
                        saveJournal = ::saveJournalToFirestore
                    )
                }
                // TIPS SCREEN
                composable("tips") {
                    TipsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("music") {
                    MusicScreen(
                        clientId = "9ace244d481f48d5b34acea812017a62",
                        clientSecret = "6bf846d587384642ae370f0700876276"
                    )
                }

                composable(
                    "profile?edited={edited}",
                    arguments = listOf(navArgument("edited") { defaultValue = "false" })
                ) { back ->
                    val edited = back.arguments?.getString("edited")?.toBoolean() ?: false
                    ProfileScreen(navController, edited)
                }

                composable("edit_profile") { EditProfileScreen(navController) }
                composable("help_support") { HelpSupportScreen(navController) }

                // --------------------------------------------------
                // JOURNAL LIST
                // --------------------------------------------------
                composable(
                    "journal_list?added={added}&edited={edited}",
                    arguments = listOf(
                        navArgument("added") { defaultValue = "false" },
                        navArgument("edited") { defaultValue = "false" }
                    )
                ) {
                    JournalListScreen(
                        navController = navController,
                        listJurnal = listJurnal,
                        added = false,
                        edited = false,
                        onAddClick = { navController.navigate("mood_picker") },
                        onDelete = { index ->
                            val item = listJurnal.getOrNull(index) ?: return@JournalListScreen
                            deleteJournalFromFirestore(item.id)
                        }
                    )
                }

                // MOOD SCREENS
                composable("mood_calendar/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    MoodCalendarScreen(
                        navController = navController,
                        userId = userId
                    )
                }
                //composable("mood_map") { MoodMapScreen(navController, listJurnal) }

                composable("mood_picker") {
                    MoodPickerScreen(
                        onContinue = { emoji, mood ->
                            navController.navigate("write_journal/$emoji/$mood")
                        },
                        navController = navController
                    )
                }

                // WRITE JOURNAL
                composable(
                    "write_journal/{emoji}/{mood}",
                    arguments = listOf(
                        navArgument("emoji") { type = NavType.StringType },
                        navArgument("mood") { type = NavType.StringType }
                    )
                ) { back ->

                    val emoji = back.arguments?.getString("emoji") ?: "😐"
                    val mood = back.arguments?.getString("mood") ?: "Neutral"
                    val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        .format(Date())

                    WriteJournalScreen(
                        emoji = emoji,
                        mood = mood,
                        date = date,

                        onSave = { title, content, location, dateNow ->
                            saveJournalToFirestore(
                                emoji, mood, title, content, location, dateNow
                            ) {
                                navController.navigate("journal_list?added=true") {
                                    popUpTo("journal_list") { inclusive = true }
                                }
                            }
                        },

                        onSkip = { dateNow ->
                            saveJournalToFirestore(
                                emoji, mood, "", "", "", dateNow
                            ) {
                                navController.navigate("journal_list") {
                                    popUpTo("journal_list") { inclusive = true }
                                }
                            }
                        },

                        navController = navController
                    )
                }

                // JOURNAL DETAIL
                composable(
                    "journal_detail/{id}?edited={edited}",
                    arguments = listOf(
                        navArgument("edited") { defaultValue = false }
                    )
                ) { back ->

                    val journalId = back.arguments?.getString("id") ?: ""
                    val edited = back.arguments?.getBoolean("edited") ?: false

                    JournalDetailScreen(
                        journalId = journalId,
                        edited = edited,
                        navController = navController
                    )
                }

                // EDIT JOURNAL
                // EDIT JOURNAL (FIXED)
                composable(
                    "edit_journal/{id}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType }
                    )
                ) { back ->

                    val journalId = back.arguments?.getString("id") ?: ""

                    EditJournalScreen(
                        journalId = journalId,
                        navController = navController
                    )
                }
            }
        }
    }
}