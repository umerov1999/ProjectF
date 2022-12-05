package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.NotificationsResponse
import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface INotificationsService {
    @POST("notifications.markAsViewed")
    fun markAsViewed(): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("notifications.get")
    operator fun get(
        @Field("count") count: Int?,
        @Field("start_from") startFrom: String?,
        @Field("filters") filters: String?,
        @Field("start_time") startTime: Long?,
        @Field("end_time") endTime: Long?
    ): Single<BaseResponse<NotificationsResponse>>

    @FormUrlEncoded
    @POST("notifications.get")
    fun getOfficial(
        @Field("count") count: Int?,
        @Field("start_from") startFrom: Int?,
        @Field("filters") filters: String?,
        @Field("start_time") startTime: Long?,
        @Field("end_time") endTime: Long?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<FeedbackVKOfficialList>>

    @FormUrlEncoded
    @POST("notifications.hide")
    fun hide(@Field("query") query: String?): Single<BaseResponse<Int>>
}