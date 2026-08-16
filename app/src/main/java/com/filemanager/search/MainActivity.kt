package com.filemanager.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.filemanager.search.data.FileItem
import com.filemanager.search.ui.FileSearchTheme
import com.filemanager.search.ui.screens.FileAnalysisScreen
import com.filemanager.search.ui.screens.FileSearchScreen
import com.filemanager.search.ui.screens.SettingsScreen

sealed class Screen {
    data object Home : Screen()
    data object Settings : Screen()
    data class Analysis(val file: FileItem) : Screen()
}

class MainActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf<Screen>(Screen.Home)
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            FileSearchTheme {
                when (val screen = currentScreen) {
                    Screen.Home -> FileSearchScreen(
                        sharedText = sharedText,
                        onSharedTextConsumed = { sharedText = null },
                        onNavigateToSettings = { currentScreen = Screen.Settings },
                        onNavigateToAnalysis = { file ->
                            currentScreen = Screen.Analysis(file)
                        }
                    )
                    Screen.Settings -> SettingsScreen(
                        onBack = { currentScreen = Screen.Home }
                    )
                    is Screen.Analysis -> FileAnalysisScreen(
                        file = screen.file,
                        onBack = { currentScreen = Screen.Home }
                    )
                }
            }
        }
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
