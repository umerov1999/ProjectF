package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.VideoAlbumDtoAdapter
import kotlinx.serialization.Serializable

/**
 * Describes a photo album
 */
@Serializable(with = VideoAlbumDtoAdapter::class)
class VKApiVideoAlbum {
    /**
     * Album ID.
     */
    var id = 0

    /**
     * Album title.
     */
    var title: String? = null

    /**
     * ID of the user or community that owns the album.
     */
    var owner_id = 0L
    var count = 0

    /**
     * Date (in Unix time) the album was last updated.
     */
    var updated_time: Long = 0
    var image: String? = null
    var privacy: VKApiPrivacy? = null
}