package dev.ragnarok.fenrir.media.voice

import dev.ragnarok.fenrir.model.VoiceMessage

class AudioEntry(val id: Int, val audio: VoiceMessage) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val entry = other as AudioEntry
        return id == entry.id
    }

    override fun hashCode(): Int {
        return id
    }
}