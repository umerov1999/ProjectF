package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.VKApiPoll
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.PollUsersResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import kotlinx.coroutines.flow.Flow

class IPollsService : IServiceRest() {
    fun create(
        question: String?,
        isAnonymous: Int?,
        isMultiple: Int?,
        disableUnvote: Int?,
        backgroundId: Int?,
        ownerId: Long?,
        addAnswers: String?
    ): Flow<BaseResponse<VKApiPoll>> {
        return rest.request(
            "polls.create", form(
                "question" to question,
                "is_anonymous" to isAnonymous,
                "is_multiple" to isMultiple,
                "disable_unvote" to disableUnvote,
                "background_id" to backgroundId,
                "owner_id" to ownerId,
                "add_answers" to addAnswers
            ), base(VKApiPoll.serializer())
        )
    }

    //https://vk.com/dev/polls.deleteVote
    fun deleteVote(
        ownerId: Long?,
        pollId: Int,
        answerId: Long,
        isBoard: Int?
    ): Flow<BaseResponse<Int>> {
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
        ownerId: Long?,
        pollId: Int,
        answerIds: String?,
        isBoard: Int?
    ): Flow<BaseResponse<Int>> {
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
        ownerId: Long?,
        isBoard: Int?,
        pollId: Int?
    ): Flow<BaseResponse<VKApiPoll>> {
        return rest.request(
            "polls.getById", form(
                "owner_id" to ownerId,
                "is_board" to isBoard,
                "poll_id" to pollId
            ), base(VKApiPoll.serializer())
        )
    }

    fun getVoters(
        ownerId: Long,
        pollId: Int,
        isBoard: Int?,
        answer_ids: String,
        offset: Int?,
        count: Int?,
        fields: String?,
        nameCase: String?
    ): Flow<BaseResponse<List<PollUsersResponse>>> {
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

    fun getBackgrounds(): Flow<BaseResponse<List<VKApiPoll.Background>>> {
        return rest.request(
            "polls.getBackgrounds", null, baseList(VKApiPoll.Background.serializer())
        )
    }
}