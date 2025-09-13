package dev.ragnarok.fenrir.link.types

class MarketLink(val ownerId: Long) : AbsLink(MARKETS) {
    override fun toString(): String {
        return "MarketLink{" +
                "ownerId=" + ownerId +
                '}'
    }
}