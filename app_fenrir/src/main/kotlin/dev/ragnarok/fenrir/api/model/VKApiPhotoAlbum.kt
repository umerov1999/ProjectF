package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.PhotoAlbumDtoAdapter
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import kotlinx.serialization.Serializable

/**
 * Describes a photo album
 */
@Serializable(with = PhotoAlbumDtoAdapter::class)
class VKApiPhotoAlbum
/**
 * Creates empty PhotoAlbum instance.
 */
    : VKApiAttachment {
    /**
     * Album ID.
     */
    var id = 0

    /**
     * Album title.
     */
    var title: String? = null

    /**
     * Number of photos in the album.
     */
    var size = 0

    /**
     * Album description.
     */
    var description: String? = null

    /**
     * ID of the user or community that owns the album.
     */
    var owner_id = 0L

    /**
     * Whether a user can upload photos to this album(false — cannot, true — can).
     */
    var can_upload = false

    /**
     * Date (in Unix time) the album was last updated.
     */
    var updated: Long = 0

    /**
     * Album creation date (in Unix time).
     */
    var created: Long = 0

    /**
     * ID of the photo which is the cover.
     */
    var thumb_id = 0

    /**
     * Link to album cover photo.
     */
    var thumb_src: String? = null

    /**
     * Links to to cover photo.
     */
    var photo: ArrayList<PhotoSizeDto>? = null

    /**
     * Настройки приватности для просмотра альбома
     */
    var privacy_view: VKApiPrivacy? = null

    /**
     * Настройки приватности для комментирования альбома
     */
    var privacy_comment: VKApiPrivacy? = null

    /**
     * кто может загружать фотографии в альбом (только для альбома сообщества,
     * не приходит для системных альбомов);
     */
    var upload_by_admins_only = false

    /**
     * отключено ли комментирование альбома (только для альбома сообщества,
     * не приходит для системных альбомов);
     */
    var comments_disabled = false
    override fun getType(): String {
        return VKApiAttachment.TYPE_ALBUM
    }

    val isSystem: Boolean
        get() = id < 0

    companion object {
        /**
         * URL for empty album cover with max width at 75px
         */
        const val COVER_S = "http://vk.com/images/s_noalbum.png"

        /**
         * URL of empty album cover with max width at 130px
         */
        const val COVER_M = "http://vk.com/images/m_noalbum.png"

        /**
         * URL of empty album cover with max width at 604px
         */
        const val COVER_X = "http://vk.com/images/x_noalbum.png"
    }
}