package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.longpoll.VkApiGroupLongpollUpdates
import dev.ragnarok.fenrir.api.model.longpoll.VkApiLongpollUpdates
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class ILongpollUpdatesService : IServiceRest() {
    fun getUpdates(
        server: String,
        act: String?,
        key: String?,
        ts: Long,
        wait: Int,
        mode: Int,
        version: Int
    ): Single<VkApiLongpollUpdates> {
        return rest.requestFullUrl(
            server,
            form(
                "act" to act,
                "key" to key,
                "ts" to ts,
                "wait" to wait,
                "mode" to mode,
                "version" to version
            ),
            VkApiLongpollUpdates.serializer()
        )
    }

    fun getGroupUpdates(
        server: String,
        act: String?,
        key: String?,
        ts: String?,
        wait: Int
    ): Single<VkApiGroupLongpollUpdates> {
        return rest.requestFullUrl(
            server,
            form("act" to act, "key" to key, "ts" to ts, "wait" to wait),
            VkApiGroupLongpollUpdates.serializer()
        )
    }
}