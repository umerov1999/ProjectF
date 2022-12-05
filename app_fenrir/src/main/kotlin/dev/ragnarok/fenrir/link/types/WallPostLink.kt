package dev.ragnarok.fenrir.link.types

class WallPostLink(val ownerId: Int, val postId: Int) : AbsLink(WALL_POST) {
    override fun toString(): String {
        return "WallPostLink{" +
                "ownerId=" + ownerId +
                ", postId=" + postId +
                '}'
    }
}