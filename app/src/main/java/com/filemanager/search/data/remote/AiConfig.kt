package com.filemanager.search.data.remote

data class AiConfig(
    val provider: String = "groq",
    val apiKey: String = "",
    val model: String = "",
    val baseUrl: String = ""
)
