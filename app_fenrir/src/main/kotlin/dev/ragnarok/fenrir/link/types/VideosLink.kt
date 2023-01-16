package dev.ragnarok.fenrir.link.types

class VideosLink(val ownerId: Long) : AbsLink(VIDEOS) {
    override fun toString(): String {
        return "VideosLink{" +
                "ownerId=" + ownerId +
                '}'
    }
}
