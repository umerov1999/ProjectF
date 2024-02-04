package dev.ragnarok.fenrir.media.voice

import dev.ragnarok.fenrir.model.VoiceMessage

class AudioEntry(val id: Int, val audio: VoiceMessage) {
    override fun equals(other: Any?): Boolean {
        return other is AudioEntry && id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}