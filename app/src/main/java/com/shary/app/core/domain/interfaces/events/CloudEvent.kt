import com.shary.app.core.domain.types.enums.StatusDataSentDb

sealed interface CloudEvent {
    // Ping
    data class PingResult(val ok: Boolean) : CloudEvent

    // UserRegistered
    data class UserRegisteredResult(val email: String, val registered: Boolean) : CloudEvent

    // UserUpload
    data class UserUploaded(val email: String, val token: String) : CloudEvent

    // UserDeleted
    data class UserDeleted(val email: String) : CloudEvent

    // DataUploaded
    data class DataUploaded(val result: Map<String, StatusDataSentDb>) : CloudEvent

    // PubKeyFetched
    data class PubKeyFetched(val userHash: String, val pubKey: String) : CloudEvent

    // Error
    data class Error(val throwable: Throwable) : CloudEvent
}