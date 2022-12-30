package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.VKApiPoll
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.PollUsersResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IPollsService : IServiceRest() {
    fun create(
        question: String?,
        isAnonymous: Int?,
        isMultiple: Int?,
        ownerId: Int?,
        addAnswers: String?
    ): Single<BaseResponse<VKApiPoll>> {
        return rest.request(
            "polls.create", form(
                "question" to question,
                "is_anonymous" to isAnonymous,
                "is_multiple" to isMultiple,
                "owner_id" to ownerId,
                "add_answers" to addAnswers
            ), base(VKApiPoll.serializer())
        )
    }

    //https://vk.com/dev/polls.deleteVote
    fun deleteVote(
        ownerId: Int?,
        pollId: Int,
        answerId: Long,
        isBoard: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "polls.deleteVote", form(
                "owner_id" to ownerId,
                "poll_id" to pollId,
                "answer_id" to answerId,
                "is_board" to isBoard
            ), baseInt
        )
    }

    //https://vk.com/dev/polls.addVote
    fun addVote(
        ownerId: Int?,
        pollId: Int,
        answerIds: String?,
        isBoard: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "polls.addVote", form(
                "owner_id" to ownerId,
                "poll_id" to pollId,
                "answer_ids" to answerIds,
                "is_board" to isBoard
            ), baseInt
        )
    }

    fun getById(
        ownerId: Int?,
        isBoard: Int?,
        pollId: Int?
    ): Single<BaseResponse<VKApiPoll>> {
        return rest.request(
            "polls.getById", form(
                "owner_id" to ownerId,
                "is_board" to isBoard,
                "poll_id" to pollId
            ), base(VKApiPoll.serializer())
        )
    }

    fun getVoters(
        ownerId: Int,
        pollId: Int,
        isBoard: Int?,
        answer_ids: String,
        offset: Int?,
        count: Int?,
        fields: String?,
        nameCase: String?
    ): Single<BaseResponse<List<PollUsersResponse>>> {
        return rest.request(
            "polls.getVoters", form(
                "owner_id" to ownerId,
                "poll_id" to pollId,
                "is_board" to isBoard,
                "answer_ids" to answer_ids,
                "offset" to offset,
                "count" to count,
                "fields" to fields,
                "name_case" to nameCase
            ), baseList(PollUsersResponse.serializer())
        )
    }
}