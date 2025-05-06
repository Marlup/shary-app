package com.shary.app.repositories.`interface`

import com.shary.app.User

interface UserRepository {
    suspend fun getAllUsers(): List<User>
    suspend fun saveUser(user: User)
    suspend fun saveUserIfNotExists(user: User): Boolean
    suspend fun deleteUser(email: String): Boolean
}
