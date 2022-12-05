package dev.ragnarok.fenrir.link.types

class PhotoAlbumLink(val ownerId: Int, val albumId: Int) : AbsLink(PHOTO_ALBUM) {
    override fun toString(): String {
        return "PhotoAlbumLink{" +
                "ownerId=" + ownerId +
                ", albumId=" + albumId +
                '}'
    }
}