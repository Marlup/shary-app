package com.shary.app.core.domain.interfaces.navigator

import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CredentialsEntryPoint {
    fun credentialsStore(): CredentialsStore
}