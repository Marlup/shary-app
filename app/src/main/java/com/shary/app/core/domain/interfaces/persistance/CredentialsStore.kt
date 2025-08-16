package com.shary.app.core.domain.interfaces.persistance

import android.content.Context


interface CredentialsStore {
    fun hasSignature(context: Context): Boolean
    fun hasCredentials(context: Context): Boolean
    fun readCredentials(context: Context): ByteArray?
    fun writeCredentials(context: Context, bytes: ByteArray)
    fun deleteCredentials(context: Context)
}