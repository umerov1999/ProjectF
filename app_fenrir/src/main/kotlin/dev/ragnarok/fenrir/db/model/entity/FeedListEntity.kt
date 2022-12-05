package dev.ragnarok.fenrir.db.model.entity

class FeedListEntity(val id: Int) {
    var title: String? = null
        private set
    var isNoReposts = false
        private set
    var sourceIds: IntArray? = null
        private set

    fun setTitle(title: String?): FeedListEntity {
        this.title = title
        return this
    }

    fun setNoReposts(noReposts: Boolean): FeedListEntity {
        isNoReposts = noReposts
        return this
    }

    fun setSourceIds(sourceIds: IntArray?): FeedListEntity {
        this.sourceIds = sourceIds
        return this
    }
}