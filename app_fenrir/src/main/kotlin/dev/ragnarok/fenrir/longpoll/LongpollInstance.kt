package dev.ragnarok.fenrir.longpoll

import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.realtime.Processors.realtimeMessages

object LongpollInstance {
    val longpollManager: ILongpollManager by lazy {
        AndroidLongpollManager(networkInterfaces, realtimeMessages)
    }
}