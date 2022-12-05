package dev.ragnarok.fenrir.db.model.entity.feedback

import androidx.annotation.Keep
import dev.ragnarok.fenrir.db.model.entity.CommentEntity
import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.model.feedback.FeedbackType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("reply_comment")
class ReplyCommentEntity : FeedbackEntity {
    private var commented: DboEntity? = null

    var ownComment: CommentEntity? = null
        private set

    var feedbackComment: CommentEntity? = null
        private set

    @Suppress("UNUSED")
    constructor()
    constructor(@FeedbackType type: Int) {
        this.type = type
    }

    fun getCommented(): DboEntity? {
        return commented
    }

    fun setCommented(commented: DboEntity?): ReplyCommentEntity {
        this.commented = commented
        return this
    }

    fun setFeedbackComment(feedbackComment: CommentEntity?): ReplyCommentEntity {
        this.feedbackComment = feedbackComment
        return this
    }

    fun setOwnComment(ownComment: CommentEntity?): ReplyCommentEntity {
        this.ownComment = ownComment
        return this
    }
}