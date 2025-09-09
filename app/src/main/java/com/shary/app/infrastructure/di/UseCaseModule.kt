package com.shary.app.infrastructure.di

import com.shary.app.application.usecases.RegisterFcmTokenUseCase
import com.shary.app.core.domain.interfaces.services.CloudService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideRegisterFcmTokenUseCase(
        cloudService: CloudService
    ): RegisterFcmTokenUseCase = RegisterFcmTokenUseCase(cloudService)
}
