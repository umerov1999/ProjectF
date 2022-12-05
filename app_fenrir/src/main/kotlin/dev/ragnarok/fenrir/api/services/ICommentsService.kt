package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.CustomCommentsResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ICommentsService {
    @FormUrlEncoded
    @POST("execute")
    operator fun get(
        @Field("code") code: String?,
        @Field("source_type") sourceType: String?,
        @Field("owner_id") ownerId: Int,
        @Field("source_id") sourceId: Int,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("sort") sort: String?,
        @Field("start_comment_id") startCommentId: Int?,
        @Field("comment_id") thread_id: Int,
        @Field("access_key") accessKey: String?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<CustomCommentsResponse>>
}