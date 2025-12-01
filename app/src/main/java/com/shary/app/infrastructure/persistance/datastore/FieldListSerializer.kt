package com.shary.app.infrastructure.persistance.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.shary.app.FieldList
import java.io.InputStream
import java.io.OutputStream

object FieldListSerializer : Serializer<FieldList> {
    override val defaultValue: FieldList = FieldList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): FieldList {
        try {
            return FieldList.parseFrom(input)
        } catch (exception: Exception) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: FieldList, output: OutputStream) {
        t.writeTo(output)
    }
}
