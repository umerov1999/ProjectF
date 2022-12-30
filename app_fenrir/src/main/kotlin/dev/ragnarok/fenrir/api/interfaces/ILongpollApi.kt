package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.api.model.longpoll.VkApiGroupLongpollUpdates
import dev.ragnarok.fenrir.api.model.longpoll.VkApiLongpollUpdates
import io.reactivex.rxjava3.core.Single

interface ILongpollApi {
    fun getUpdates(
        server: String,
        key: String?,
        ts: Long,
        wait: Int,
        mode: Int,
        version: Int
    ): Single<VkApiLongpollUpdates>

    fun getGroupUpdates(
        server: String,
        key: String?,
        ts: String?,
        wait: Int
    ): Single<VkApiGroupLongpollUpdates>
}