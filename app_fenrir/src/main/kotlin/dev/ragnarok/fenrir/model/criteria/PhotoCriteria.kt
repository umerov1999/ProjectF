package dev.ragnarok.fenrir.model.criteria

import dev.ragnarok.fenrir.db.DatabaseIdRange

class PhotoCriteria(val accountId: Long) {
    var ownerId = 0L
        private set
    var albumId = 0
        private set
    var orderBy: String? = null
        private set
    var sortInvert = false
        private set
    var range: DatabaseIdRange? = null
        private set

    fun setRange(range: DatabaseIdRange?): PhotoCriteria {
        this.range = range
        return this
    }

    fun setSortInvert(invert: Boolean): PhotoCriteria {
        sortInvert = invert
        return this
    }

    fun setOwnerId(ownerId: Long): PhotoCriteria {
        this.ownerId = ownerId
        return this
    }

    fun setAlbumId(albumId: Int): PhotoCriteria {
        this.albumId = albumId
        return this
    }

    fun setOrderBy(orderBy: String?): PhotoCriteria {
        this.orderBy = orderBy
        return this
    }
}