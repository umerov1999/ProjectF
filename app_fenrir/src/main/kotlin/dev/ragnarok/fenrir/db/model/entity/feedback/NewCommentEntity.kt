package dev.ragnarok.fenrir.db.model.entity.feedback

import androidx.annotation.Keep
import dev.ragnarok.fenrir.db.model.entity.CommentEntity
import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.model.feedback.FeedbackType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("new_comment")
class NewCommentEntity : FeedbackEntity {
    private var commented: DboEntity? = null

    var comment: CommentEntity? = null
        private set

    @Suppress("UNUSED")
    constructor()
    constructor(@FeedbackType type: Int) {
        this.type = type
    }

    fun setComment(comment: CommentEntity?): NewCommentEntity {
        this.comment = comment
        return this
    }

    fun getCommented(): DboEntity? {
        return commented
    }

    fun setCommented(commented: DboEntity?): NewCommentEntity {
        this.commented = commented
        return this
    }
}