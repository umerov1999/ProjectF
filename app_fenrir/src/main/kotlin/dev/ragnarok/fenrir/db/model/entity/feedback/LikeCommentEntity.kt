package dev.ragnarok.fenrir.db.model.entity.feedback

import androidx.annotation.Keep
import dev.ragnarok.fenrir.db.model.entity.CommentEntity
import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.model.feedback.FeedbackType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("like_comment")
class LikeCommentEntity : FeedbackEntity {
    var likesOwnerIds: LongArray? = null
        private set

    var commented: DboEntity? = null
        private set

    var liked: CommentEntity? = null
        private set

    @Suppress("UNUSED")
    constructor()
    constructor(@FeedbackType type: Int) {
        this.type = type
    }

    fun setLiked(liked: CommentEntity?): LikeCommentEntity {
        this.liked = liked
        return this
    }

    fun setCommented(commented: DboEntity?): LikeCommentEntity {
        this.commented = commented
        return this
    }

    fun setLikesOwnerIds(likesOwnerIds: LongArray?): LikeCommentEntity {
        this.likesOwnerIds = likesOwnerIds
        return this
    }
}