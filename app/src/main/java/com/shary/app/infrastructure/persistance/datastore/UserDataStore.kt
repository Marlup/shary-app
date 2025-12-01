package com.shary.app.infrastructure.persistance.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.shary.app.UserList

val Context.userListDataStore: DataStore<UserList> by dataStore(
    fileName = "users.pb",
    serializer = UserListSerializer
)
