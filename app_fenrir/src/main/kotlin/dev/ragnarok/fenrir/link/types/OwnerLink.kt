package dev.ragnarok.fenrir.link.types

class OwnerLink(val ownerId: Long) : AbsLink(if (ownerId >= 0) PROFILE else GROUP) {
    override fun toString(): String {
        return "OwnerLink{" +
                "ownerId=" + ownerId +
                '}'
    }
}