package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.domain.ICommunitiesInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapCommunities
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformCommunities
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildCommunitiesFromDbos
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.ignoreElement
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

class CommunitiesInteractor(private val networker: INetworker, private val stores: IStorages) :
    ICommunitiesInteractor {
    override fun getCachedData(accountId: Long, userId: Long): Flow<List<Community>> {
        return stores.relativeship()
            .getCommunities(accountId, userId)
            .map { obj -> buildCommunitiesFromDbos(obj) }
    }

    override fun getActual(
        accountId: Long,
        userId: Long,
        count: Int,
        offset: Int,
        store: Boolean
    ): Flow<List<Community>> {
        return networker.vkDefault(accountId)
            .groups()[userId, true, null, Fields.FIELDS_BASE_GROUP, offset, count]
            .flatMapConcat { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                val dbos = mapCommunities(dtos)
                if (store) {
                    stores.relativeship()
                        .storeCommunities(accountId, dbos, userId, offset == 0)
                        .map {
                            buildCommunitiesFromDbos(dbos)
                        }
                } else {
                    toFlow(buildCommunitiesFromDbos(dbos))
                }
            }
    }

    override fun search(
        accountId: Long,
        q: String?,
        type: String?,
        countryId: Int?,
        cityId: Int?,
        futureOnly: Boolean?,
        sort: Int?,
        count: Int,
        offset: Int
    ): Flow<List<Community>> {
        return networker.vkDefault(accountId)
            .groups()
            .search(
                q,
                type,
                Fields.FIELDS_BASE_GROUP,
                countryId,
                cityId,
                futureOnly,
                null,
                sort,
                offset,
                count
            )
            .map { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                transformCommunities(dtos)
            }
    }

    override fun join(accountId: Long, groupId: Long): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .groups()
            .join(groupId, null)
            .ignoreElement()
    }

    override fun leave(accountId: Long, groupId: Long): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .groups()
            .leave(groupId)
            .ignoreElement()
    }
}