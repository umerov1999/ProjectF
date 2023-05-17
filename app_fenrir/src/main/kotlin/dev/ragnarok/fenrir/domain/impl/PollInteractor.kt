package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.domain.IPollInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformUsers
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.User
import io.reactivex.rxjava3.core.Single

class PollInteractor(private val networker: INetworker) : IPollInteractor {
    override fun createPoll(
        accountId: Long,
        question: String?,
        anon: Boolean,
        multiple: Boolean,
        disableUnvote: Boolean,
        backgroundId: Int?,
        ownerId: Long,
        options: List<String>
    ): Single<Poll> {
        return networker.vkDefault(accountId)
            .polls()
            .create(question, anon, multiple, disableUnvote, backgroundId, ownerId, options)
            .map { transform(it) }
    }

    override fun addVote(accountId: Long, poll: Poll, answerIds: Set<Long>): Single<Poll> {
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

    override fun removeVote(accountId: Long, poll: Poll, answerId: Long): Single<Poll> {
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

    override fun getBackgrounds(accountId: Long): Single<List<Poll.PollBackground>> {
        return networker.vkDefault(accountId)
            .polls()
            .getBackgrounds()
            .flatMap {
                val tmpList = ArrayList<Poll.PollBackground>(it.size + 1)
                tmpList.add(Poll.PollBackground(-1).setName("default"))
                for (i in it) {
                    val s = Dto2Model.buildPollBackgroundGradient(i)
                    if (s != null) {
                        tmpList.add(s)
                    }
                }
                Single.just(tmpList)
            }
    }

    override fun getPollById(
        accountId: Long,
        ownerId: Long,
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
        accountId: Long,
        ownerId: Long,
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