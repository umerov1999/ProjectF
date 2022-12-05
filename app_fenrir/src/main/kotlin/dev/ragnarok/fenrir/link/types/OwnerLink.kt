package dev.ragnarok.fenrir.link.types

class OwnerLink(val ownerId: Int) : AbsLink(if (ownerId >= 0) PROFILE else GROUP) {
    override fun toString(): String {
        return "OwnerLink{" +
                "ownerId=" + ownerId +
                '}'
    }
}