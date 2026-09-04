package com.shary.app.application

import android.app.Application
import com.shary.app.application.session.SessionTimeoutCoordinator
import com.shary.app.infrastructure.services.clipboard.ClipboardCleanupCoordinator
import com.shary.app.infrastructure.services.email.EmailAttachmentCleanupCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject lateinit var sessionTimeoutCoordinator: SessionTimeoutCoordinator
    @Inject lateinit var clipboardCleanupCoordinator: ClipboardCleanupCoordinator
    @Inject lateinit var emailAttachmentCleanupCoordinator: EmailAttachmentCleanupCoordinator

    override fun onCreate() {
        super.onCreate()
        clipboardCleanupCoordinator.start()
        emailAttachmentCleanupCoordinator.start()
        sessionTimeoutCoordinator.start()
    }
}
