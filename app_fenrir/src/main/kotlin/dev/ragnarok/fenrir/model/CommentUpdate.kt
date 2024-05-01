package dev.ragnarok.fenrir.model

class CommentUpdate private constructor(
    val accountId: Long,
    val commented: Commented,
    val commentId: Int
) {
    var likeUpdate: LikeUpdate? = null
        private set
    var deleteUpdate: DeleteUpdate? = null
        private set

    fun withDeletion(deleted: Boolean): CommentUpdate {
        deleteUpdate = DeleteUpdate(deleted)
        return this
    }

    fun withLikes(userLikes: Boolean, count: Int): CommentUpdate {
        likeUpdate = LikeUpdate(userLikes, count)
        return this
    }

    class DeleteUpdate(val deleted: Boolean)
    class LikeUpdate(val userLikes: Boolean, val count: Int)

    companion object {
        fun create(accountId: Long, commented: Commented, commentId: Int): CommentUpdate {
            return CommentUpdate(accountId, commented, commentId)
        }
    }
}