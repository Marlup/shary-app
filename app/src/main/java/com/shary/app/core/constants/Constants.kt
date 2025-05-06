package com.shary.app.core.constants

object Constants {

    // Dimensions
    const val FIELDS_HEIGHT = 1200
    const val FIELDS_WIDTH = 1800
    const val LOGIN_HEIGHT = 1200
    const val LOGIN_WIDTH = 1800
    const val ROW_HEIGHT = 40
    const val USERS_HEIGHT = 1200
    const val USERS_WIDTH = 1800
    const val DEFAULT_NUM_ROWS_PAGE = 20
    const val DEFAULT_ROW_KEY_WIDTH = 50
    const val DEFAULT_ROW_REST_WIDTH = 50
    const val DEFAULT_ROW_VALUE_WIDTH = 200
    const val DEFAULT_USE_PAGINATION = false
    const val DEFAULT_SECRET_LENGTH = 16

    // Time values
    const val TIME_ALIVE_DOCUMENT: Long = 24 * 60 * 60 // 3600s

    // Formats
    val FIELD_HEADERS = arrayOf("key", "value", "creation_date")
    val FILE_FORMATS = arrayOf("json", "csv", "xml", "yaml")
    val USER_HEADERS = arrayOf("username", "email", "creation_date")

    // Names
    const val SCREEN_NAME_FIELDS = "fields"
    const val SCREEN_NAME_FILES_VISUALIZER = "files_visualizer"
    const val SCREEN_NAME_LOGIN = "login"
    const val SCREEN_NAME_REQUESTS = "requests"
    const val SCREEN_NAME_USERS = "users"
    const val SCREEN_NAME_USER_CREATION = "user_creation"
    const val APPLICATION_NAME = "Shary"

    // Network (HTTP, SMTP)
    const val SMTP_SERVER = "smtp.gmail.com"
    const val BACKEND_HOST = "localhost"
    const val BACKEND_PORT = 5001
    const val BACKEND_APP_ID = "shary-21b61"
    const val NAME_GC_LOCATION_HOST = "us-central1"
    const val COLLECTION_SHARE_NAME = "sharing"
    const val SMTP_SSL_PORT = 465
    const val SMTP_TLS_PORT = 587
    const val baseEndpoint = "http://$BACKEND_HOST:$BACKEND_PORT/$BACKEND_APP_ID/$NAME_GC_LOCATION_HOST"
    const val endpointGetPubKey = "$baseEndpoint/get_pubKey"
    const val endpointStoreUser = "$baseEndpoint/store_user"
    const val endpointDeleteUser = "$baseEndpoint/delete_user"
    const val endpointSendData = "$baseEndpoint/store_payload"
    const val endpointPing = "$baseEndpoint/ping"

    // Paths
    const val PATH_DB = "./shary_demo.db"
    const val PATH_PRIVATE_KEY = "./data/authentication/private_key.pem"
    const val PATH_PUBLIC_KEY = "./data/authentication/public_key.pem"
    const val PATH_SECRET_KEY = "./data/authentication/secret.txt"
    const val PATH_AUTH_SIGNATURE = "./data/authentication/"
    const val PATH_ENV_VARIABLES = "./data/authentication/.env"

    // KV file paths (Layout schemas)
    const val PATH_SCHEMA_FIELD = "./ui_layouts/field.kv"
    const val PATH_SCHEMA_USER = "./ui_layouts/user.kv"
    const val PATH_SCHEMA_LOGIN = "./ui_layouts/login.kv"
    const val PATH_SCHEMA_REQUEST = "./ui_layouts/request.kv"
    const val PATH_SCHEMA_USER_CREATION = "./ui_layouts/user_creation.kv"
    const val PATH_SCHEMA_FILE_VISUALIZER = "./ui_layouts/file_visualizer.kv"
    const val PATH_SCHEMA_FIELD_DIALOG = "./ui_layouts/field_dialog.kv"
    const val PATH_SCHEMA_SEND_EMAIL_DIALOG = "./ui_layouts/send_email_dialog.kv"
    const val PATH_SCHEMA_SELECT_CHANNEL_DIALOG = "./ui_layouts/select_channel_dialog.kv"
    const val PATH_SCHEMA_USER_DIALOG = "./ui_layouts/user_dialog.kv"
    const val PATH_SCHEMA_REQUEST_DIALOG = "./ui_layouts/request_dialog.kv"

    // Data directories
    const val PATH_AUTHENTICATION = "data/authentication/"
    const val PATH_DOWNLOAD = "data/download"

    // Others
    val KV_PATHS_OTHERS = arrayOf(
        PATH_SCHEMA_FIELD,
        PATH_SCHEMA_USER,
        PATH_SCHEMA_REQUEST,
        PATH_SCHEMA_FILE_VISUALIZER
    )

    // Predefined messages
    const val MSG_DEFAULT_SEND_FILENAME = "shary_fields_from_"
    const val MSG_DEFAULT_REQUEST_FILENAME = "shary_fields_request_from_"
    const val MSG_DELETION_WARNING = "Are you sure you want to delete these fields?\n"

    // Debug and Testing
    const val CONTINUE_FOR_TESTING = true
    }

