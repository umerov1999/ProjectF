package dev.ragnarok.fenrir.link.types

class AudioPlaylistLink(val ownerId: Long, val playlistId: Int, val access_key: String?) : AbsLink(
    PLAYLIST
) {
    override fun toString(): String {
        return "AudioPlaylistLink{" +
                "ownerId=" + ownerId +
                ", playlistId=" + playlistId +
                ", access_key=" + access_key +
                '}'
    }
}