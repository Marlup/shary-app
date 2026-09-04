package com.shary.app.core.domain.types.enums

enum class ConnectorCapability(val wireName: String) {
    VAULT_ACCESS("vaults.list"),
    FIELDS_READ("fields.metadata.read"),
    FIELDS_VALUE_READ("fields.value.read"),
    FIELDS_WRITE("fields.write"),
    USERS_READ("users.read"),
    USERS_WRITE("users.write"),
    ACTIONS_PREVIEW("actions.preview"),
    ACTIONS_COMMIT("actions.commit")
}
