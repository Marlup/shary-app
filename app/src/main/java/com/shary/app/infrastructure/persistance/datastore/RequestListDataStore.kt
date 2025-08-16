package com.shary.app.infrastructure.persistance.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.shary.app.RequestList

val Context.requestListDataStore: DataStore<RequestList> by dataStore(
    fileName = "requests.pb",
    serializer = RequestListSerializer
)
