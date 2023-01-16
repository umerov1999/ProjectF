package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Topic
import io.reactivex.rxjava3.core.Single

interface IBoardInteractor {
    fun getCachedTopics(accountId: Long, ownerId: Long): Single<List<Topic>>
    fun getActualTopics(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Single<List<Topic>>
}