package dev.ragnarok.fenrir.longpoll

import dev.ragnarok.fenrir.api.model.longpoll.VKApiLongpollUpdates
import kotlinx.coroutines.flow.Flow

interface ILongpollManager {
    fun forceDestroy(accountId: Long)
    fun observe(): Flow<VKApiLongpollUpdates>
    fun observeKeepAlive(): Flow<Long>
    fun keepAlive(accountId: Long)
}