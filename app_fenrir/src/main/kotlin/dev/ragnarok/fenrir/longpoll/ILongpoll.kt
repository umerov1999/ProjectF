package dev.ragnarok.fenrir.longpoll

interface ILongpoll {
    val accountId: Int
    fun connect()
    fun shutdown()
}