package dev.ragnarok.fenrir.link.types

class AudioTrackLink(val ownerId: Long, val trackId: Int) : AbsLink(AUDIO_TRACK) {
    override fun toString(): String {
        return "AudioTrackLink{" +
                "ownerId=" + ownerId +
                ", trackId=" + trackId +
                '}'
    }
}