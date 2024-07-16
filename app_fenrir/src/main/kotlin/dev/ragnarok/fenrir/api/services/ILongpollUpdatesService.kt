package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.longpoll.VKApiGroupLongpollUpdates
import dev.ragnarok.fenrir.api.model.longpoll.VKApiLongpollUpdates
import dev.ragnarok.fenrir.api.rest.IServiceRest
import kotlinx.coroutines.flow.Flow

class ILongpollUpdatesService : IServiceRest() {
    fun getUpdates(
        server: String,
        act: String?,
        key: String?,
        ts: Long,
        wait: Long,
        mode: Int,
        version: Int
    ): Flow<VKApiLongpollUpdates> {
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
            VKApiLongpollUpdates.serializer()
        )
    }

    fun getGroupUpdates(
        server: String,
        act: String?,
        key: String?,
        ts: String?,
        wait: Long
    ): Flow<VKApiGroupLongpollUpdates> {
        return rest.requestFullUrl(
            server,
            form("act" to act, "key" to key, "ts" to ts, "wait" to wait),
            VKApiGroupLongpollUpdates.serializer()
        )
    }
}