package dev.ragnarok.fenrir.link.types

class PhotoAlbumsLink(val ownerId: Long) : AbsLink(ALBUMS) {
    override fun toString(): String {
        return "PhotoAlbumsLink{" +
                "ownerId=" + ownerId +
                '}'
    }
}