package com.shary.app.core.domain.interfaces.repositories

import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.Tag
import kotlinx.coroutines.flow.Flow

sealed interface ContactUpdateResult {
    data object Updated : ContactUpdateResult
    data object Unchanged : ContactUpdateResult
    data object NotFound : ContactUpdateResult
    data object EmailAlreadyExists : ContactUpdateResult
}

interface UserRepository {
    val users: Flow<List<UserDomain>>
    suspend fun getAllUsers(): List<UserDomain>
    suspend fun saveUserIfNotExists(user: UserDomain): Boolean
    suspend fun upsertUser(user: UserDomain): Boolean
    suspend fun updateUser(originalEmail: String, updatedUser: UserDomain): ContactUpdateResult
    suspend fun updateTag(email: String, newTag: Tag)
    suspend fun deleteUser(email: String): Boolean
    suspend fun deleteUsers(emails: Collection<String>): Int
}


