package com.shary.app.utils

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import com.shary.app.R // **CRITICAL: Ensure this import points to your app's R class**
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(val environment: String)

fun loadAppConfig(context: Context): AppConfig? {
    var jsonString: String? = null

    // 1. Open the raw resource stream using the resource ID
    val resourceId = R.raw.config

    try {
        context.resources.openRawResource(resourceId).use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                jsonString = reader.readText()
            }
        }
    } catch (e: Exception) {
        // Log IO or Resource Not Found errors
        android.util.Log.e("ConfigLoader", "Failed to read raw resource R.raw.config", e)
        return null
    }

    // 2. Parse the JSON string using Kotlinx Serialization
    if (jsonString != null) {
        return try {
            Json.decodeFromString<AppConfig>(jsonString!!)
        } catch (e: Exception) {
            android.util.Log.e("ConfigLoader", "JSON Parsing Failed!", e)
            null
        }
    }
    return null
}