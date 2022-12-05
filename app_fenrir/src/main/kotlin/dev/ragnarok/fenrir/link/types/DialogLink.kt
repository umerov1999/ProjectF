package dev.ragnarok.fenrir.link.types

class DialogLink(val peerId: Int) : AbsLink(DIALOG) {
    override fun toString(): String {
        return "DialogLink{" +
                "peerId=" + peerId +
                '}'
    }
}