package com.shary.app.infrastructure.persistance.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.shary.app.FieldList

val Context.fieldListDataStore: DataStore<FieldList> by dataStore(
    fileName = "fields.pb",
    serializer = FieldListSerializer
)
