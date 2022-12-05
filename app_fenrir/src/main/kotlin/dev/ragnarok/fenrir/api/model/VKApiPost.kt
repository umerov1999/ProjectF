package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.PostDtoAdapter
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import kotlinx.serialization.Serializable

/**
 * A post object describes a wall post.
 */
@Serializable(with = PostDtoAdapter::class)
class VKApiPost : VKApiAttachment, Commentable, Likeable, Copyable {
    /**
     * Post ID on the wall, positive number
     */
    var id = 0

    /**
     * Wall owner ID.
     */
    var owner_id = 0

    /**
     * ID of the user who posted.
     */
    var from_id = 0

    /**
     * Date (in Unix time) the post was added.
     */
    var date: Long = 0

    /**
     * Text of the post.
     */
    var text: String? = null

    /**
     * ID of the wall owner the post to which the reply is addressed (if the post is a reply to another wall post).
     */
    var reply_owner_id = 0

    /**
     * ID of the wall post to which the reply is addressed (if the post is a reply to another wall post).
     */
    var reply_post_id = 0

    /**
     * True, if the post was created with "Friends only" option.
     */
    var friends_only = false

    /**
     * Number of comments.
     */
    var comments: CommentsDto? = null

    /**
     * Number of users who liked the post.
     */
    var likes_count = 0

    /**
     * Whether the user liked the post (false — not liked, true — liked)
     */
    var user_likes = false

    /**
     * Whether the user can like the post (false — cannot, true — can).
     */
    var can_like = false

    /**
     * Whether the user can repost (false — cannot, true — can).
     */
    var can_publish = false

    /**
     * Number of users who copied the post.
     */
    var reposts_count = 0

    /**
     * Whether the user reposted the post (false — not reposted, true — reposted).
     */
    var user_reposted = false

    /**
     * Type of the post, can be: post, copy, reply, postpone, suggest.
     */
    var post_type = 0

    /**
     * Information about attachments to the post (photos, links, etc.), if any;
     */
    var attachments: VKApiAttachments? = null

    /**
     * ID of the author (if the post was published by a community and signed by a user).
     */
    var signer_id = 0

    /**
     * информация о том, может ли текущий пользователь закрепить запись. R.Kolbasa
     */
    var can_pin = false

    /**
     * информация о том, закреплена ли запись. R.Kolbasa.
     */
    var is_pinned = false

    /**
     * List of history of the reposts.
     */
    var copy_history: ArrayList<VKApiPost>? = null
    var post_source: VKApiPostSource? = null
    var views = 0
    var created_by = 0
    var can_edit = false
    var is_favorite = false

    var copyright: Copyright? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_POST
    }

    val attachmentsCount: Int
        get() = attachments?.size().orZero()

    fun hasAttachments(): Boolean {
        return attachmentsCount > 0
    }

    fun hasCopyHistory(): Boolean {
        return copy_history.nonNullNoEmpty()
    }

    class Copyright(val name: String, val link: String?)

    object Type {
        const val POST = 1
        const val COPY = 2
        const val REPLY = 3
        const val POSTPONE = 4
        const val SUGGEST = 5
        const val DONUT = 6
        fun parse(type: String?): Int {
            return if (type == null) 0 else when (type) {
                "post" -> POST
                "copy" -> COPY
                "reply" -> REPLY
                "postpone" -> POSTPONE
                "suggest" -> SUGGEST
                "donut" -> DONUT
                else -> 0
            }
        }
    }
}