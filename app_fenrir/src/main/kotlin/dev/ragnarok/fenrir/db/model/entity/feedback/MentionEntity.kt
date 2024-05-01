package dev.ragnarok.fenrir.db.model.entity.feedback

import androidx.annotation.Keep
import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.model.feedback.FeedbackType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("mention")
class MentionEntity : FeedbackEntity {
    var where: DboEntity? = null
        private set

    @Suppress("UNUSED")
    constructor()
    constructor(@FeedbackType type: Int) {
        this.type = type
    }

    fun setWhere(where: DboEntity?): MentionEntity {
        this.where = where
        return this
    }
}