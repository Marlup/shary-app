package com.shary.app.infrastructure.services.cloud

import com.shary.app.BuildConfig

object Constants {
    // Time values
    const val TIME_ALIVE_FIREBASE_DOCUMENT: Long = 24 * 60 * 60 // 86400s

    // ---------------- DEBUG ----------------
    // In Android emulators, the localhost won't target the host (your machine). Instead you must use:
    //10.0.2.2 (this will redirect the localhost to the host from the emulator).
    //private const val DEBUG_BASE_FIREBASE_ENDPOINT = "http://10.0.2.2:5002/shary-21b61/us-central1"

    // ---------------- PRODUCTION ----------------
    val FIREBASE_MAIN_ENTRYPOINT = BuildConfig.FIREBASE_BASE_URL


    // ---------------- ENDPOINTS ----------------

    const val FIREBASE_ENDPOINT_GET_PUB_KEY = "/get_pubkey"
    const val FIREBASE_ENDPOINT_UPLOAD_USER = "/upload_user"
    const val FIREBASE_ENDPOINT_DELETE_USER = "/delete_user"
    const val FIREBASE_ENDPOINT_UPLOAD_PAYLOAD = "/upload_payload"
    const val FIREBASE_ENDPOINT_FETCH_PAYLOAD = "/fetch_payload"
    const val FIREBASE_ENDPOINT_PING = "/ping"

}