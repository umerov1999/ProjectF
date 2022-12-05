package dev.ragnarok.fenrir.db.model

class PhotoPatch {
    var like: Like? = null
        private set
    var deletion: Deletion? = null
        private set

    fun setDeletion(deletion: Deletion?): PhotoPatch {
        this.deletion = deletion
        return this
    }

    fun setLike(like: Like?): PhotoPatch {
        this.like = like
        return this
    }

    class Like(val count: Int, val isLiked: Boolean)
    class Deletion(val isDeleted: Boolean)
}