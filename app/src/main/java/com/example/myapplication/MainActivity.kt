package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                    composable("splash") {
                        SplashScreen(
                            onNavigateToHome = {
                                navController.navigate("home") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("home") {
                        HomeScreen(
                            onNavigateToEditor = { name, type ->
                                navController.navigate("editor/$name/$type")
                            },
                            onNavigateToRecentAll = {
                                navController.navigate("recent_all")
                            },
                            onNavigateToVersions = {
                                navController.navigate("versions")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }
                    composable(
                        route = "editor/{name}/{type}",
                        arguments = listOf(
                            navArgument("name") { type = NavType.StringType },
                            navArgument("type") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val name = backStackEntry.arguments?.getString("name") ?: "Untitled"
                        val type = backStackEntry.arguments?.getString("type") ?: "Kotlin"
                        EditorScreen(
                            fileName = name,
                            fileType = type,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToRecentAll = { navController.navigate("recent_all") },
                            onNavigateToVersions = { navController.navigate("versions") },
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("recent_all") {
                        RecentFilesScreen()
                    }
                    composable("versions") {
                        VersionsScreen()
                    }
                    composable("settings") {
                        SettingsScreen()
                    }
                }
            }
        }
    }
}
