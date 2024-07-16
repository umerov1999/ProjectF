package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.NewsfeedComment
import dev.ragnarok.fenrir.util.Pair
import kotlinx.coroutines.flow.Flow

interface INewsfeedInteractor {
    fun getNewsfeedComments(
        accountId: Long,
        count: Int,
        startFrom: String?,
        filter: String?
    ): Flow<Pair<List<NewsfeedComment>, String?>>

    fun getMentions(
        accountId: Long,
        owner_id: Long?,
        count: Int?,
        offset: Int?,
        startTime: Long?,
        endTime: Long?
    ): Flow<Pair<List<NewsfeedComment>, String?>>
}