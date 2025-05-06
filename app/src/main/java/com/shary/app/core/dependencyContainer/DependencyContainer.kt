package com.shary.app.core.dependencyContainer

import android.content.Context
import com.shary.app.controller.Controller
import com.shary.app.core.Session
import com.shary.app.repositories.impl.FieldRepositoryImpl
import com.shary.app.repositories.impl.UserRepositoryImpl
import com.shary.app.security.CryptographyManager
import com.shary.app.services.cloud.CloudService
import com.shary.app.services.email.EmailService
import com.shary.app.services.field.FieldService
import com.shary.app.services.file.FileService
import com.shary.app.services.requestField.RequestFieldService
import com.shary.app.services.user.UserService

object DependencyContainer {

    private val services = mutableMapOf<String, Any>()

    private fun <T> register(name: String, instance: T) {
        services[name] = instance as Any
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(name: String): T {
        return services[name] as? T ?: throw IllegalStateException("Service '$name' not found.")
    }

    fun initAll(context: Context) {
        // Security
        val cryptographyManager = CryptographyManager
        register("cryptography_manager", cryptographyManager)

        // Session
        val session = Session
        Session.cryptographyManager = CryptographyManager
        register("session", session)

        // Repositories
        val fieldRepository = FieldRepositoryImpl(context)
        register("field_repository", fieldRepository)

        val userRepository = UserRepositoryImpl(context)
        register("user_repository", userRepository)

        // Services
        val emailService = EmailService(context, session)
        register("email_service", emailService)

        val cloudService = CloudService(cryptographyManager)
        register("cloud_service", cloudService)

        val userService = UserService(session)
        register("user_service", userService)

        val fieldService = FieldService(session)
        register("field_service", fieldService)

        val requestFieldService = RequestFieldService(session)
        register("requestField_service", requestFieldService)

        val fileService = FileService(context, session)
        register("file_service", fileService)

        // Controller
        val controller = Controller(session, cryptographyManager, cloudService, emailService)
        register("controller", controller)
    }

    fun clearAll() {
        services.clear()
    }
}
