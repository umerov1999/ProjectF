package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IPollsApi
import dev.ragnarok.fenrir.api.model.VKApiPoll
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.services.IPollsService
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkInt
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyListFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal class PollsApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IPollsApi {
    override fun create(
        question: String?,
        isAnonymous: Boolean?,
        isMultiple: Boolean?,
        disableUnvote: Boolean,
        backgroundId: Int?,
        ownerId: Long,
        addAnswers: List<String>
    ): Flow<VKApiPoll> {
        return provideService(IPollsService(), TokenType.USER)
            .flatMapConcat {
                it.create(
                    question,
                    integerFromBoolean(isAnonymous),
                    integerFromBoolean(isMultiple),
                    integerFromBoolean(disableUnvote),
                    backgroundId,
                    ownerId,
                    Json.encodeToString(ListSerializer(String.serializer()), addAnswers)
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun deleteVote(
        ownerId: Long?,
        pollId: Int,
        answerId: Long,
        isBoard: Boolean?
    ): Flow<Boolean> {
        return provideService(IPollsService(), TokenType.USER)
            .flatMapConcat {
                it.deleteVote(ownerId, pollId, answerId, integerFromBoolean(isBoard))
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun addVote(
        ownerId: Long,
        pollId: Int,
        answerIds: Set<Long>,
        isBoard: Boolean?
    ): Flow<Boolean> {
        return provideService(IPollsService(), TokenType.USER)
            .flatMapConcat {
                it.addVote(ownerId, pollId, join(answerIds, ","), integerFromBoolean(isBoard))
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun getById(ownerId: Long, isBoard: Boolean?, pollId: Int): Flow<VKApiPoll> {
        return provideService(IPollsService(), TokenType.USER)
            .flatMapConcat {
                it.getById(ownerId, integerFromBoolean(isBoard), pollId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getBackgrounds(): Flow<List<VKApiPoll.Background>> {
        return provideService(IPollsService(), TokenType.USER)
            .flatMapConcat {
                it.getBackgrounds()
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getVoters(
        ownerId: Long,
        pollId: Int,
        isBoard: Int?,
        answer_ids: List<Long>, offset: Int?, count: Int?
    ): Flow<List<VKApiUser>> {
        val ids = join(answer_ids, ",") { it.toString() } ?: return emptyListFlow()
        return provideService(IPollsService(), TokenType.USER)
            .flatMapConcat { s ->
                s.getVoters(
                    ownerId,
                    pollId,
                    isBoard,
                    ids,
                    offset,
                    count,
                    Fields.FIELDS_BASE_USER,
                    null
                )
                    .map(extractResponseWithErrorHandling())
                    .map {
                        Utils.listEmptyIfNull(if (it.isEmpty()) null else it[0].users?.items)
                    }
            }
    }
}