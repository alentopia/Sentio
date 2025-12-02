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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainNavHost() {
    val navController = rememberNavController()
    val listJurnal = remember { mutableStateListOf<JurnalModel>() }
    val systemUiController = rememberSystemUiController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Cek apakah sedang di halaman Auth (signin/signup/forgotpassword)
    val isAuthScreen = currentRoute?.startsWith("signin") == true ||
            currentRoute?.startsWith("signup") == true ||
            currentRoute?.startsWith("forgotpassword") == true

    //  Ubah warna system bar sesuai halaman
    SideEffect {
        if (isAuthScreen) {
            systemUiController.setStatusBarColor(Color(0xFF7E63FF), darkIcons = false)
            systemUiController.setNavigationBarColor(Color.White, darkIcons = true)
        } else {
            systemUiController.setStatusBarColor(Color.Transparent, darkIcons = true)
            systemUiController.setNavigationBarColor(Color.Transparent, darkIcons = true)
        }
    }

    // Gradient Background
    val gradientColors = if (isAuthScreen) {
        listOf(Color(0xFF7E63FF), Color.White) // ungu dan putih untuk sign in, sign up, dan forgetpassword
    } else {
        listOf(Color(0xFFEED3F2), Color(0xFFD1E5FF)) // gradient gitu untuk yg lainny
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradientColors))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            // Bottom nav cuma muncul di halaman utama
            bottomBar = {
                val showBottomNav =
                    currentRoute?.startsWith("journal_list") == true ||
                            currentRoute?.startsWith("profile") == true ||
                            currentRoute in listOf("home", "music", "mood_calendar")

                if (showBottomNav) {
                BottomNavBar(navController = navController)
            }
            }
        ) { innerPadding ->

            NavHost(
                navController = navController,
                startDestination = "splash",
                modifier = Modifier.padding(innerPadding)
            ) {
                //  Splash & Auth
                composable("splash") { SplashScreen(navController) }
                composable(
                    route = "signin?created={created}",
                    arguments = listOf(
                        navArgument("created") { defaultValue = "false" }
                    )
                ) { backStackEntry ->
                    val created = backStackEntry.arguments?.getString("created")?.toBoolean() ?: false
                    SignInScreen(navController = navController, created = created)
                }
                composable("signup") { SignUpScreen(navController) }
                composable("forgotpassword") { ForgotPasswordScreen(navController) }

                //  Home Tabs
                composable("home") { HomeScreen(navController, listJurnal) }
                composable("music") {
                    MusicScreen(
                        clientId = "9ace244d481f48d5b34acea812017a62",
                        clientSecret = "6bf846d587384642ae370f0700876276" //ini seharusnya rahasia deh tapi saya bingung kalo saya gak masukkin trus gmn :v
                    )
                }
                composable(
                    route = "profile?edited={edited}",
                    arguments = listOf(
                        navArgument("edited") { defaultValue = "false" }
                    )
                ) { backStackEntry ->
                    val edited = backStackEntry.arguments?.getString("edited")?.toBoolean() ?: false
                    ProfileScreen(navController = navController, edited = edited)
                }

                composable("edit_profile") { EditProfileScreen(navController) }
                composable("help_support") { HelpSupportScreen(navController) }

                //  Journal List
                composable(
                    route = "journal_list?added={added}&edited={edited}",
                    arguments = listOf(
                        navArgument("added") { defaultValue = "false" },
                        navArgument("edited") { defaultValue = "false" }
                    )
                ) { backStackEntry ->
                    val added = backStackEntry.arguments?.getString("added")?.toBoolean() ?: false
                    val edited = backStackEntry.arguments?.getString("edited")?.toBoolean() ?: false

                    JournalListScreen(
                        navController = navController,
                        listJurnal = listJurnal,
                        added = added,
                        edited = edited,
                        onAddClick = { navController.navigate("mood_picker") },
                        onDelete = { index ->
                            if (index in listJurnal.indices) listJurnal.removeAt(index)
                        }
                    )
                }

                // Calendar
                composable("mood_calendar") { MoodCalendarScreen(navController) }
                // MoodMap
                composable("mood_map") {
                    MoodMapScreen(navController, listJurnal)
                }
                // Mood Picker ke Write Journal
                composable("mood_picker") {
                    MoodPickerScreen(
                        onContinue = { emoji, mood ->
                            navController.navigate("write_journal/$emoji/$mood")
                        },
                        navController = navController
                    )
                }

                composable(
                    "write_journal/{emoji}/{mood}",
                    arguments = listOf(
                        navArgument("emoji") { type = NavType.StringType },
                        navArgument("mood") { type = NavType.StringType }
                    )
                ) { backStack ->
                    val emoji = backStack.arguments?.getString("emoji") ?: "😐"
                    val mood = backStack.arguments?.getString("mood") ?: "Neutral"
                    val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

                    WriteJournalScreen(
                        emoji = emoji,
                        mood = mood,
                        date = date,
                        onSave = { title, content, location, dateNow ->
                            listJurnal.add(
                                0,
                                JurnalModel(
                                    emoji = emoji,
                                    mood = mood,
                                    title = title,
                                    content = content,
                                    location = location,
                                    date = dateNow
                                )
                            )
                            navController.navigate("journal_list?added=true") {
                                popUpTo("journal_list") { inclusive = true }
                            }
                        },
                        onSkip = { dateNow ->
                            listJurnal.add(
                                0,
                                JurnalModel(
                                    emoji = emoji,
                                    mood = mood,
                                    title = "Untitled",
                                    content = "",
                                    location = "Unknown",
                                    date = dateNow
                                )
                            )
                            navController.navigate("journal_list") {
                                popUpTo("journal_list") { inclusive = true }
                            }
                        },
                        navController = navController
                    )
                }

                //  Journal Detail
                composable(
                    "journal_detail/{title}/{content}/{date}/{emoji}/{location}?edited={edited}",
                    arguments = listOf(
                        navArgument("title") { type = NavType.StringType },
                        navArgument("content") { type = NavType.StringType },
                        navArgument("date") { type = NavType.StringType },
                        navArgument("emoji") { type = NavType.StringType },
                        navArgument("location") { type = NavType.StringType },
                        navArgument("edited") { defaultValue = "false" }
                    )
                ) { entry ->
                    val title = entry.arguments?.getString("title") ?: ""
                    val content = entry.arguments?.getString("content") ?: ""
                    val date = entry.arguments?.getString("date") ?: ""
                    val emoji = entry.arguments?.getString("emoji") ?: ""
                    val location = entry.arguments?.getString("location") ?: ""
                    val edited = entry.arguments?.getString("edited")?.toBoolean() ?: false

                    JournalDetailScreen(
                        title = title,
                        content = content,
                        date = date,
                        emoji = emoji,
                        location = location,
                        edited = edited,
                        navController = navController
                    )
                }

                // Edit Journal
                composable(
                    "edit_journal/{title}/{content}/{date}/{emoji}/{location}",
                    arguments = listOf(
                        navArgument("title") { type = NavType.StringType },
                        navArgument("content") { type = NavType.StringType },
                        navArgument("date") { type = NavType.StringType },
                        navArgument("emoji") { type = NavType.StringType },
                        navArgument("location") { type = NavType.StringType }
                    )
                ) { entry ->
                    val title = entry.arguments?.getString("title") ?: ""
                    val content = entry.arguments?.getString("content") ?: ""
                    val date = entry.arguments?.getString("date") ?: ""
                    val emoji = entry.arguments?.getString("emoji") ?: ""
                    val location = entry.arguments?.getString("location") ?: ""

                    EditJournalScreen(
                        title = title,
                        content = content,
                        date = date,
                        emoji = emoji,
                        location = location,
                        navController = navController,
                        onSave = { newTitle, newContent, newDate, newLocation ->
                            val index = listJurnal.indexOfFirst { it.title == title && it.date == date }

                            if (index != -1) {
                                listJurnal[index] = listJurnal[index].copy(
                                    title = newTitle,
                                    content = newContent,
                                    date = newDate,
                                    location = newLocation,
                                    isEdited = true
                                )
                            }

                            // nnti dia lempar data ke detail
                            navController.navigate(
                                "journal_detail/$newTitle/$newContent/$newDate/$emoji/$newLocation?edited=true"
                            ) {
                                popUpTo("journal_detail/$title/$content/$date/$emoji/$location") {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
