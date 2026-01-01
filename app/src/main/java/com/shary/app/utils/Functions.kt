package com.shary.app.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.shary.app.core.domain.models.FieldDomain
import org.json.JSONObject
import java.io.File
import java.io.StringWriter
import java.util.regex.Pattern

object Functions {

    fun buildFileFromFields(field: List<FieldDomain>, fileFormat: String = "json"): String? {
        return when (fileFormat) {
            "json" -> buildJsonStringFromFields(field)
            "csv" -> makeCSVStringFromFields(field)
            "xml" -> makeXMLStringFromFields(field)
            "yaml" -> makeYamlStringFromFields(field)
            else -> null
        }
    }

    fun buildJsonStringFromFields(fields: List<FieldDomain>): String {
        val json = JSONObject()
        fields.forEach { field ->
            json.put(field.key, field.value)
        }
        return json.toString(4)
    }

    fun makeJsonStringFromRequestKeys(fields: List<FieldDomain>, user: String): String {
        val json = JSONObject()
        json.put("user", user)

        val keys = org.json.JSONArray()
        fields.forEach { field -> keys.put(field.key) }
        json.put("keys", keys)

        return json.toString(4)
    }

    private fun makeCSVStringFromFields(fields: List<FieldDomain>): String {
        val writer = StringWriter()
        writer.append("Key,Value\n")
        fields.forEach { field ->
            writer.append("$field.key,$field.value\n")
        }
        return writer.toString()
    }

    private fun makeXMLStringFromFields(fields: List<FieldDomain>): String {
        val builder = StringBuilder()
        builder.append("<Fields>\n")
        fields.forEach { field ->
            builder.append("    <Field key=\"$field.key\">$field.value</Field>\n")
        }
        builder.append("</Fields>")
        return builder.toString()
    }

    private fun makeYamlStringFromFields(fields: List<FieldDomain>): String {
        val builder = StringBuilder()
        fields.forEach { field ->
            builder.append("$field.key: $field.value\n")
        }
        return builder.toString()
    }

    fun makeStringListFromFields(fields: List<FieldDomain>): String {
        return fields.joinToString(separator = "\n\t") { field ->
            "- ${field.key.toString()}: ${field.value.toString()}"
        }
    }

    fun makeStringListFromKeys(keys: List<String>): String {
        return keys.joinToString(separator = "\n\t") { key -> "- $key" }
    }

    fun informationPanel(context: Context, panelName: String, message: String) {
        Toast.makeText(context, "$panelName: $message", Toast.LENGTH_LONG).show()
    }

    fun validatePassword(password: String): Pair<Boolean, String> {
        if (password.length < 8)
            return false to "Password must be at least 8 characters long."
        if (!Pattern.compile("[A-Z]").matcher(password).find())
            return false to "Password must contain at least one uppercase letter."
        if (!Pattern.compile("[a-z]").matcher(password).find())
            return false to "Password must contain at least one lowercase letter."
        if (!Pattern.compile("\\d").matcher(password).find())
            return false to "Password must contain at least one number."
        if (!Pattern.compile("[!@#\$%^&*(),.?\":{}|<>]").matcher(password).find())
            return false to "Password must contain at least one special character."

        Log.d("Functions", "Password syntax validated.")
        return true to ""
    }

    fun validateEmailSyntax(email: String): Pair<Boolean, String> {
        if (email.isEmpty())
            return false to "Email cannot be empty."
        if (!email.contains("@"))
            return false to "Unexpected email format: @ missing."
        if (email.startsWith("@"))
            return false to "Unexpected email format: starts with @."

        Log.d("Functions", "Email syntax validated: '$email'")
        return true to ""
    }

    fun checkNewJsonFiles(oldFiles: List<String>, downloadPath: String): Boolean {
        val newFiles = File(downloadPath).listFiles()?.filter { it.extension == "json" } ?: return false
        return oldFiles.size != newFiles.size
    }

    fun getDownloadedFiles(downloadPath: String): List<String> {
        val dir = File(downloadPath)
        if (!dir.exists()) dir.mkdirs()
        return dir.list()?.filter { it.endsWith(".json") } ?: emptyList()
    }

    fun enterMessage(hasCreds: Boolean, isRegistered: Boolean): String {
        val credsMsg = if (hasCreds) "You have credentials." else "You don't have credentials."
        val regisMsg = if (isRegistered) "You are registered and online." else "You are not registered or online."
        return "$credsMsg $regisMsg"
    }
}
