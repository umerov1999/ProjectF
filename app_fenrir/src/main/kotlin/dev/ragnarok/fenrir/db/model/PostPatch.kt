package dev.ragnarok.fenrir.db.model

class PostPatch {
    var likePatch: LikePatch? = null
        private set
    var deletePatch: DeletePatch? = null
        private set
    var pinPatch: PinPatch? = null
        private set

    fun withDeletion(deleted: Boolean): PostPatch {
        deletePatch = DeletePatch(deleted)
        return this
    }

    fun withPin(pinned: Boolean): PostPatch {
        pinPatch = PinPatch(pinned)
        return this
    }

    fun withLikes(count: Int, usesLikes: Boolean): PostPatch {
        likePatch = LikePatch(usesLikes, count)
        return this
    }

    class LikePatch(val isLiked: Boolean, val count: Int)
    class DeletePatch(val isDeleted: Boolean)
    class PinPatch(val isPinned: Boolean)
}