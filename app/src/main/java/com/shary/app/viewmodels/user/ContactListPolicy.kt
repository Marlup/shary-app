package com.shary.app.viewmodels.user

import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.ContactSortByParameter

fun filterUsersByQuery(users: List<UserDomain>, query: String): List<UserDomain> {
    val q = query.trim()
    if (q.isBlank()) return users
    return users.filter { user ->
        user.username.contains(q, ignoreCase = true) ||
            user.email.contains(q, ignoreCase = true)
    }
}

fun filterUsersByTag(users: List<UserDomain>, activeFilter: String): List<UserDomain> {
    if (activeFilter == "All") return users
    return users.filter { user ->
        user.tag.toTagString().equals(activeFilter, ignoreCase = true)
    }
}

fun sortUsers(
    users: List<UserDomain>,
    param: ContactSortByParameter,
    ascending: Boolean
): List<UserDomain> {
    val comparator = compareBy<UserDomain> {
        when (param) {
            ContactSortByParameter.NAME -> it.username.lowercase()
            ContactSortByParameter.TAG -> it.tag.toTagString().lowercase()
            ContactSortByParameter.DATE_ADDED -> it.dateAdded
        }
    }
    return users.sortedWith(if (ascending) comparator else comparator.reversed())
}
