package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.User
import kotlinx.coroutines.flow.Flow

interface IPollInteractor {
    fun createPoll(
        accountId: Long,
        question: String?,
        anon: Boolean,
        multiple: Boolean,
        disableUnvote: Boolean,
        backgroundId: Int?,
        ownerId: Long,
        options: List<String>
    ): Flow<Poll>

    fun addVote(accountId: Long, poll: Poll, answerIds: Set<Long>): Flow<Poll>
    fun removeVote(accountId: Long, poll: Poll, answerId: Long): Flow<Poll>
    fun getPollById(accountId: Long, ownerId: Long, pollId: Int, isBoard: Boolean): Flow<Poll>
    fun getVoters(
        accountId: Long,
        ownerId: Long,
        pollId: Int,
        isBoard: Int?,
        answer_ids: List<Long>,
        offset: Int?,
        count: Int?
    ): Flow<List<User>>

    fun getBackgrounds(accountId: Long): Flow<List<Poll.PollBackground>>
}