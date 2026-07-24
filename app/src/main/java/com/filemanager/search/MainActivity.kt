package com.filemanager.search

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.filemanager.search.ui.FileSearchTheme
import com.filemanager.search.ui.screens.FileSearchScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FileSearchTheme {
                FileSearchScreen()
            }
        }
    }
}
