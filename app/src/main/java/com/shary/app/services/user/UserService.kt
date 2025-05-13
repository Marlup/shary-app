package com.shary.app.services.user

import com.shary.app.User
import com.shary.app.core.Session

class UserService(
    private val session: Session
) {
    fun cacheSelectedEmails(emails: List<String>) {
        println("Saving selected emails on stop: $emails")
        session.selectedEmails.value = emails
    }

    fun userToTriple(user: User): Triple<String, String, String> {
        return Triple(user.username, user.email, "")
    }

    fun valuesToUser(username: String, email: String, date: Long=0L): User {
        return User
            .newBuilder()
            .setUsername(username)
            .setEmail(email)
            .setDateAdded(date)
            .build()
    }

    fun userToPair(user: User): Pair<String, String> {
        return Pair(user.username, user.email)
    }
}