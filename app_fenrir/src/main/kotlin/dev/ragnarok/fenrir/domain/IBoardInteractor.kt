package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Topic
import io.reactivex.rxjava3.core.Single

interface IBoardInteractor {
    fun getCachedTopics(accountId: Int, ownerId: Int): Single<List<Topic>>
    fun getActualTopics(
        accountId: Int,
        ownerId: Int,
        count: Int,
        offset: Int
    ): Single<List<Topic>>
}