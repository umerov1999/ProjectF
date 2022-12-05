package dev.ragnarok.fenrir.db.model.entity.feedback

import androidx.annotation.Keep
import dev.ragnarok.fenrir.db.model.entity.CommentEntity
import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.model.feedback.FeedbackType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base class for types [mention_comments, mention_comment_photo, mention_comment_video]
 */
@Keep
@Serializable
@SerialName("mention_comment")
class MentionCommentEntity : FeedbackEntity {
    var where: CommentEntity? = null
        private set

    private var commented: DboEntity? = null

    @Suppress("UNUSED")
    constructor()
    constructor(@FeedbackType type: Int) {
        this.type = type
    }

    fun setWhere(where: CommentEntity?): MentionCommentEntity {
        this.where = where
        return this
    }

    fun getCommented(): DboEntity? {
        return commented
    }

    fun setCommented(commented: DboEntity?): MentionCommentEntity {
        this.commented = commented
        return this
    }
}