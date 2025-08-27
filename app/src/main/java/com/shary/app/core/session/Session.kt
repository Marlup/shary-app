// :infrastructure  -> com.shary.app.core.session
package com.shary.app.core.session

import com.shary.app.core.domain.interfaces.security.AuthService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.types.valueobjects.Purpose
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Fachada fina para mantener compatibilidad y centralizar el acceso en app/servicios.
 * Delegado internamente a SelectionCache.
 */
@Singleton
class Session @Inject constructor(
    private val cacheSelection: CacheService,
    private val auth: AuthService
) {

    // ==================== Owner ====================

    // Email
    fun getOwnerEmail(): String = auth.state.value.email
    //fun setOwnerEmail(newEmail: String) { auth.state.value.email = newEmail }
    
    // Username
    fun getOwnerUsername(): String = auth.state.value.username
    
    // AuthToken
    fun getOwnerAuthToken(): String = auth.getAuthToken()
    //fun setOwnerAuthToken(authToken: String) = auth.setAuthToken(authToken)
    
    // SafePassword
    fun getOwnerSafePassword(): String = auth.getSafePassword()
    //fun setOwnerSafePassword(safePassword: String) = auth.setSafePassword(safePassword)

    // SafePassword
    fun getLocalKeyByPurpose(purpose: Purpose): ByteArray? = auth.getLocalKeyByPurpose(purpose)
    //fun addLocalKeyByPurpose(purpose: Purpose, value: ByteArray) = auth.addLocalKeyByPurpose(purpose, value)

    // IsOnline
    fun getOwnerIsOnline() = auth.getIsOnline()
    fun setOwnerIsOnline(value: Boolean) = auth.setIsOnline(value)

    
    // ==================== Fields ====================

    fun setCachedFields(fields: List<FieldDomain>) = cacheSelection.cacheFields(fields)
    fun getCachedFields(): List<FieldDomain>       = cacheSelection.readFields()
    fun isAnyFieldCached(): Boolean                = cacheSelection.readFields().isNotEmpty()
    fun resetCachedFields() = cacheSelection.clearFields()


    // ==================== Users ====================

    fun setCachedUsers(users: List<UserDomain>)    = cacheSelection.cacheUsers(users)
    fun getCachedEmails(): List<String>            =
        cacheSelection.readUsers().map { it.email }
    fun isAnyUserCached(): Boolean = cacheSelection.readUsers().isNotEmpty()
    fun resetCachedUsers() = cacheSelection.clearUsers()


    // ==================== Phone ====================

    fun setCachedPhoneNumber(n: String?)           = cacheSelection.setPhoneNumber(n)
    //fun getCachedPhoneNumber(): String?            = cacheSelection.getPhoneNumber()


    // ==================== Global ====================

    fun resetCachedData()                          = cacheSelection.clearAll()
}
