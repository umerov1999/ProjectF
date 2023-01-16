package dev.ragnarok.fenrir.longpoll

interface ILongpoll {
    val accountId: Long
    fun connect()
    fun shutdown()
}