package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.longpoll.VkApiGroupLongpollUpdates
import dev.ragnarok.fenrir.api.model.longpoll.VkApiLongpollUpdates
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface ILongpollUpdatesService {
    @GET
    fun getUpdates(
        @Url server: String?,
        @Query("act") act: String?,
        @Query("key") key: String?,
        @Query("ts") ts: Long,
        @Query("wait") wait: Int,
        @Query("mode") mode: Int,
        @Query("version") version: Int
    ): Single<VkApiLongpollUpdates>

    @GET
    fun getGroupUpdates(
        @Url server: String?,
        @Query("act") act: String?,
        @Query("key") key: String?,
        @Query("ts") ts: String?,
        @Query("wait") wait: Int
    ): Single<VkApiGroupLongpollUpdates>
}