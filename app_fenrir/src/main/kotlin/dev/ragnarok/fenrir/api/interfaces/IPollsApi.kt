package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.VKApiPoll
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.coroutines.flow.Flow

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
    ): Flow<VKApiPoll>

    @CheckResult
    fun deleteVote(ownerId: Long?, pollId: Int, answerId: Long, isBoard: Boolean?): Flow<Boolean>

    @CheckResult
    fun addVote(
        ownerId: Long,
        pollId: Int,
        answerIds: Set<Long>,
        isBoard: Boolean?
    ): Flow<Boolean>

    @CheckResult
    fun getById(ownerId: Long, isBoard: Boolean?, pollId: Int): Flow<VKApiPoll>

    @CheckResult
    fun getVoters(
        ownerId: Long,
        pollId: Int,
        isBoard: Int?,
        answer_ids: List<Long>,
        offset: Int?, count: Int?
    ): Flow<List<VKApiUser>>

    @CheckResult
    fun getBackgrounds(): Flow<List<VKApiPoll.Background>>
}