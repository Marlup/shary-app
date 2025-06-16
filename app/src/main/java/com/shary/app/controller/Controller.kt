package com.shary.app.controller

import com.shary.app.core.session.Session
import com.shary.app.services.cloud.CloudService
import com.shary.app.services.email.EmailService
import com.shary.app.services.security.CryptoManager

class Controller(
    private val session: Session,
    private val cryptographyManager: CryptoManager,
    private val cloudService: CloudService,
    private val emailService: EmailService
) {

    // ----- Injection Getters -----
    fun getSecurityService(): CryptoManager {
        return cryptographyManager
    }

    fun getSession(): Session {
        return session
    }

    fun getCloudService(): CloudService {
        return cloudService
    }

    fun getEmailService(): EmailService {
        return emailService
    }
}
