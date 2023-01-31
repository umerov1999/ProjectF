package dev.ragnarok.fenrir.longpoll

import dev.ragnarok.fenrir.api.model.longpoll.VKApiLongpollUpdates
import io.reactivex.rxjava3.core.Flowable

interface ILongpollManager {
    fun forceDestroy(accountId: Long)
    fun observe(): Flowable<VKApiLongpollUpdates>
    fun observeKeepAlive(): Flowable<Long>
    fun keepAlive(accountId: Long)
}