package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.VideoDtoAdapter
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.IIdComparable
import dev.ragnarok.fenrir.api.model.interfaces.Commentable
import dev.ragnarok.fenrir.api.model.interfaces.Copyable
import dev.ragnarok.fenrir.api.model.interfaces.Likeable
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import kotlinx.serialization.Serializable

/**
 * A video object describes an video file.
 */
@Serializable(with = VideoDtoAdapter::class)
class VKApiVideo : VKApiAttachment, Commentable, Likeable, Copyable, IIdComparable {
    /**
     * Video ID.
     */
    var id = 0

    /**
     * Video owner ID.
     */
    var owner_id = 0L

    /**
     * Video album ID.
     */
    var album_id = 0

    /**
     * Video title.
     */
    var title: String? = null

    /**
     * Text describing video.
     */
    var description: String? = null

    /**
     * Duration of the video in seconds.
     */
    var duration = 0L

    /**
     * String with video+vid key.
     */
    var link: String? = null

    /**
     * Date when the video was created, as unixtime.
     */
    var date: Long = 0

    /**
     * Date when the video was added, as unixtime.
     */
    var adding_date: Long = 0

    /**
     * Number of views of the video.
     */
    var views = 0

    /**
     * URL of the page with a player that can be used to play a video in the browser.
     * Flash and HTML5 video players are supported; the player is always zoomed to fit
     * the window size.
     */
    var player: String? = null

    /**
     * URL of the video cover image
     */
    var image: String? = null

    /**
     * An access key using for get information about hidden objects.
     */
    var access_key: String? = null

    /**
     * Number of comments on the video.
     */
    var comments: CommentsDto? = null

    /**
     * Whether the current user can comment on the video
     */
    var can_comment = false

    /**
     * Whether the current user can repost this video
     */
    var can_repost = false

    /**
     * Information whether the current user liked the video.
     */
    var user_likes = false

    /**
     * Information whether the the video should be repeated.
     */
    var repeat = false

    /**
     * Number of likes on the video.
     */
    var likes = 0

    /**
     * VKApiPrivacy to view of this video.
     */
    var privacy_view: VKApiPrivacy? = null

    /**
     * VKApiPrivacy to comment of this video.
     */
    var privacy_comment: VKApiPrivacy? = null

    /**
     * URL of video with height of 240 pixels. Returns only if you use direct auth.
     */
    var mp4_240: String? = null

    /**
     * URL of video with height of 360 pixels. Returns only if you use direct auth.
     */
    var mp4_360: String? = null

    /**
     * URL of video with height of 480 pixels. Returns only if you use direct auth.
     */
    var mp4_480: String? = null

    /**
     * URL of video with height of 720 pixels. Returns only if you use direct auth.
     */
    var mp4_720: String? = null

    /**
     * URL of video with height of 1080 pixels. Returns only if you use direct auth.
     */
    var mp4_1080: String? = null

    /**
     * URL of video with height of 1440 pixels. Returns only if you use direct auth.
     */
    var mp4_1440: String? = null

    /**
     * URL of video with height of 2160 pixels. Returns only if you use direct auth.
     */
    var mp4_2160: String? = null

    /**
     * URL of the external video link.
     */
    var external: String? = null
    var hls: String? = null
    var live: String? = null
    var platform: String? = null
    var can_edit = false
    var can_add = false
    var is_private = false
    var is_favorite = false
    override fun getType(): String {
        return VKApiAttachment.TYPE_VIDEO
    }

    override fun compareFullId(object_s: String): Boolean {
        return (owner_id.toString() + "_" + id) == object_s
    }
}