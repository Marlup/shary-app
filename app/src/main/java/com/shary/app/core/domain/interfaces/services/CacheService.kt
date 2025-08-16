package com.shary.app.core.domain.interfaces.services

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import kotlinx.coroutines.flow.StateFlow


/**
 * Cache efímera de selección cross-screen (proceso).
 * No persiste en disco. Thread-safe vía flujos/serialización de accesos en la impl.
 */
interface CacheService {

    // Observables (útiles para reactive UI/servicios)
    val fieldsFlow: StateFlow<List<FieldDomain>>
    val usersFlow: StateFlow<List<UserDomain>>


    // Fields
    fun clearFields()
    fun cacheFields(fields: List<FieldDomain>)
    fun readFields(): List<FieldDomain>


    // Users
    fun clearUsers()
    fun cacheUsers(users: List<UserDomain>)
    fun readUsers(): List<UserDomain>


    // Extras opcionales (por si usas teléfono en WhatsApp/Telegram)
    fun setPhoneNumber(number: String?)
    fun getPhoneNumber(): String?
    fun clearAll()
}
