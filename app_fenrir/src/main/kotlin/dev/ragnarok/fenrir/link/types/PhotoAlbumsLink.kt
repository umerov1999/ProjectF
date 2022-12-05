package dev.ragnarok.fenrir.link.types

class PhotoAlbumsLink(val ownerId: Int) : AbsLink(ALBUMS) {
    override fun toString(): String {
        return "PhotoAlbumsLink{" +
                "ownerId=" + ownerId +
                '}'
    }
}