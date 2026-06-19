package com.shary.app.core.domain.types.enums

enum class ConnectorCapability(val wireName: String) {
    FIELDS_READ("fields.read"),
    FIELDS_WRITE("fields.write"),
    USERS_READ("users.read"),
    USERS_WRITE("users.write"),
    ACTIONS_PREVIEW("actions.preview"),
    ACTIONS_COMMIT("actions.commit")
}
