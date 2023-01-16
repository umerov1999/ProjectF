package dev.ragnarok.fenrir.link.types

class WallCommentLink(val ownerId: Long, val postId: Int, val commentId: Int) : AbsLink(
    WALL_COMMENT
) {
    override val isValid: Boolean
        get() = ownerId != 0L && postId > 0 && commentId > 0
}