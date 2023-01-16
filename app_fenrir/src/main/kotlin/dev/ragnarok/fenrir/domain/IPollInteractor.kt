package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.User
import io.reactivex.rxjava3.core.Single

interface IPollInteractor {
    fun createPoll(
        accountId: Long,
        question: String?,
        anon: Boolean,
        multiple: Boolean,
        ownerId: Long,
        options: List<String>
    ): Single<Poll>

    fun addVote(accountId: Long, poll: Poll, answerIds: Set<Long>): Single<Poll>
    fun removeVote(accountId: Long, poll: Poll, answerId: Long): Single<Poll>
    fun getPollById(accountId: Long, ownerId: Long, pollId: Int, isBoard: Boolean): Single<Poll>
    fun getVoters(
        accountId: Long,
        ownerId: Long,
        pollId: Int,
        isBoard: Int?,
        answer_ids: List<Long>,
        offset: Int?,
        count: Int?
    ): Single<List<User>>
}