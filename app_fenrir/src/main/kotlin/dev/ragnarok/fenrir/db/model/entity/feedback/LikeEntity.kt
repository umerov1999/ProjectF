package dev.ragnarok.fenrir.db.model.entity.feedback

import androidx.annotation.Keep
import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.model.feedback.FeedbackType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("like")
class LikeEntity : FeedbackEntity {
    var likesOwnerIds: LongArray? = null
        private set

    private var liked: DboEntity? = null

    @Suppress("UNUSED")
    constructor()
    constructor(@FeedbackType type: Int) {
        this.type = type
    }

    fun setLikesOwnerIds(likesOwnerIds: LongArray?): LikeEntity {
        this.likesOwnerIds = likesOwnerIds
        return this
    }

    fun getLiked(): DboEntity? {
        return liked
    }

    fun setLiked(liked: DboEntity?): LikeEntity {
        this.liked = liked
        return this
    }
}