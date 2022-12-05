package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.CommentDtoAdapter
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import kotlinx.serialization.Serializable

/**
 * Comment object describes a comment.
 */
@Serializable(with = CommentDtoAdapter::class)
class VKApiComment
/**
 * Creates empty Comment instance.
 */
{
    /**
     * Comment ID, positive number
     */
    var id = 0

    /**
     * Comment author ID.
     */
    var from_id = 0

    /**
     * Date when the comment was added as unixtime.
     */
    var date: Long = 0

    /**
     * Text of the comment
     */
    var text: String? = null

    /**
     * ID of the user or community to whom the reply is addressed (if the comment is a reply to another comment).
     */
    var reply_to_user = 0

    /**
     * ID of the comment the reply to which is represented by the current comment (if the comment is a reply to another comment).
     */
    var reply_to_comment = 0

    /**
     * Number of likes on the comment.
     */
    var likes = 0

    /**
     * Information whether the current user liked the comment.
     */
    var user_likes = false

    /**
     * Whether the current user can like on the comment.
     */
    var can_like = false
    var can_edit = false

    /**
     * Information about attachments in the comments (photos, links, etc.;)
     */
    var attachments: VKApiAttachments? = null
    var threads_count = 0
    var threads: List<VKApiComment>? = null
    var pid = 0
    val attachmentsCount: Int
        get() = attachments?.size().orZero()

    fun hasAttachments(): Boolean {
        return attachmentsCount > 0
    }

    fun hasThreads(): Boolean {
        return threads.nonNullNoEmpty()
    }
}