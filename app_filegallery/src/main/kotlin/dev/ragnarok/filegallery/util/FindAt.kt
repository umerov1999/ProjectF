package dev.ragnarok.filegallery.util

import dev.ragnarok.filegallery.nonNullNoEmpty

class FindAt {

    constructor() {
        this.q = null
        this.offset = 0
        this.ended = false
    }

    constructor(q: String?) {
        this.q = q
        this.offset = 0
        this.ended = false
    }

    constructor(q: String?, offset: Int) {
        this.q = q
        this.offset = offset
        this.ended = false
    }

    constructor(q: String?, offset: Int, ended: Boolean) {
        this.q = q
        this.offset = offset
        this.ended = ended
    }

    fun do_compare(q: String?): Boolean {
        if (q.isNullOrEmpty() && this.q.isNullOrEmpty() || this.q.nonNullNoEmpty() && q.nonNullNoEmpty() && this.q.equals(
                q,
                ignoreCase = true
            )
        ) {
            return true
        }
        this.q = q
        this.offset = 0
        this.ended = false
        return false
    }

    fun reset(clear: Boolean): FindAt {
        this.offset = 0
        this.ended = false
        if (clear) {
            q = null
        }
        return this
    }

    fun isEnded(): Boolean {
        return ended
    }

    fun isSearchMode(): Boolean {
        return q.nonNullNoEmpty()
    }

    fun getOffset(): Int {
        return offset
    }

    fun getQuery(): String? {
        return q
    }

    private var q: String?
    private var offset: Int
    private var ended: Boolean
}
