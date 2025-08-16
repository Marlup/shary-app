package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.models.UserDomain
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val users: Flow<List<UserDomain>>
    suspend fun getAllUsers(): List<UserDomain>
    suspend fun saveUserIfNotExists(user: UserDomain): Boolean
    suspend fun upsertUser(user: UserDomain): Boolean
    suspend fun deleteUser(email: String): Boolean
    suspend fun deleteUsers(emails: Collection<String>): Int
}


