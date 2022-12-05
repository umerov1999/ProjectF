package dev.ragnarok.fenrir.link.types

class DomainLink(val fullLink: String, val domain: String) : AbsLink(DOMAIN) {
    override fun toString(): String {
        return "DomainLink{" +
                "fullLink='" + fullLink + '\'' +
                ", domain='" + domain + '\'' +
                '}'
    }
}