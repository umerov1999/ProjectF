package dev.ragnarok.fenrir.link.types

class VideoAlbumLink(val ownerId: Long, val albumId: Int) : AbsLink(VIDEO_ALBUM) {
    override fun toString(): String {
        return "VideoAlbumLink{" +
                "ownerId=" + ownerId +
                ", albumId=" + albumId +
                '}'
    }
}