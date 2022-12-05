package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.db.column.GroupColumns
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.domain.ICommunitiesInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapCommunities
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformCommunities
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildCommunitiesFromDbos
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class CommunitiesInteractor(private val networker: INetworker, private val stores: IStorages) :
    ICommunitiesInteractor {
    override fun getCachedData(accountId: Int, userId: Int): Single<List<Community>> {
        return stores.relativeship()
            .getCommunities(accountId, userId)
            .map { obj -> buildCommunitiesFromDbos(obj) }
    }

    override fun getActual(
        accountId: Int,
        userId: Int,
        count: Int,
        offset: Int,
        store: Boolean
    ): Single<List<Community>> {
        return networker.vkDefault(accountId)
            .groups()[userId, true, null, GroupColumns.API_FIELDS, offset, count]
            .flatMap { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                val dbos = mapCommunities(dtos)
                if (store) {
                    stores.relativeship()
                        .storeComminities(accountId, dbos, userId, offset == 0)
                        .andThen(Single.just(buildCommunitiesFromDbos(dbos)))
                } else {
                    Single.just(buildCommunitiesFromDbos(dbos))
                }
            }
    }

    override fun search(
        accountId: Int,
        q: String?,
        type: String?,
        countryId: Int?,
        cityId: Int?,
        futureOnly: Boolean?,
        sort: Int?,
        count: Int,
        offset: Int
    ): Single<List<Community>> {
        return networker.vkDefault(accountId)
            .groups()
            .search(
                q,
                type,
                GroupColumns.API_FIELDS,
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

    override fun join(accountId: Int, groupId: Int): Completable {
        return networker.vkDefault(accountId)
            .groups()
            .join(groupId, null)
            .ignoreElement()
    }

    override fun leave(accountId: Int, groupId: Int): Completable {
        return networker.vkDefault(accountId)
            .groups()
            .leave(groupId)
            .ignoreElement()
    }
}