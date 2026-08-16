package com.filemanager.search.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson = Gson()

    suspend fun analyzeFile(
        fileInfo: String,
        fileContent: String?,
        config: AiConfig
    ): String = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(fileInfo, fileContent)

        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                return@withContext callAi(prompt, config)
            } catch (e: Exception) {
                lastError = e
                if (attempt < 2) Thread.sleep(2000L * (attempt + 1))
            }
        }
        throw lastError ?: Exception("Failed after 3 attempts")
    }

    private fun buildPrompt(fileInfo: String, fileContent: String?): String {
        val contentSection = if (!fileContent.isNullOrBlank()) {
            "\n\nFile content (first portion):\n```\n$fileContent\n```"
        } else ""

        return """You are a helpful file analysis assistant. Analyze this file and provide useful information.

File details:
$fileInfo$contentSection

Provide a clear analysis:
1. **What is this file?** — Purpose and description
2. **How to open it** — Recommended apps/programs
3. **Content summary** — (if content is available)
4. **Tips** — Any useful information

Keep the response concise. If the file content is in Arabic, respond in Arabic."""
    }

    private fun callAi(prompt: String, config: AiConfig): String {
        val url = buildApiUrl(config)
        val model = buildModel(config)

        val body = gson.toJson(mapOf(
            "model" to model,
            "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
            "max_tokens" to 1500,
            "temperature" to 0.7
        ))

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json; charset=UTF-8")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (response.code == 429) throw Exception("Rate limit exceeded — wait a moment")
        if (response.code == 401) throw Exception("Invalid API key — check Settings")
        if (response.code != 200) throw Exception("Server error ${response.code}: ${responseBody?.take(150)}")
        if (responseBody.isNullOrBlank()) throw Exception("Empty response from server")

        return extractText(responseBody)
    }

    private fun buildApiUrl(config: AiConfig): String {
        return when (config.provider) {
            "groq"       -> "https://api.groq.com/openai/v1/chat/completions"
            "openrouter" -> "https://openrouter.ai/api/v1/chat/completions"
            "openai"     -> "https://api.openai.com/v1/chat/completions"
            "gemini"     -> {
                val m = config.model.ifBlank { "gemini-2.0-flash" }
                "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=${config.apiKey}"
            }
            "custom"     -> "${config.baseUrl.trimEnd('/')}/v1/chat/completions"
            else         -> throw Exception("Unknown provider: ${config.provider}")
        }
    }

    private fun buildModel(config: AiConfig): String {
        return config.model.ifBlank {
            when (config.provider) {
                "groq"       -> "llama-3.3-70b-versatile"
                "openrouter" -> "google/gemini-2.0-flash-exp"
                "openai"     -> "gpt-4o-mini"
                else         -> "auto"
            }
        }
    }

    private fun extractText(body: String): String {
        val json = JsonParser.parseString(body).asJsonObject

        json.getAsJsonArray("choices")?.let { choices ->
            if (choices.size() > 0) {
                choices[0].asJsonObject
                    .getAsJsonObject("message")
                    ?.get("content")?.asString?.trim()
                    ?.let { return it }
            }
        }

        json.getAsJsonArray("candidates")?.let { candidates ->
            if (candidates.size() > 0) {
                candidates[0].asJsonObject
                    .getAsJsonObject("content")
                    ?.getAsJsonArray("parts")
                    ?.let { parts ->
                        if (parts.size() > 0) {
                            parts[0].asJsonObject.get("text")?.asString?.trim()
                                ?.let { return it }
                        }
                    }
            }
        }

        throw Exception("Unexpected response format: ${body.take(200)}")
    }
}
