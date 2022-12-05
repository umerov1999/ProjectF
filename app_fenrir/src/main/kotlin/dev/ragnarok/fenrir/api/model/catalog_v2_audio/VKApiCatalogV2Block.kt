package dev.ragnarok.fenrir.api.model.catalog_v2_audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCatalogV2Block {
    @SerialName("audios_ids")
    var audios_ids: ArrayList<String>? = null

    @SerialName("playlists_ids")
    var playlists_ids: ArrayList<String>? = null

    @SerialName("artists_ids")
    var artists_ids: ArrayList<String>? = null

    @SerialName("links_ids")
    var links_ids: ArrayList<String>? = null

    @SerialName("artist_videos_ids")
    var artist_videos_ids: ArrayList<String>? = null

    @SerialName("videos_ids")
    var videos_ids: ArrayList<String>? = null

    @SerialName("id")
    var id: String? = null

    @SerialName("data_type")
    var data_type: String? = null

    @SerialName("next_from")
    var next_from: String? = null

    @SerialName("buttons")
    var buttons: List<VKApiCatalogV2Button>? = null

    @SerialName("layout")
    var layout: VKApiCatalogV2Layout? = null

    @SerialName("badge")
    var badge: VKApiCatalogV2Badge? = null
}