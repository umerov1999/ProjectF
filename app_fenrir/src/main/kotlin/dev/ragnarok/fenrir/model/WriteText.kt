package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.nonNullNoEmpty

class WriteText(val accountId: Int, peerId: Int, from_ids: IntArray?, val isText: Boolean) {
    val peerId: Int
    private val from_ids: MutableList<Int>
    fun getFrom_ids(): List<Int> {
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