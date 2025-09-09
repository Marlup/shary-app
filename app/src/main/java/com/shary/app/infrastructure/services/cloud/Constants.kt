package com.shary.app.infrastructure.services.cloud

object Constants {
    // Time values
    const val TIME_ALIVE_FIREBASE_DOCUMENT: Long = 24 * 60 * 60 // 3600s

    // ---------------- DEBUG ----------------
    // In Android emulators, the localhost won't target the host (your machine). Instead you must use:
    //10.0.2.2 (this will redirect the localhost to the host from the emulator).
    private const val DEBUG_FIREBASE_HOST = "10.0.2.2" //"localhost"
    private const val DEBUG_GC_HOST_LOCATION = "us-central1"
    private const val DEBUG_FIREBASE_PORT = 5002
    private const val DEBUG_FIREBASE_APP_ID = "shary-21b61"
    private const val DEBUG_BASE_FIREBASE_ENDPOINT =
        "https://$DEBUG_FIREBASE_HOST" + ":" + DEBUG_FIREBASE_PORT +
                "/" + DEBUG_FIREBASE_APP_ID + "/" + DEBUG_GC_HOST_LOCATION

    // ---------------- PRODUCTION ----------------

    private const val FIREBASE_APP_ID = "shary-21b61"
    private const val GC_HOST_LOCATION= "us-central1"
    private const val GC_SERVICE_NAME = "cloudfunctions.net"
    //private const val FIREBASE_SAFE_PORT = 443 // HTTPS
    private const val FIREBASE_MAIN_ENTRYPOINT =
        "https://$GC_HOST_LOCATION" + "-" + FIREBASE_APP_ID + "." +
                 GC_SERVICE_NAME

    // ---------------- ENDPOINTS ----------------

    const val FIREBASE_ENDPOINT_GET_PUB_KEY = "$FIREBASE_MAIN_ENTRYPOINT/get_pubkey"
    const val FIREBASE_ENDPOINT_STORE_USER = "$FIREBASE_MAIN_ENTRYPOINT/store_user"
    const val FIREBASE_ENDPOINT_DELETE_USER = "$FIREBASE_MAIN_ENTRYPOINT/delete_user"
    const val FIREBASE_ENDPOINT_SEND_DATA = "$FIREBASE_MAIN_ENTRYPOINT/store_payload"
    const val FIREBASE_ENDPOINT_PING = "$FIREBASE_MAIN_ENTRYPOINT/ping"
    const val FIREBASE_ENDPOINT_UPDATE_FCM_TOKEN = "$FIREBASE_MAIN_ENTRYPOINT/updateFcmToken"

}