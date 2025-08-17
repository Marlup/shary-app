// ConnectivityBindModule.kt
package com.shary.app.infrastructure.di

import com.shary.app.core.domain.interfaces.ports.CloudStatusDataSource
import com.shary.app.infrastructure.services.connectivity.NoopCloudStatusDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityBindModule {
    @Binds
    @Singleton
    abstract fun bindCloudStatusDataSource(
        impl: NoopCloudStatusDataSource
    ): CloudStatusDataSource
}
