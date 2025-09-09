package com.shary.app.infrastructure.di

import com.shary.app.core.domain.interfaces.ports.CloudStatusDataSource
import com.shary.app.infrastructure.services.connectivity.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class ConnectivityPingPeriod

@Module
@InstallIn(SingletonComponent::class)
object ConnectivityModule {

    @Provides
    @Singleton
    fun provideCloudStatusDataSource(
        impl: NoopCloudStatusDataSource
    ): CloudStatusDataSource = impl

    @Provides
    @ConnectivityPingPeriod
    fun providePingPeriod(): Long = 10_000L
}
