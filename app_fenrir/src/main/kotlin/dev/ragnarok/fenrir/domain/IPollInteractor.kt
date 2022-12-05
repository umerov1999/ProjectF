package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.User
import io.reactivex.rxjava3.core.Single

interface IPollInteractor {
    fun createPoll(
        accountId: Int,
        question: String?,
        anon: Boolean,
        multiple: Boolean,
        ownerId: Int,
        options: List<String>
    ): Single<Poll>

    fun addVote(accountId: Int, poll: Poll, answerIds: Set<Long>): Single<Poll>
    fun removeVote(accountId: Int, poll: Poll, answerId: Long): Single<Poll>
    fun getPollById(accountId: Int, ownerId: Int, pollId: Int, isBoard: Boolean): Single<Poll>
    fun getVoters(
        accountId: Int,
        ownerId: Int,
        pollId: Int,
        isBoard: Int?,
        answer_ids: List<Long>,
        offset: Int?,
        count: Int?
    ): Single<List<User>>
}