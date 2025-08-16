// :infrastructure  -> com.shary.app.core.session
package com.shary.app.core.session

import com.shary.app.core.domain.interfaces.security.AuthService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.interfaces.services.CacheService
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
    fun setOwnerEmail(newEmail: String) { auth.state.value.email = newEmail }
    
    // Username
    fun getOwnerUsername(): String = auth.state.value.username
    
    // AuthToken
    fun getOwnerAuthToken(): String = auth.getAuthToken()
    fun setOwnerAuthToken(authToken: String) = auth.setAuthToken(authToken)
    
    // SafePassword
    fun getOwnerSafePassword(): String = auth.getSafePassword()
    fun setOwnerSafePassword(safePassword: String) = auth.setSafePassword(safePassword)
    
    // IsOnline
    fun getOwnerIsOnline() = auth.getIsOnline()
    fun setOwnerIsOnline(value: Boolean) = auth.setIsOnline(value)

    
    // ==================== Fields ====================

    fun setSelectedFields(fields: List<FieldDomain>) = cacheSelection.cacheFields(fields)
    fun getSelectedFields(): List<FieldDomain>       = cacheSelection.readFields()
    fun isAnyFieldSelected(): Boolean                = cacheSelection.readFields().isNotEmpty()
    fun resetSelectedFields() = cacheSelection.clearFields()


    // ==================== Users ====================

    fun setSelectedUsers(users: List<UserDomain>)    = cacheSelection.cacheUsers(users)
    fun getSelectedEmails(): List<String>            =
        cacheSelection.readUsers().map { it.email }
    fun isAnyEmailSelected(): Boolean                = cacheSelection.readUsers().isNotEmpty()
    fun resetSelectedUsers() = cacheSelection.clearUsers()


    // ==================== Phone ====================

    fun setSelectedPhoneNumber(n: String?)           = cacheSelection.setPhoneNumber(n)
    fun getSelectedPhoneNumber(): String?            = cacheSelection.getPhoneNumber()


    // ==================== Global ====================

    fun resetSelectedData()                          = cacheSelection.clearAll()
}
