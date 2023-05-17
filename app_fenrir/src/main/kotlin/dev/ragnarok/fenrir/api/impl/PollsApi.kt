package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IPollsApi
import dev.ragnarok.fenrir.api.model.VKApiPoll
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.services.IPollsService
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.serializeble.json.Json
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

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
    ): Single<VKApiPoll> {
        return provideService(IPollsService(), TokenType.USER)
            .flatMap { service ->
                service
                    .create(
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
    ): Single<Boolean> {
        return provideService(IPollsService(), TokenType.USER)
            .flatMap { service ->
                service.deleteVote(ownerId, pollId, answerId, integerFromBoolean(isBoard))
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun addVote(
        ownerId: Long,
        pollId: Int,
        answerIds: Set<Long>,
        isBoard: Boolean?
    ): Single<Boolean> {
        return provideService(IPollsService(), TokenType.USER)
            .flatMap { service ->
                service.addVote(ownerId, pollId, join(answerIds, ","), integerFromBoolean(isBoard))
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun getById(ownerId: Long, isBoard: Boolean?, pollId: Int): Single<VKApiPoll> {
        return provideService(IPollsService(), TokenType.USER)
            .flatMap { service ->
                service.getById(ownerId, integerFromBoolean(isBoard), pollId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getBackgrounds(): Single<List<VKApiPoll.Background>> {
        return provideService(IPollsService(), TokenType.USER)
            .flatMap { service ->
                service.getBackgrounds()
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getVoters(
        ownerId: Long,
        pollId: Int,
        isBoard: Int?,
        answer_ids: List<Long>, offset: Int?, count: Int?
    ): Single<List<VKApiUser>> {
        val ids = join(answer_ids, ",") { obj: Any -> obj.toString() } ?: return Single.just(
            emptyList()
        )
        return provideService(IPollsService(), TokenType.USER)
            .flatMap { service ->
                service.getVoters(
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