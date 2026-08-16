package com.filemanager.search.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AiProvider(
    val key: String,
    val name: String,
    val defaultModel: String,
    val supportsCustomUrl: Boolean = false
)

private val PROVIDERS = listOf(
    AiProvider("groq", "Groq (Free)", "llama-3.3-70b-versatile"),
    AiProvider("openrouter", "OpenRouter", "google/gemini-2.0-flash-exp"),
    AiProvider("openai", "OpenAI", "gpt-4o-mini"),
    AiProvider("gemini", "Gemini", "gemini-2.0-flash"),
    AiProvider("custom", "Custom URL", "auto", supportsCustomUrl = true)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("filesearch_prefs", Context.MODE_PRIVATE)
    }

    var provider by remember {
        mutableStateOf(prefs.getString("ai_provider", "groq") ?: "groq")
    }
    var apiKey by remember {
        mutableStateOf(prefs.getString("ai_key", "") ?: "")
    }
    var model by remember {
        mutableStateOf(prefs.getString("ai_model", "") ?: "")
    }
    var baseUrl by remember {
        mutableStateOf(prefs.getString("ai_base_url", "") ?: "")
    }

    fun save() {
        prefs.edit()
            .putString("ai_provider", provider)
            .putString("ai_key", apiKey)
            .putString("ai_model", model)
            .putString("ai_base_url", baseUrl)
            .apply()
    }

    val selectedProvider = PROVIDERS.find { it.key == provider } ?: PROVIDERS[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { save(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ═══ قسم AI ═══
            Text(
                "AI Provider",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PROVIDERS.forEach { p ->
                    FilterChip(
                        selected = provider == p.key,
                        onClick = {
                            provider = p.key
                            if (model.isBlank()) model = p.defaultModel
                        },
                        label = { Text(p.name, fontSize = 13.sp) }
                    )
                }
            }

            // ═══ مفتاح API ═══
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                placeholder = {
                    Text(
                        when (provider) {
                            "groq" -> "gsk_..."
                            "openai" -> "sk-..."
                            "gemini" -> "AIza..."
                            else -> "Enter API key"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            // ═══ الموديل ═══
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                placeholder = { Text(selectedProvider.defaultModel) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // ═══ رابط مخصص ═══
            if (selectedProvider.supportsCustomUrl) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Custom API URL") },
                    placeholder = { Text("https://api.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(8.dp))

            // ═══ نصائح ═══
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Tips",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Groq is the fastest free provider\n" +
                        "• Get a free key at console.groq.com\n" +
                        "• Leave Model empty for default\n" +
                        "• AI analyzes file content for text/code files",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
