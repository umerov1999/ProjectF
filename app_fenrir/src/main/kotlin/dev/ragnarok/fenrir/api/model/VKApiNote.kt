package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import kotlinx.serialization.Serializable

/**
 * A note object describes a note.
 */
@Serializable
class VKApiNote
/**
 * Creates empty Note instance.
 */
    : VKApiAttachment {
    /**
     * Note ID, positive number
     */
    var id = 0

    /**
     * Note owner ID.
     */
    var user_id = 0L

    /**
     * Note title.
     */
    var title: String? = null

    /**
     * Note text.
     */
    var text: String? = null

    /**
     * Date (in Unix time) when the note was created.
     */
    var date: Long = 0

    /**
     * Number of comments.
     */
    var comments = 0

    /**
     * Number of read comments (only if owner_id is the current user).
     */
    var read_comments = 0
    override fun getType(): String {
        return VKApiAttachment.TYPE_NOTE
    }
}