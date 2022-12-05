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
        ownerId: Int,
        addAnswers: List<String>
    ): Single<VKApiPoll>

    @CheckResult
    fun deleteVote(ownerId: Int?, pollId: Int, answerId: Long, isBoard: Boolean?): Single<Boolean>

    @CheckResult
    fun addVote(
        ownerId: Int,
        pollId: Int,
        answerIds: Set<Long>,
        isBoard: Boolean?
    ): Single<Boolean>

    @CheckResult
    fun getById(ownerId: Int, isBoard: Boolean?, pollId: Int): Single<VKApiPoll>

    @CheckResult
    fun getVoters(
        ownerId: Int,
        pollId: Int,
        isBoard: Int?,
        answer_ids: List<Long>,
        offset: Int?, count: Int?
    ): Single<List<VKApiUser>>
}