package dev.ragnarok.fenrir.link.types

class AudiosLink(val ownerId: Int) : AbsLink(AUDIOS) {
    override fun toString(): String {
        return "AudiosLink{" +
                "ownerId=" + ownerId +
                '}'
    }
}