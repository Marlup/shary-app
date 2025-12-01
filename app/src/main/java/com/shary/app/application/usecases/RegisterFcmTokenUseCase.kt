package com.shary.app.application.usecases

import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.interfaces.services.CloudService


class RegisterFcmTokenUseCase(
    private val cloudService: CloudService
) {
    suspend operator fun invoke(token: String): Boolean {
        return cloudService.updateUserFcmToken(token)
    }
}

