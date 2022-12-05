package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.model.criteria.Criteria

class FeedSourceCriteria(private val accountId: Int) : Criteria() {
    fun getAccountId(): Int {
        return accountId
    }
}