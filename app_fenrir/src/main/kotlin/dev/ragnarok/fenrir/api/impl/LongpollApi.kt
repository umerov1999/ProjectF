package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IOtherVkRetrofitProvider
import dev.ragnarok.fenrir.api.interfaces.ILongpollApi
import dev.ragnarok.fenrir.api.model.longpoll.VkApiGroupLongpollUpdates
import dev.ragnarok.fenrir.api.model.longpoll.VkApiLongpollUpdates
import dev.ragnarok.fenrir.api.services.ILongpollUpdatesService
import io.reactivex.rxjava3.core.Single

class LongpollApi internal constructor(private val provider: IOtherVkRetrofitProvider) :
    ILongpollApi {
    override fun getUpdates(
        server: String?,
        key: String?,
        ts: Long,
        wait: Int,
        mode: Int,
        version: Int
    ): Single<VkApiLongpollUpdates> {
        return provider.provideLongpollRetrofit()
            .flatMap { wrapper ->
                wrapper.create(
                    ILongpollUpdatesService::class.java
                )
                    .getUpdates(server, "a_check", key, ts, wait, mode, version)
            }
    }

    override fun getGroupUpdates(
        server: String?,
        key: String?,
        ts: String?,
        wait: Int
    ): Single<VkApiGroupLongpollUpdates> {
        return provider.provideLongpollRetrofit()
            .flatMap { wrapper ->
                wrapper.create(
                    ILongpollUpdatesService::class.java
                )
                    .getGroupUpdates(server, "a_check", key, ts, wait)
            }
    }
}