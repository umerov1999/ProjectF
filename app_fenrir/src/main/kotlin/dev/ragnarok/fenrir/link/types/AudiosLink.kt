package dev.ragnarok.fenrir.link.types

class AudiosLink(val ownerId: Long) : AbsLink(AUDIOS) {
    override fun toString(): String {
        return "AudiosLink{" +
                "ownerId=" + ownerId +
                '}'
    }
}