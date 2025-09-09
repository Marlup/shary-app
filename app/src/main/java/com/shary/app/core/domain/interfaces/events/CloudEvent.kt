import com.shary.app.core.domain.types.enums.StatusDataSentDb

sealed interface CloudEvent {
    data class PingResult(val ok: Boolean) : CloudEvent
    data class UserRegisteredResult(val email: String, val registered: Boolean) : CloudEvent
    data class UserUploaded(val email: String, val token: String) : CloudEvent
    data class UserDeleted(val email: String) : CloudEvent
    data class DataUploaded(val result: Map<String, StatusDataSentDb>) : CloudEvent
    data class PubKeyFetched(val userHash: String, val pubKey: String) : CloudEvent
    data class Error(val throwable: Throwable) : CloudEvent
    data class AnonymousReady(val uid: String) : CloudEvent
    data class TokenRefreshed(val token: String) : CloudEvent
    data object CloudSignedOut : CloudEvent
}