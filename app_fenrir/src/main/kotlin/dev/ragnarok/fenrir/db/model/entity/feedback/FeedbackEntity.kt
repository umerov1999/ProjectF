package dev.ragnarok.fenrir.db.model.entity.feedback

import dev.ragnarok.fenrir.db.model.entity.CommentEntity
import dev.ragnarok.fenrir.model.feedback.FeedbackType
import kotlinx.serialization.Serializable

@Serializable
sealed class FeedbackEntity {
    @FeedbackType
    var type = 0
        protected set

    var date: Long = 0
        private set

    var reply: CommentEntity? = null
        private set

    fun setDate(date: Long): FeedbackEntity {
        this.date = date
        return this
    }

    fun setReply(reply: CommentEntity?): FeedbackEntity {
        this.reply = reply
        return this
    }
}