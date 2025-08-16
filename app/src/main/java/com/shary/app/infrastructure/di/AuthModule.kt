package com.shary.app.infrastructure.di

import android.content.Context
import com.shary.app.core.domain.interfaces.security.AuthService
import com.shary.app.application.security.LoginUseCase
import com.shary.app.application.security.RegisterUseCase
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.infrastructure.persistance.credentials.FileCredentialsStore
import com.shary.app.infrastructure.security.auth.AuthServiceImpl
import com.shary.app.core.domain.interfaces.security.CryptographyManager
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
        auth: AuthService
    ) = RegisterUseCase(crypto, auth)


    @Provides @Singleton fun provideLoginUC(
        crypto: CryptographyManager,
        credentialsStore: CredentialsStore
    ) = LoginUseCase(crypto, AuthServiceImpl(crypto, credentialsStore))


    @Provides @Singleton fun provideFileCredentialsStore(
        @ApplicationContext context: Context
    ): CredentialsStore = FileCredentialsStore(context)


    @Provides @Singleton fun provideAuthService(
        crypto: CryptographyManager,
        fileCredentialsStore: FileCredentialsStore
    ): AuthService = AuthServiceImpl(crypto, fileCredentialsStore)
}
