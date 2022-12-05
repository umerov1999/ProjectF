package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.adapters.ServicePlaylistResponseDtoAdapter
import dev.ragnarok.fenrir.api.model.VKApiAudioPlaylist
import kotlinx.serialization.Serializable

@Serializable(with = ServicePlaylistResponseDtoAdapter::class)
class ServicePlaylistResponse {
    var playlists: ArrayList<VKApiAudioPlaylist>? = null
}