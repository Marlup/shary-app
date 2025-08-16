package com.shary.app.core.domain.security


interface CredentialsProvider {
    fun username(): String
    fun password(): CharArray
}