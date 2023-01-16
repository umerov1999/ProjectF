package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.db.model.entity.TopicDboEntity
import dev.ragnarok.fenrir.domain.IBoardInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.buildTopicDbo
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapOwners
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformOwners
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildTopicFromDbo
import dev.ragnarok.fenrir.model.Topic
import dev.ragnarok.fenrir.model.criteria.TopicsCriteria
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.VKOwnIds
import io.reactivex.rxjava3.core.Single
import kotlin.math.abs

class BoardInteractor(
    private val networker: INetworker,
    private val stores: IStorages,
    private val ownersRepository: IOwnersRepository
) : IBoardInteractor {
    override fun getCachedTopics(accountId: Long, ownerId: Long): Single<List<Topic>> {
        val criteria = TopicsCriteria(accountId, ownerId)
        return stores.topics()
            .getByCriteria(criteria)
            .flatMap { dbos ->
                val ids = VKOwnIds()
                for (dbo in dbos) {
                    ids.append(dbo.creatorId)
                    ids.append(dbo.updatedBy)
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ids.all,
                    IOwnersRepository.MODE_ANY
                )
                    .map<List<Topic>> {
                        val topics: MutableList<Topic> = ArrayList(dbos.size)
                        for (dbo in dbos) {
                            topics.add(buildTopicFromDbo(dbo, it))
                        }
                        topics
                    }
            }
    }

    //public static final int ORDER_DESCENDING_UPDATE_TIME = 1;
    //public static final int ORDER_DESCENDING_CREATE_TIME = 2;
    //public static final int ORDER_ASCENDING_UPDATE_TIME = -1;
    //public static final int ORDER_ASCENDING_CREATE_TIME = -2;
    override fun getActualTopics(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Single<List<Topic>> {
        return networker.vkDefault(accountId)
            .board()
            .getTopics(
                abs(ownerId),
                null,
                null,
                offset,
                count,
                true,
                null,
                null,
                Fields.FIELDS_BASE_OWNER
            )
            .flatMap { response ->
                val dtos = listEmptyIfNull(response.items)
                val dbos: MutableList<TopicDboEntity> = ArrayList(dtos.size)
                for (dto in dtos) {
                    dbos.add(buildTopicDbo(dto))
                }
                val ownerEntities = mapOwners(response.profiles, response.groups)
                val ownerIds = VKOwnIds()
                for (dbo in dbos) {
                    ownerIds.append(dbo.creatorId)
                    ownerIds.append(dbo.updatedBy)
                }
                val owners = transformOwners(response.profiles, response.groups)
                stores.topics()
                    .store(
                        accountId,
                        ownerId,
                        dbos,
                        ownerEntities,
                        response.canAddTopics == 1,
                        response.defaultOrder,
                        offset == 0
                    )
                    .andThen(
                        ownersRepository.findBaseOwnersDataAsBundle(
                            accountId,
                            ownerIds.all,
                            IOwnersRepository.MODE_ANY,
                            owners
                        )
                            .map<List<Topic>> {
                                val topics: MutableList<Topic> = ArrayList(dbos.size)
                                for (dbo in dbos) {
                                    topics.add(buildTopicFromDbo(dbo, it))
                                }
                                topics
                            })
            }
    }
}