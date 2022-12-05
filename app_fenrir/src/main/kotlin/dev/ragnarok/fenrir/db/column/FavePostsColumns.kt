package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object FavePostsColumns : BaseColumns {
    const val TABLENAME = "fave_posts"
    const val POST = "post"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_POST = "$TABLENAME.$POST"
}