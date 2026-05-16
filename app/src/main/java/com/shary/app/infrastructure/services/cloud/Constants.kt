package com.shary.app.infrastructure.services.cloud

import com.shary.app.BuildConfig

object Constants {
    // Time values
    const val TIME_ALIVE_FIREBASE_DOCUMENT: Long = 7 * 24 * 60 * 60 // 7 days

    // ---------------- DEBUG ----------------
    // In Android emulators, the localhost won't target the host (your machine). Instead you must use:
    //10.0.2.2 (this will redirect the localhost to the host from the emulator).

    // ---------------- PRODUCTION ----------------
    val FIREBASE_MAIN_ENTRYPOINT = BuildConfig.FIREBASE_BASE_URL


    // ---------------- ENDPOINTS ----------------

    const val FIREBASE_ENDPOINT_UPLOAD_PAYLOAD = "/upload_payload"
    const val FIREBASE_ENDPOINT_UPLOAD_REQUEST = "/upload_request"
    const val FIREBASE_ENDPOINT_FETCH_PAYLOAD = "/fetch_payload"
    const val FIREBASE_ENDPOINT_FETCH_REQUEST = "/fetch_request"
    const val FIREBASE_ENDPOINT_PAYLOAD_DECISION = "/payload_decision"
    const val FIREBASE_ENDPOINT_REQUEST_DECISION = "/request_decision"
    const val FIREBASE_ENDPOINT_PING = "/ping"
    const val CLOUD_SCHEMA_VERSION = 1
    const val HEADER_X_REQUEST_ID = "X-Request-Id"

    // ---------------- V2 IDENTITY CONTRACT ----------------
    const val FIREBASE_ENDPOINT_V2_REGISTER_IDENTITY = "/v2/identity/register"
    const val FIREBASE_ENDPOINT_V2_ROTATE_IDENTITY = "/v2/identity/rotate"
    const val FIREBASE_ENDPOINT_V2_DELETE_IDENTITY = "/v2/identity/delete"
    const val FIREBASE_ENDPOINT_V2_GET_PUB_KEY = "/v2/identity/pubkey"

    const val ENFORCE_VERIFIED_EMAIL_FOR_IDENTITY = true

}
