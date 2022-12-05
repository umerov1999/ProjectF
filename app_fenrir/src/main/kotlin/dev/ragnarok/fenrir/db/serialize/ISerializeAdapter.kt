package dev.ragnarok.fenrir.db.serialize

interface ISerializeAdapter<T> {
    fun deserialize(raw: ByteArray): T
    fun serialize(data: T): ByteArray
}