package dev.ragnarok.fenrir.db.interfaces

interface Cancelable {
    suspend fun canceled(): Boolean
}