package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.PhotoDtoAdapter
import kotlinx.serialization.Serializable

/**
 * Describes a photo object from VK.
 */
@Serializable(with = PhotoDtoAdapter::class)
class VKApiPhoto
/**
 * Creates empty Photo instance.
 */
    : VKApiAttachment, Commentable, Likeable, Copyable {
    /**
     * Photo ID, positive number
     */
    var id = 0

    /**
     * Photo album ID.
     */
    var album_id = 0

    /**
     * ID of the user or community that owns the photo.
     */
    var owner_id = 0

    /**
     * Width (in pixels) of the original photo.
     */
    var width = 0

    /**
     * Height (in pixels) of the original photo.
     */
    var height = 0

    /**
     * Text describing the photo.
     */
    var text: String? = null

    /**
     * Date (in Unix time) the photo was added.
     */
    var date: Long = 0

    /**
     * Information whether the current user liked the photo.
     */
    var user_likes = false

    /**
     * Whether the current user can comment on the photo
     */
    var can_comment = false

    /**
     * Number of likes on the photo.
     */
    var likes = 0

    /**
     * Number of reposts on the photo.
     */
    var reposts = 0

    /**
     * Number of comments on the photo.
     */
    var comments: CommentsDto? = null

    /**
     * Number of tags on the photo.
     */
    var tags = 0

    /**
     * An access key using for get information about hidden objects.
     */
    var access_key: String? = null
    var post_id = 0
    var sizes: ArrayList<PhotoSizeDto>? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_PHOTO
    }
}