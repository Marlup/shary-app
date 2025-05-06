package com.shary.app.services.file

import android.content.Context
import android.util.Log
import com.shary.app.core.Session
import com.shary.app.utils.UtilsFunctions.resourcePath
import org.json.JSONObject
import java.io.File

class FileService(private val context: Context, private val session: Session) {
    fun getJsonFiles(from: String = "data/downloaded"): List<String> {

        val dir = resourcePath(context, from)
        return dir
            .listFiles { file -> file.extension == "json" }
            ?.map { it.name } ?: emptyList()
    }

    fun loadFileOfFields(filename: String, from: String = "data/downloaded"): Map<String, Map<String, String>> {
        val file = File(resourcePath(context, from), filename)
        Log.d("FileService - path", file.absolutePath)
        val json = file.readText()
        val parsed = JSONObject(json)
        val dataObj = parsed.optJSONObject("fields") ?: JSONObject()
        val data = mutableMapOf<String, String>()
        dataObj.keys().forEach { key ->
            data[key] = dataObj.getString(key)
        }
        return mapOf("fields" to data)
    }
}
