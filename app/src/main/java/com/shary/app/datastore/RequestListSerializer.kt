package com.shary.app.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.shary.app.RequestList
import java.io.InputStream
import java.io.OutputStream

object RequestListSerializer : Serializer<RequestList> {
    override val defaultValue: RequestList = RequestList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): RequestList {
        try {
            return RequestList.parseFrom(input)
        } catch (exception: Exception) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: RequestList, output: OutputStream) {
        t.writeTo(output)
    }
}