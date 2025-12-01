package com.shary.app.infrastructure.services.cloud

object Constants {
    // Time values
    const val TIME_ALIVE_FIREBASE_DOCUMENT: Long = 24 * 60 * 60 // 3600s

    // ---------------- DEBUG ----------------
    // In Android emulators, the localhost won't target the host (your machine). Instead you must use:
    //10.0.2.2 (this will redirect the localhost to the host from the emulator).
    private const val DEBUG_BASE_FIREBASE_ENDPOINT = "http://10.0.2.2:5002/shary-21b61/us-central1"

    // ---------------- PRODUCTION ----------------

    //private const val FIREBASE_MAIN_ENTRYPOINT = "http://10.0.2.2:5003/shary-21b61/us-central1"
    private const val FIREBASE_MAIN_ENTRYPOINT = "https://us-central1-shary-21b61.cloudfunctions.net"

    // ---------------- ENDPOINTS ----------------

    const val FIREBASE_ENDPOINT_GET_PUB_KEY = "$FIREBASE_MAIN_ENTRYPOINT/get_pubkey"
    const val FIREBASE_ENDPOINT_SEND_USER = "$FIREBASE_MAIN_ENTRYPOINT/send_user"
    const val FIREBASE_ENDPOINT_DELETE_USER = "$FIREBASE_MAIN_ENTRYPOINT/delete_user"
    const val FIREBASE_ENDPOINT_SEND_DATA = "$FIREBASE_MAIN_ENTRYPOINT/send_payload"
    const val FIREBASE_ENDPOINT_PING = "$FIREBASE_MAIN_ENTRYPOINT/ping"
    const val FIREBASE_ENDPOINT_UPDATE_FCM_TOKEN = "$FIREBASE_MAIN_ENTRYPOINT/updateFcmToken"

}