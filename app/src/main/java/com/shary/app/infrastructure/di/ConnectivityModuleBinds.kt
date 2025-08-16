package com.shary.app.infrastructure.di

import com.shary.app.core.domain.interfaces.ports.CloudStatusDataSource
import com.shary.app.core.domain.interfaces.ports.ConnectivityPort
import com.shary.app.infrastructure.services.connectivity.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Qualifier
annotation class ConnectivityPingPeriod

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModuleBinds {
    @Binds @Singleton
    abstract fun bindConnectivityPort(impl: ConnectivityPortImpl): ConnectivityPort

    @Binds @Singleton
    abstract fun bindCloudStatusDataSource(impl: NoopCloudStatusDataSource): CloudStatusDataSource
}

@Module
@InstallIn(SingletonComponent::class)
object ConnectivityModuleProvides {
    @Provides
    @ConnectivityPingPeriod
    //fun providePingPeriod(): Duration = 30.seconds
    fun providePingPeriodMillis(): Long = 30_000L   // 30s en ms, evita kotlin.time
}
