package com.shary.app.core.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CredentialsLocalDataSource {
    suspend fun hasCredentials(): Boolean
}
interface CryptoLocalDataSource {
    suspend fun hasActiveSignatureKey(): Boolean
}
interface AppPrefsDataSource {
    suspend fun isFirstRun(): Boolean
}

class SessionStatusRepositoryImpl(
    private val credentials: CredentialsLocalDataSource,
    private val crypto: CryptoLocalDataSource,
    private val appPrefs: AppPrefsDataSource
) : SessionStatus {
    override suspend fun hasValidCredentials(): Boolean = withContext(Dispatchers.IO) {
        credentials.hasCredentials()
    }
    override suspend fun hasActiveSignature(): Boolean = withContext(Dispatchers.IO) {
        crypto.hasActiveSignatureKey()
    }
    override suspend fun isFirstRun(): Boolean = withContext(Dispatchers.IO) {
        appPrefs.isFirstRun()
    }
}
