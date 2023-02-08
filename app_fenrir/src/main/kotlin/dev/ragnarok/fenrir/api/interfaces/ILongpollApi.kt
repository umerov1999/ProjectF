package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.api.model.longpoll.VKApiGroupLongpollUpdates
import dev.ragnarok.fenrir.api.model.longpoll.VKApiLongpollUpdates
import io.reactivex.rxjava3.core.Single

interface ILongpollApi {
    fun getUpdates(
        server: String,
        key: String?,
        ts: Long,
        wait: Long,
        mode: Int,
        version: Int
    ): Single<VKApiLongpollUpdates>

    fun getGroupUpdates(
        server: String,
        key: String?,
        ts: String?,
        wait: Long
    ): Single<VKApiGroupLongpollUpdates>
}