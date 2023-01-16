package dev.ragnarok.fenrir.link.types

class WallLink(val ownerId: Long) : AbsLink(WALL) {
    override fun toString(): String {
        return "WallLink{" +
                "ownerId=" + ownerId +
                '}'
    }
}