package com.shary.app.infrastructure.persistance.repositories

import androidx.datastore.core.DataStore
import com.shary.app.UserList
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.infrastructure.mappers.toDomain
import com.shary.app.infrastructure.mappers.toProto
import com.shary.app.core.domain.models.UserDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val dataStore: DataStore<UserList>
) : UserRepository {

    // --------------------------- Public API (Domain) ------------------------

    // Reactive stream of domain users
    override val users: Flow<List<UserDomain>> =
        dataStore.data
            .map { it.usersList.map { up -> up.toDomain() } }
            .distinctUntilChanged()

    // Convenience – one-shot snapshot
    override suspend fun getAllUsers(): List<UserDomain> =
        dataStore.data.first().usersList.map { it.toDomain() }

    private fun normalizeEmail(email: String): String =
        email.trim().lowercase()

    override suspend fun saveUserIfNotExists(user: UserDomain): Boolean {
        var added = false
        dataStore.updateData { current ->
            val normalizedTarget = normalizeEmail(user.email)
            val alreadyExists = current.usersList.any { normalizeEmail(it.email) == normalizedTarget }
            if (alreadyExists) {
                // No change -> return current to avoid a write
                current
            } else {
                added = true
                current.toBuilder()
                    .addUsers(user.toProto()) // you could sort here for deterministic order
                    .build()
            }
        }
        return added
    }

    // Upsert (replace if email exists, otherwise add). Returns true if created.
    override suspend fun upsertUser(user: UserDomain): Boolean {
        var created = false
        val protoUser = user.toProto()
        dataStore.updateData { current ->
            val normalizedTarget = normalizeEmail(user.email)
            val index = current.usersList.indexOfFirst { normalizeEmail(it.email) == normalizedTarget }
            if (index >= 0) {
                // Replace only if different to avoid unnecessary writes
                val existing = current.usersList[index]
                if (existing == protoUser) {
                    current
                } else {
                    current.toBuilder()
                        .setUsers(index, protoUser)
                        .build()
                }
            } else {
                created = true
                current.toBuilder()
                    .addUsers(protoUser)
                    .build()
            }
        }
        return created
    }

    override suspend fun deleteUser(email: String): Boolean {
        var removed = false
        dataStore.updateData { current ->
            val normalizedTarget = normalizeEmail(email)
            val filtered = current.usersList.filter { normalizeEmail(it.email) != normalizedTarget }
            if (filtered.size == current.usersList.size) {
                // Nothing removed -> avoid write
                current
            } else {
                removed = true
                current.toBuilder()
                    .clearUsers()
                    .addAllUsers(filtered)
                    .build()
            }
        }
        return removed
    }

    // Batch delete for efficiency
    override suspend fun deleteUsers(emails: Collection<String>): Int {
        if (emails.isEmpty()) return 0
        val normalizedSet = emails.map { normalizeEmail(it) }.toSet()
        var removedCount = 0
        dataStore.updateData { current ->
            val (keep, remove) = current.usersList.partition { normalizeEmail(it.email) !in normalizedSet }
            removedCount = remove.size
            if (removedCount == 0) {
                current
            } else {
                current.toBuilder()
                    .clearUsers()
                    .addAllUsers(keep)
                    .build()
            }
        }
        return removedCount
    }
}
