package dev.ragnarok.fenrir.model

class CommentUpdate private constructor(
    private val accountId: Long,
    private val commented: Commented,
    private val commentId: Int
) {
    private var likeUpdate: LikeUpdate? = null
    private var deleteUpdate: DeleteUpdate? = null
    fun getAccountId(): Long {
        return accountId
    }

    fun withDeletion(deleted: Boolean): CommentUpdate {
        deleteUpdate = DeleteUpdate(deleted)
        return this
    }

    fun getCommented(): Commented {
        return commented
    }

    fun getCommentId(): Int {
        return commentId
    }

    fun getDeleteUpdate(): DeleteUpdate? {
        return deleteUpdate
    }

    fun getLikeUpdate(): LikeUpdate? {
        return likeUpdate
    }

    fun withLikes(userLikes: Boolean, count: Int): CommentUpdate {
        likeUpdate = LikeUpdate(userLikes, count)
        return this
    }

    class DeleteUpdate(private val deleted: Boolean) {
        fun isDeleted(): Boolean {
            return deleted
        }
    }

    class LikeUpdate(private val userLikes: Boolean, private val count: Int) {
        fun getCount(): Int {
            return count
        }

        fun isUserLikes(): Boolean {
            return userLikes
        }
    }

    companion object {
        fun create(accountId: Long, commented: Commented, commentId: Int): CommentUpdate {
            return CommentUpdate(accountId, commented, commentId)
        }
    }
}