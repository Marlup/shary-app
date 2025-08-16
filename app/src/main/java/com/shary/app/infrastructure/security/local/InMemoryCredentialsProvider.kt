package com.shary.app.infrastructure.security.local

import com.shary.app.core.domain.security.CredentialsProvider


class InMemoryCredentialsProvider : CredentialsProvider {
    private var username: String = ""
    private var password: CharArray = charArrayOf()

    fun setCredentials(user: String, pass: CharArray) {
        username = user
        password = pass
    }

    override fun username() = username
    override fun password() = password
}
