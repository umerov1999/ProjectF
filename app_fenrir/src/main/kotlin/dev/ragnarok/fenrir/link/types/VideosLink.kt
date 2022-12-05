package dev.ragnarok.fenrir.link.types

class VideosLink(val ownerId: Int) : AbsLink(VIDEOS) {
    override fun toString(): String {
        return "VideosLink{" +
                "ownerId=" + ownerId +
                '}'
    }
}
