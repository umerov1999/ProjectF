package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.domain.ILikesInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformOwner
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import io.reactivex.rxjava3.core.Single

class LikesInteractor(private val networker: INetworker) : ILikesInteractor {
    override fun getLikes(
        accountId: Long,
        type: String?,
        ownerId: Long,
        itemId: Int,
        filter: String?,
        count: Int,
        offset: Int
    ): Single<List<Owner>> {
        return networker.vkDefault(accountId)
            .likes()
            .getList(
                type,
                ownerId,
                itemId,
                null,
                filter,
                null,
                offset,
                count,
                null,
                Fields.FIELDS_BASE_OWNER
            )
            .map { response ->
                val dtos = listEmptyIfNull(response.owners)
                val owners: MutableList<Owner> = ArrayList(dtos.size)
                for (dto in dtos) {
                    transformOwner(dto)?.let { owners.add(it) }
                }
                owners
            }
    }
}