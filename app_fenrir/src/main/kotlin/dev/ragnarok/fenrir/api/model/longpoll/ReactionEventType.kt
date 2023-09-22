package dev.ragnarok.fenrir.api.model.longpoll

import androidx.annotation.IntDef

@IntDef(
    ReactionEventType.UNKNOW,
    ReactionEventType.I_ADDED_REACTION,
    ReactionEventType.SOMEBODY_ADDED_REACTION,
    ReactionEventType.I_DELETED_REACTION,
    ReactionEventType.SOMEBODY_DELETED_REACTION
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class ReactionEventType {
    companion object {
        @ReactionEventType
        fun toReactionEventType(type: Int): Int {
            return when (type) {
                1 -> I_ADDED_REACTION
                2 -> SOMEBODY_ADDED_REACTION
                3 -> I_DELETED_REACTION
                4 -> SOMEBODY_DELETED_REACTION
                else -> UNKNOW
            }
        }

        const val UNKNOW = 0
        const val I_ADDED_REACTION = 1
        const val SOMEBODY_ADDED_REACTION = 2
        const val I_DELETED_REACTION = 3
        const val SOMEBODY_DELETED_REACTION = 4
    }
}
