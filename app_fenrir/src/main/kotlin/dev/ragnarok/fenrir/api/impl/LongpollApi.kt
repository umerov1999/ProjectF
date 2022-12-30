package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IOtherVkRestProvider
import dev.ragnarok.fenrir.api.interfaces.ILongpollApi
import dev.ragnarok.fenrir.api.model.longpoll.VkApiGroupLongpollUpdates
import dev.ragnarok.fenrir.api.model.longpoll.VkApiLongpollUpdates
import dev.ragnarok.fenrir.api.services.ILongpollUpdatesService
import io.reactivex.rxjava3.core.Single

class LongpollApi internal constructor(private val provider: IOtherVkRestProvider) :
    ILongpollApi {
    override fun getUpdates(
        server: String,
        key: String?,
        ts: Long,
        wait: Int,
        mode: Int,
        version: Int
    ): Single<VkApiLongpollUpdates> {
        return provider.provideLongpollRest()
            .flatMap {
                val ret = ILongpollUpdatesService()
                ret.addon(it)
                ret.getUpdates(server, "a_check", key, ts, wait, mode, version)
            }
    }

    override fun getGroupUpdates(
        server: String,
        key: String?,
        ts: String?,
        wait: Int
    ): Single<VkApiGroupLongpollUpdates> {
        return provider.provideLongpollRest()
            .flatMap {
                val ret = ILongpollUpdatesService()
                ret.addon(it)
                ret.getGroupUpdates(server, "a_check", key, ts, wait)
            }
    }
}