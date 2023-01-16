package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.nonNullNoEmpty

class WriteText(val accountId: Long, peerId: Long, from_ids: LongArray?, val isText: Boolean) {
    val peerId: Long
    private val from_ids: MutableList<Long>
    fun getFrom_ids(): List<Long> {
        return from_ids
    }

    init {
        this.from_ids = ArrayList()
        if (from_ids.nonNullNoEmpty()) {
            for (from_id in from_ids) {
                if (accountId != from_id) {
                    this.from_ids.add(from_id)
                }
            }
        }
        this.peerId = peerId
    }
}