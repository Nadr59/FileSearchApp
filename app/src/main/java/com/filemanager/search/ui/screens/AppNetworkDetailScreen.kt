package com.filemanager.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.filemanager.search.data.FileItem
import com.filemanager.search.ui.FileSearchTheme
import com.filemanager.search.ui.screens.AppNetworkDetailScreen
import com.filemanager.search.ui.screens.FileAnalysisScreen
import com.filemanager.search.ui.screens.FileSearchScreen
import com.filemanager.search.ui.screens.MonitorScreen
import com.filemanager.search.ui.screens.NetworkScreen
import com.filemanager.search.ui.screens.SettingsScreen
import com.filemanager.search.ui.screens.UsageAccessScreen

// ═══ شاشات التطبيق ═══
sealed class Screen {
    data object Home : Screen()
    data object Settings : Screen()
    data class Analysis(val file: FileItem) : Screen()

    // جديد: شاشات المراقبة
    data object Monitor : Screen()
    data object Network : Screen()
    data object UsageAccess : Screen()
    data class AppNetworkDetail(val packageName: String) : Screen()
}

class MainActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf<Screen>(Screen.Home)
    private var previousScreen by mutableStateOf<Screen>(Screen.Home)
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            FileSearchTheme {
                MainContent()
            }
        }
    }

    @Composable
    private fun MainContent() {
        val showBottomBar = when (currentScreen) {
            is Screen.Home, is Screen.Monitor, is Screen.Network -> true
            else -> false
        }

        if (showBottomBar) {
            Scaffold(
                bottomBar = { BottomNavBar() }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    ScreenContent()
                }
            }
        } else {
            ScreenContent()
        }
    }

    @Composable
    private fun ScreenContent() {
        when (val screen = currentScreen) {
            Screen.Home -> FileSearchScreen(
                sharedText = sharedText,
                onSharedTextConsumed = { sharedText = null },
                onNavigateToSettings = { navigateTo(Screen.Settings) },
                onNavigateToAnalysis = { file -> navigateTo(Screen.Analysis(file)) },
                onNavigateToMonitor = { navigateTo(Screen.Monitor) },
                onNavigateToNetwork = { navigateTo(Screen.Network) }
            )

            Screen.Settings -> SettingsScreen(
                onBack = { navigateBack() }
            )

            is Screen.Analysis -> FileAnalysisScreen(
                file = screen.file,
                onBack = { navigateBack() }
            )

            Screen.Monitor -> MonitorScreen(
                onNavigateToUsageAccess = {
                    navigateTo(Screen.UsageAccess)
                },
                onNavigateToNetwork = { navigateTo(Screen.Network) }
            )

            Screen.Network -> NetworkScreen(
                onNavigateToUsageAccess = {
                    navigateTo(Screen.UsageAccess)
                },
                onAppClick = { pkg ->
                    navigateTo(Screen.AppNetworkDetail(pkg))
                }
            )

            Screen.UsageAccess -> UsageAccessScreen(
                onBack = { navigateBack() }
            )

            is Screen.AppNetworkDetail -> AppNetworkDetailScreen(
                packageName = screen.packageName,
                onBack = { navigateBack() }
            )
        }
    }

    @Composable
    private fun BottomNavBar() {
        NavigationBar {
            NavigationBarItem(
                selected = currentScreen is Screen.Home,
                onClick = { navigateTo(Screen.Home) },
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("بحث") }
            )
            NavigationBarItem(
                selected = currentScreen is Screen.Monitor,
                onClick = { navigateTo(Screen.Monitor) },
                icon = { Icon(Icons.Default.Memory, contentDescription = null) },
                label = { Text("الذاكرة") }
            )
            NavigationBarItem(
                selected = currentScreen is Screen.Network,
                onClick = { navigateTo(Screen.Network) },
                icon = { Icon(Icons.Default.DataUsage, contentDescription = null) },
                label = { Text("الشبكة") }
            )
        }
    }

    private fun navigateTo(screen: Screen) {
        previousScreen = currentScreen
        currentScreen = screen
    }

    private fun navigateBack() {
        currentScreen = previousScreen
        previousScreen = Screen.Home
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
}
