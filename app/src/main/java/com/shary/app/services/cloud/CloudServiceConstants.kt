package com.shary.app.services.cloud

object CloudServiceConstants {
    // Time values
    const val TIME_ALIVE_DOCUMENT: Long = 24 * 60 * 60 // 3600s

    // En emuladores de Android, localhost no apunta al host (tu máquina). En su lugar debes usar:
    //10.0.2.2 (esto redirige localhost al host desde el emulador).
    const val BACKEND_HOST = "10.0.2.2" //"localhost"
    const val BACKEND_PORT = 5001
    const val BACKEND_APP_ID = "shary-21b61"
    const val NAME_GC_LOCATION_HOST = "us-central1"
    const val BASE_ENDPOINT = "http://$BACKEND_HOST:$BACKEND_PORT/$BACKEND_APP_ID/$NAME_GC_LOCATION_HOST"
    const val ENDPOINT_GET_PUB_KEY = "$BASE_ENDPOINT/get_pubkey"
    const val ENDPOINT_STORE_USER = "$BASE_ENDPOINT/store_user"
    const val ENDPOINT_DELETE_USER = "$BASE_ENDPOINT/delete_user"
    const val ENDPOINT_SEND_DATA = "$BASE_ENDPOINT/store_payload"
    const val ENDPOINT_PING = "$BASE_ENDPOINT/ping"
}