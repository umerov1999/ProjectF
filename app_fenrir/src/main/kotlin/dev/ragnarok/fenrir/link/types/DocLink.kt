package dev.ragnarok.fenrir.link.types

class DocLink(val ownerId: Long, val docId: Int, val access_key: String?) : AbsLink(DOC) {
    override fun toString(): String {
        return "DocLink{" +
                "ownerId=" + ownerId +
                ", docId=" + docId +
                ", Access_Key=" + access_key +
                '}'
    }
}