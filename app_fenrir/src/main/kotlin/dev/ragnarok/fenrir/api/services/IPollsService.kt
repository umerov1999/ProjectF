package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.VKApiPoll
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.PollUsersResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IPollsService {
    @FormUrlEncoded
    @POST("polls.create")
    fun create(
        @Field("question") question: String?,
        @Field("is_anonymous") isAnonymous: Int?,
        @Field("is_multiple") isMultiple: Int?,
        @Field("owner_id") ownerId: Int?,
        @Field("add_answers") addAnswers: String?
    ): Single<BaseResponse<VKApiPoll>>

    //https://vk.com/dev/polls.deleteVote
    @FormUrlEncoded
    @POST("polls.deleteVote")
    fun deleteVote(
        @Field("owner_id") ownerId: Int?,
        @Field("poll_id") pollId: Int,
        @Field("answer_id") answerId: Long,
        @Field("is_board") isBoard: Int?
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/polls.addVote
    @FormUrlEncoded
    @POST("polls.addVote")
    fun addVote(
        @Field("owner_id") ownerId: Int?,
        @Field("poll_id") pollId: Int,
        @Field("answer_ids") answerIds: String?,
        @Field("is_board") isBoard: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("polls.getById")
    fun getById(
        @Field("owner_id") ownerId: Int?,
        @Field("is_board") isBoard: Int?,
        @Field("poll_id") pollId: Int?
    ): Single<BaseResponse<VKApiPoll>>

    @FormUrlEncoded
    @POST("polls.getVoters")
    fun getVoters(
        @Field("owner_id") ownerId: Int,
        @Field("poll_id") pollId: Int,
        @Field("is_board") isBoard: Int?,
        @Field("answer_ids") answer_ids: String,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("fields") fields: String?,
        @Field("name_case") nameCase: String?
    ): Single<BaseResponse<List<PollUsersResponse>>>
}