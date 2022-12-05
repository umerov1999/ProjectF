package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.BaseResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IStatusService {
    /**
     * Sets a new status for the current user.
     *
     * @param text    Text of the new status.
     * @param groupId Identifier of a community to set a status in. If left blank the status is set to current user.
     * @return 1
     */
    @FormUrlEncoded
    @POST("status.set")
    operator fun set(
        @Field("text") text: String?,
        @Field("group_id") groupId: Int?
    ): Single<BaseResponse<Int>>
}