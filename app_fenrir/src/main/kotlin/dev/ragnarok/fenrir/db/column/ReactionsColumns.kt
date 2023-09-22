package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object ReactionsColumns : BaseColumns {
    const val TABLENAME = "reaction_assets"
    const val REACTION_ID = "reaction_id"
    const val ACCOUNT_ID = "account_id"
    const val STATIC = "static"
    const val SMALL_ANIMATION = "small_animation"
    const val BIG_ANIMATION = "big_animation"
}
