package com.shary.app.infrastructure.di

import android.content.Context
import com.shary.app.core.domain.interfaces.security.AuthenticationService
import com.shary.app.application.security.LoginUseCase
import com.shary.app.application.security.RegisterUseCase
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.interfaces.security.AuthBackend
import com.shary.app.infrastructure.persistance.credentials.FileCredentialsStore
import com.shary.app.infrastructure.security.auth.AuthenticationServiceImpl
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.infrastructure.security.auth.InMemoryAuthBackend
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides @Singleton fun provideRegisterUC(
        crypto: CryptographyManager,
        authBackend: AuthBackend
    ) = RegisterUseCase(crypto, authBackend)


    @Provides @Singleton fun provideLoginUC(
        crypto: CryptographyManager,
        authBackend: AuthBackend
    ) = LoginUseCase(crypto, authBackend)


    @Provides @Singleton fun provideFileCredentialsStore(
        @ApplicationContext context: Context
    ): CredentialsStore = FileCredentialsStore(context)


    @Provides @Singleton fun provideAuthService(
        crypto: CryptographyManager,
        fileCredentialsStore: FileCredentialsStore,
    ): AuthenticationService = AuthenticationServiceImpl(crypto, fileCredentialsStore)

    @Provides @Singleton fun provideAuthBackend(
        crypto: CryptographyManager
    ): AuthBackend = InMemoryAuthBackend(crypto)
}
