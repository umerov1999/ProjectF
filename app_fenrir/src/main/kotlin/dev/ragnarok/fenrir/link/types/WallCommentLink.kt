package dev.ragnarok.fenrir.link.types

class WallCommentLink(val ownerId: Int, val postId: Int, val commentId: Int) : AbsLink(
    WALL_COMMENT
) {
    override val isValid: Boolean
        get() = ownerId != 0 && postId > 0 && commentId > 0
}