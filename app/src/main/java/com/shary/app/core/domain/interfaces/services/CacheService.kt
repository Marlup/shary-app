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
    fun clearCachedFields()
    fun cacheFields(fields: List<FieldDomain>)
    fun getFields(): List<FieldDomain>
    fun isAnyFieldCached(): Boolean


    // Users
    fun clearCachedUsers()
    fun cacheUsers(users: List<UserDomain>)
    fun getUsers(): List<UserDomain>
    fun isAnyUserCached(): Boolean


    // Owner
    fun cacheOwner(owner: UserDomain)
    fun cacheOwnerUsername(username: String?)
    fun cacheOwnerEmail(email: String?)
    fun cachePhoneNumber(number: String?)
    fun getOwner(): UserDomain
    fun getOwnerUsername(): String?
    fun getOwnerEmail(): String?
    fun getPhoneNumber(): String?

    // General
    fun clearAllCaches()
}
