package com.shary.app.core.domain.interfaces.navigator

import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.session.Session
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// CredentialsStore for AppNavigator's start-destination resolve
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CredentialsEntryPoint {
    fun credentialsStore(): CredentialsStore
}

// Session for HomeScreen
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeDepsEntryPoint {
    fun session(): Session
    // (Add other services later if you need them here)
}
