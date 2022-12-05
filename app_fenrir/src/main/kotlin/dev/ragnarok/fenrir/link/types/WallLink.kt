package dev.ragnarok.fenrir.link.types

class WallLink(val ownerId: Int) : AbsLink(WALL) {
    override fun toString(): String {
        return "WallLink{" +
                "ownerId=" + ownerId +
                '}'
    }
}