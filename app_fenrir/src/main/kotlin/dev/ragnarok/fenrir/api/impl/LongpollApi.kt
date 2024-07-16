package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IOtherVKRestProvider
import dev.ragnarok.fenrir.api.interfaces.ILongpollApi
import dev.ragnarok.fenrir.api.model.longpoll.VKApiGroupLongpollUpdates
import dev.ragnarok.fenrir.api.model.longpoll.VKApiLongpollUpdates
import dev.ragnarok.fenrir.api.services.ILongpollUpdatesService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat

class LongpollApi internal constructor(private val provider: IOtherVKRestProvider) :
    ILongpollApi {
    override fun getUpdates(
        server: String,
        key: String?,
        ts: Long,
        wait: Long,
        mode: Int,
        version: Int
    ): Flow<VKApiLongpollUpdates> {
        return provider.provideLongpollRest()
            .flatMapConcat {
                val ret = ILongpollUpdatesService()
                ret.addon(it)
                ret.getUpdates(server, "a_check", key, ts, wait, mode, version)
            }
    }

    override fun getGroupUpdates(
        server: String,
        key: String?,
        ts: String?,
        wait: Long
    ): Flow<VKApiGroupLongpollUpdates> {
        return provider.provideLongpollRest()
            .flatMapConcat {
                val ret = ILongpollUpdatesService()
                ret.addon(it)
                ret.getGroupUpdates(server, "a_check", key, ts, wait)
            }
    }
}