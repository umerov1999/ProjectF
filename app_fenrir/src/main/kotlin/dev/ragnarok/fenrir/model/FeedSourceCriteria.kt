package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.model.criteria.Criteria

class FeedSourceCriteria(private val accountId: Long) : Criteria() {
    fun getAccountId(): Long {
        return accountId
    }
}