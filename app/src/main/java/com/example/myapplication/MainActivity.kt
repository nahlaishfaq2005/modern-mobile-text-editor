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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val theme by settingsViewModel.theme.collectAsState()
            
            val isDarkTheme = when (theme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            
            MyApplicationTheme(darkTheme = isDarkTheme) {
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
                            onNavigateToHome = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onNavigateToRecentAll = { navController.navigate("recent_all") },
                            onNavigateToEditor = { newName, newType ->
                                navController.navigate("editor/$newName/$newType")
                            },
                            onNavigateToVersions = { navController.navigate("versions") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            settingsViewModel = settingsViewModel
                        )
                    }
                    composable("recent_all") {
                        RecentFilesScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToEditor = { name, type ->
                                navController.navigate("editor/$name/$type")
                            },
                            onNavigateToHome = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onNavigateToVersions = {
                                navController.navigate("versions")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }
                    composable("versions") {
                        VersionDocumentsScreen(
                            onNavigateToHistory = { fileName ->
                                navController.navigate("versions/$fileName")
                            },
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToHome = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onNavigateToEditor = {
                                navController.navigate("recent_all")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }
                    composable(
                        route = "versions/{fileName}",
                        arguments = listOf(navArgument("fileName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
                        VersionsScreen(
                            fileName = fileName,
                            onNavigateBack = { navController.popBackStack() },
                            onViewVersion = { versionId ->
                                navController.navigate("version_preview/$versionId?fileName=$fileName")
                            },
                            onCompareVersions = { v1, v2 ->
                                navController.navigate("diff/$v2?oldVersionId=$v1&fileName=$fileName")
                            },
                            onNavigateToHome = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onNavigateToEditor = {
                                navController.navigate("recent_all")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }
                    composable(
                        route = "version_preview/{versionId}?fileName={fileName}",
                        arguments = listOf(
                            navArgument("versionId") { type = NavType.StringType },
                            navArgument("fileName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val versionId = backStackEntry.arguments?.getString("versionId") ?: ""
                        val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
                        VersionPreviewScreen(
                            versionId = versionId,
                            fileName = fileName,
                            onNavigateBack = { navController.popBackStack() },
                            onRestore = {
                                navController.navigate("versions/$fileName") {
                                    popUpTo("versions/$fileName") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        route = "diff/{newVersionId}?oldVersionId={oldVersionId}&fileName={fileName}",
                        arguments = listOf(
                            navArgument("newVersionId") { type = NavType.StringType },
                            navArgument("oldVersionId") { 
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("fileName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val newVersionId = backStackEntry.arguments?.getString("newVersionId")
                        val oldVersionId = backStackEntry.arguments?.getString("oldVersionId")
                        val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
                        DiffViewerScreen(
                            oldVersionId = oldVersionId,
                            newVersionId = newVersionId,
                            fileName = fileName,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onNavigateToHome = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onNavigateToEditor = {
                                navController.navigate("recent_all")
                            },
                            onNavigateToVersions = {
                                navController.navigate("versions")
                            },
                            viewModel = settingsViewModel
                        )
                    }
                }
            }
        }
    }
}
