package dev.ragnarok.fenrir.link.types

class VideoLink(val ownerId: Int, val videoId: Int, val access_key: String?) : AbsLink(VIDEO) {
    override fun toString(): String {
        return "VideoLink{" +
                "ownerId=" + ownerId +
                ", videoId=" + videoId +
                ", Access_Key=" + access_key +
                '}'
    }
}