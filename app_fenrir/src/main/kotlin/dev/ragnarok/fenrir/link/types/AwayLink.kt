package dev.ragnarok.fenrir.link.types

class AwayLink(val link: String) : AbsLink(EXTERNAL_LINK) {
    override fun toString(): String {
        return "AwayLink{" +
                "link='" + link + '\'' +
                '}'
    }
}