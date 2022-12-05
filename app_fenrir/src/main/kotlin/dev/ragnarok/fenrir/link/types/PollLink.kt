package dev.ragnarok.fenrir.link.types

class PollLink(val ownerId: Int, val Id: Int) : AbsLink(POLL) {
    override fun toString(): String {
        return "PollLink{" +
                "ownerId=" + ownerId +
                ", Id=" + Id +
                '}'
    }
}