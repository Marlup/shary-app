package com.shary.app.services.user

import com.shary.app.core.Session

class UserService(
    private val session: Session
) {
    fun cacheSelectedEmails(emails: List<String>) {
        println("Saving selected emails on stop: $emails")
        session.selectedEmails.value = emails
    }
}