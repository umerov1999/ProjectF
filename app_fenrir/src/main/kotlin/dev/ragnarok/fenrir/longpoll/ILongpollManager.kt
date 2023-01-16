package dev.ragnarok.fenrir.longpoll

import dev.ragnarok.fenrir.api.model.longpoll.VkApiLongpollUpdates
import io.reactivex.rxjava3.core.Flowable

interface ILongpollManager {
    fun forceDestroy(accountId: Long)
    fun observe(): Flowable<VkApiLongpollUpdates>
    fun observeKeepAlive(): Flowable<Long>
    fun keepAlive(accountId: Long)
}