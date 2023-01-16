package dev.ragnarok.fenrir.link.types

class PhotoLink(val id: Int, val ownerId: Long, val access_key: String?) : AbsLink(PHOTO) {
    override fun toString(): String {
        return "PhotoLink{" +
                "ownerId=" + ownerId +
                ", Id=" + id +
                ", Access_Key=" + access_key +
                '}'
    }
}