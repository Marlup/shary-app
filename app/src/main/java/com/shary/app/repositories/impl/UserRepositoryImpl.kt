package com.shary.app.repositories.impl

import android.content.Context
import androidx.datastore.core.DataStore
import com.shary.app.User
import com.shary.app.UserList
import com.shary.app.datastore.userListDataStore
import com.shary.app.repositories.`interface`.UserRepository
import kotlinx.coroutines.flow.first

class UserRepositoryImpl(
    context: Context
) : UserRepository {
    private val dataStore: DataStore<UserList> = context.userListDataStore

    override suspend fun getAllUsers(): List<User> {
        val userList = dataStore.data.first()
        return userList.usersList
    }

    override suspend fun saveUser(user: User) {
        dataStore.updateData { current ->
            current.toBuilder()
                .addUsers(user)
                .build()
        }
    }

    override suspend fun saveUserIfNotExists(user: User): Boolean {
        val existingUsers = getAllUsers()
        val userAlreadyExists = existingUsers.any { it.email == user.email }

        if (!userAlreadyExists) {
            saveUser(user)
            return true
        } else {
            return false
        }
    }

    override suspend fun deleteUser(email: String): Boolean {
        dataStore.updateData { current ->
            val updatedUsers = current.usersList.filter { it.email != email }
            current.toBuilder()
                .clearUsers()
                .addAllUsers(updatedUsers)
                .build()
        }
        return true
    }
}
