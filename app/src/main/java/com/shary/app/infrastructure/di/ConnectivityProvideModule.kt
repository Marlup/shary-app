// ConnectivityProvideModule.kt
package com.shary.app.infrastructure.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ConnectivityProvideModule {
    @Provides
    @DependenciesContainer.ConnectivityPingPeriod
    fun providePingPeriod(): Long = 10_000L
}
