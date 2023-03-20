package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Owner
import io.reactivex.rxjava3.core.Single

interface ILikesInteractor {
    fun getLikes(
        accountId: Long,
        type: String?,
        ownerId: Long,
        itemId: Int,
        filter: String?,
        count: Int,
        offset: Int
    ): Single<List<Owner>>

    companion object {
        const val FILTER_LIKES = "likes"
        const val FILTER_COPIES = "copies"
    }
}