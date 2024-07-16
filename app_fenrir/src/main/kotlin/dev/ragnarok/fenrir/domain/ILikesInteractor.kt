package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Owner
import kotlinx.coroutines.flow.Flow

interface ILikesInteractor {
    fun getLikes(
        accountId: Long,
        type: String?,
        ownerId: Long,
        itemId: Int,
        filter: String?,
        count: Int,
        offset: Int
    ): Flow<List<Owner>>

    companion object {
        const val FILTER_LIKES = "likes"
        const val FILTER_COPIES = "copies"
    }
}