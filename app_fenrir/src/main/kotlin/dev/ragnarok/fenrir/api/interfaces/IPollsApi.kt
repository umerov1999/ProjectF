package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.VKApiPoll
import dev.ragnarok.fenrir.api.model.VKApiUser
import io.reactivex.rxjava3.core.Single

interface IPollsApi {
    @CheckResult
    fun create(
        question: String?,
        isAnonymous: Boolean?,
        isMultiple: Boolean?,
        disableUnvote: Boolean,
        backgroundId: Int?,
        ownerId: Long,
        addAnswers: List<String>
    ): Single<VKApiPoll>

    @CheckResult
    fun deleteVote(ownerId: Long?, pollId: Int, answerId: Long, isBoard: Boolean?): Single<Boolean>

    @CheckResult
    fun addVote(
        ownerId: Long,
        pollId: Int,
        answerIds: Set<Long>,
        isBoard: Boolean?
    ): Single<Boolean>

    @CheckResult
    fun getById(ownerId: Long, isBoard: Boolean?, pollId: Int): Single<VKApiPoll>

    @CheckResult
    fun getVoters(
        ownerId: Long,
        pollId: Int,
        isBoard: Int?,
        answer_ids: List<Long>,
        offset: Int?, count: Int?
    ): Single<List<VKApiUser>>

    @CheckResult
    fun getBackgrounds(): Single<List<VKApiPoll.Background>>
}