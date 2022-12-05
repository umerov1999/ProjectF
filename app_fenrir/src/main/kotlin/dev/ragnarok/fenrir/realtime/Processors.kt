package dev.ragnarok.fenrir.realtime

object Processors {
    val realtimeMessages: IRealtimeMessagesProcessor by lazy {
        RealtimeMessagesProcessor()
    }
}