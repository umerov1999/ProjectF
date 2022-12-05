package dev.ragnarok.fenrir.link.types

class WallCommentThreadLink(
    val ownerId: Int,
    val postId: Int,
    val commentId: Int,
    val threadId: Int
) : AbsLink(
    WALL_COMMENT_THREAD
) {
    override val isValid: Boolean
        get() = ownerId != 0 && postId > 0 && commentId > 0 && threadId > 0
}