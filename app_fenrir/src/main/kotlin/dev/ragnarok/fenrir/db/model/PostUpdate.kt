package dev.ragnarok.fenrir.db.model

class PostUpdate(val accountId: Int, val postId: Int, val ownerId: Int) {
    var pinUpdate: PinUpdate? = null
        private set
    var deleteUpdate: DeleteUpdate? = null
        private set
    var likeUpdate: LikeUpdate? = null
        private set

    fun withDeletion(deleted: Boolean): PostUpdate {
        deleteUpdate = DeleteUpdate(deleted)
        return this
    }

    fun withPin(pinned: Boolean): PostUpdate {
        pinUpdate = PinUpdate(pinned)
        return this
    }

    fun withLikes(count: Int, usesLikes: Boolean): PostUpdate {
        likeUpdate = LikeUpdate(usesLikes, count)
        return this
    }

    class PinUpdate(val isPinned: Boolean)
    class DeleteUpdate(val isDeleted: Boolean)
    class LikeUpdate(val isLiked: Boolean, val count: Int)
}