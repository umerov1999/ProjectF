package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("wall_reply")
class WallReplyDboEntity : DboEntity() {
    var id = 0
        private set
    var fromId = 0L
        private set
    var postId = 0
        private set
    var ownerId = 0L
        private set
    var text: String? = null
        private set
    private var attachments: List<DboEntity>? = null
    val attachmentsCount: Int
        get() = safeCountOf(attachments)

    fun hasAttachments(): Boolean {
        return attachmentsCount > 0
    }

    fun setId(id: Int): WallReplyDboEntity {
        this.id = id
        return this
    }

    fun setOwnerId(owner_id: Long): WallReplyDboEntity {
        ownerId = owner_id
        return this
    }

    fun setFromId(from_id: Long): WallReplyDboEntity {
        fromId = from_id
        return this
    }

    fun setPostId(post_id: Int): WallReplyDboEntity {
        postId = post_id
        return this
    }

    fun setText(text: String?): WallReplyDboEntity {
        this.text = text
        return this
    }

    fun getAttachments(): List<DboEntity>? {
        return attachments
    }

    fun setAttachments(entities: List<DboEntity>?): WallReplyDboEntity {
        attachments = entities
        return this
    }
}