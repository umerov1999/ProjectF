package dev.ragnarok.fenrir.db.model.entity.feedback

import androidx.annotation.Keep
import dev.ragnarok.fenrir.model.feedback.FeedbackType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("users")
class UsersEntity : FeedbackEntity {
    var owners: LongArray? = null
        private set

    @Suppress("UNUSED")
    constructor()
    constructor(@FeedbackType type: Int) {
        this.type = type
    }

    fun setOwners(ids: LongArray?): UsersEntity {
        owners = ids
        return this
    }
}