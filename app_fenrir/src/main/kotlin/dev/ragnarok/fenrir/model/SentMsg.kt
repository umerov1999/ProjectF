package dev.ragnarok.fenrir.model

class SentMsg(
    val dbid: Int,
    val vkid: Int,
    val peerId: Long,
    val conversation_message_id: Int,
    val accountId: Long
)