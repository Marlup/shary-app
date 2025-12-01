package com.shary.app.infrastructure.persistance.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.shary.app.UserList
import java.io.InputStream
import java.io.OutputStream

object UserListSerializer : Serializer<UserList> {
    override val defaultValue: UserList = UserList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserList {
        try {
            return UserList.parseFrom(input)
        } catch (exception: Exception) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: UserList, output: OutputStream) {
        t.writeTo(output)
    }
}
