package dev.ragnarok.fenrir.link.types

class ArtistsLink(val Id: String) : AbsLink(ARTISTS) {
    override fun toString(): String {
        return "ArtistsLink{" +
                "Id=" + Id +
                '}'
    }
}