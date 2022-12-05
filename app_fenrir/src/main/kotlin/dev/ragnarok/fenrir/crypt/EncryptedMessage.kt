package dev.ragnarok.fenrir.crypt

class EncryptedMessage(
    val sessionId: Long, val originalBody: String, @KeyLocationPolicy val
    KeyLocationPolicy: Int
)