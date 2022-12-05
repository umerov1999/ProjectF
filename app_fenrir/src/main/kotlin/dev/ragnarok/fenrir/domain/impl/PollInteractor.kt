package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.domain.IPollInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformUsers
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.User
import io.reactivex.rxjava3.core.Single

class PollInteractor(private val networker: INetworker) : IPollInteractor {
    override fun createPoll(
        accountId: Int,
        question: String?,
        anon: Boolean,
        multiple: Boolean,
        ownerId: Int,
        options: List<String>
    ): Single<Poll> {
        return networker.vkDefault(accountId)
            .polls()
            .create(question, anon, multiple, ownerId, options)
            .map { transform(it) }
    }

    override fun addVote(accountId: Int, poll: Poll, answerIds: Set<Long>): Single<Poll> {
        return networker.vkDefault(accountId)
            .polls()
            .addVote(poll.ownerId, poll.id, answerIds, poll.isBoard)
            .flatMap {
                getPollById(
                    accountId,
                    poll.ownerId,
                    poll.id,
                    poll.isBoard
                )
            }
    }

    override fun removeVote(accountId: Int, poll: Poll, answerId: Long): Single<Poll> {
        return networker.vkDefault(accountId)
            .polls()
            .deleteVote(poll.ownerId, poll.id, answerId, poll.isBoard)
            .flatMap {
                getPollById(
                    accountId,
                    poll.ownerId,
                    poll.id,
                    poll.isBoard
                )
            }
    }

    override fun getPollById(
        accountId: Int,
        ownerId: Int,
        pollId: Int,
        isBoard: Boolean
    ): Single<Poll> {
        return networker.vkDefault(accountId)
            .polls()
            .getById(ownerId, isBoard, pollId)
            .map {
                transform(
                    it
                ).setBoard(isBoard)
            }
    }

    override fun getVoters(
        accountId: Int,
        ownerId: Int,
        pollId: Int,
        isBoard: Int?,
        answer_ids: List<Long>,
        offset: Int?,
        count: Int?
    ): Single<List<User>> {
        return networker.vkDefault(accountId)
            .polls()
            .getVoters(ownerId, pollId, isBoard, answer_ids, offset, count)
            .map {
                transformUsers(
                    it
                )
            }
    }
}