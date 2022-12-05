package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.NewsfeedComment
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Single

interface INewsfeedInteractor {
    fun getNewsfeedComments(
        accountId: Int,
        count: Int,
        startFrom: String?,
        filter: String?
    ): Single<Pair<List<NewsfeedComment>, String?>>

    fun getMentions(
        accountId: Int,
        owner_id: Int?,
        count: Int?,
        offset: Int?,
        startTime: Long?,
        endTime: Long?
    ): Single<Pair<List<NewsfeedComment>, String?>>
}