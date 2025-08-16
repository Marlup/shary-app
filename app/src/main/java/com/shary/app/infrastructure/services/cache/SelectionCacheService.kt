package com.shary.app.infrastructure.services.cache

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.interfaces.services.CacheService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SelectionCacheService @Inject constructor(
) : CacheService {
    private val lock = Mutex()

    private val _fields = MutableStateFlow<List<FieldDomain>>(emptyList())
    private val _users  = MutableStateFlow<List<UserDomain>>(emptyList())
    private val _phone  = MutableStateFlow<String?>(null)

    override val fieldsFlow: StateFlow<List<FieldDomain>> get() = _fields
    override val usersFlow: StateFlow<List<UserDomain>> get() = _users

    override fun readFields(): List<FieldDomain> = _fields.value
    override fun readUsers(): List<UserDomain> = _users.value
    override fun getPhoneNumber(): String? = _phone.value


    override fun cacheFields(fields: List<FieldDomain>) {
        // dedup por key
        val unique = fields.distinctBy { it.key.trim().lowercase() }
        _fields.value = unique
    }

    override fun cacheUsers(users: List<UserDomain>) {
        // dedup por email normalizado
        val unique = users.distinctBy { it.email.trim().lowercase() }
        _users.value = unique
    }

    override fun clearFields() { _fields.value = emptyList() }
    override fun clearUsers()  { _users.value = emptyList() }
    override fun clearAll() {
        _fields.value = emptyList()
        _users.value = emptyList()
        _phone.value = null
    }

    override fun setPhoneNumber(number: String?) { _phone.value = number }

    // Si prefieres serializar escrituras:
    suspend fun cacheFieldsSafe(fields: List<FieldDomain>) = lock.withLock { cacheFields(fields) }
    suspend fun cacheUsersSafe(users: List<UserDomain>)   = lock.withLock { cacheUsers(users) }
}
