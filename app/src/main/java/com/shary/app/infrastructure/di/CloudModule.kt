package com.shary.app.infrastructure.di

import com.shary.app.core.domain.interfaces.ports.AuthPort
import com.shary.app.core.domain.interfaces.ports.ConnectivityPort
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudModule {

    @Provides
    @Singleton
    fun provideConnectivityPort(
        connectivityPort: ConnectivityPort
    ): ConnectivityPort = connectivityPort

    @Provides @Singleton fun provideAuthPort(
        authPort: AuthPort
    ): AuthPort = authPort

    // CloudStateStore already provided as InMemoryCloudStateStore
    // CloudSession provided as @Singleton and injected wherever needed
}
