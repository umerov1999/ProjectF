package dev.ragnarok.fenrir.db.model.entity

class ReactionAssetEntity {
    var big_animation: String? = null
        private set
    var small_animation: String? = null
        private set
    var static: String? = null
        private set
    var reaction_id = 0
        private set

    fun setBigAnimation(big_animation: String?): ReactionAssetEntity {
        this.big_animation = big_animation
        return this
    }

    fun setSmallAnimation(small_animation: String?): ReactionAssetEntity {
        this.small_animation = small_animation
        return this
    }

    fun setStatic(static: String?): ReactionAssetEntity {
        this.static = static
        return this
    }

    fun setReactionId(reaction_id: Int): ReactionAssetEntity {
        this.reaction_id = reaction_id
        return this
    }
}
