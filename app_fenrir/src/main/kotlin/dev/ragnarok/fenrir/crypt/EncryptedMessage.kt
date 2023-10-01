package dev.ragnarok.fenrir.crypt

class EncryptedMessage(
    val sessionId: Long, val originalText: String, @KeyLocationPolicy val
    KeyLocationPolicy: Int
)