package com.shary.app.infrastructure.di

import com.shary.app.application.usecases.RegisterFcmTokenUseCase
import com.shary.app.core.domain.interfaces.handler.PushNotificationHandler
import com.shary.app.infrastructure.services.cloud.notification.PushNotificationHandlerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MessagingModule {
    @Provides
    @Singleton
    fun providePushHandler(
        useCase: RegisterFcmTokenUseCase
    ): PushNotificationHandler = PushNotificationHandlerImpl(useCase)
}
