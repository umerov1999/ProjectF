package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class ReactionEntity {
    var count: Int = 0
        private set
    var reaction_id = 0
        private set

    fun setCount(count: Int): ReactionEntity {
        this.count = count
        return this
    }

    fun setReactionId(reaction_id: Int): ReactionEntity {
        this.reaction_id = reaction_id
        return this
    }
}
