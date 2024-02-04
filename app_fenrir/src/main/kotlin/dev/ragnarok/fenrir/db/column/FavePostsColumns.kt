package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object FavePostsColumns : BaseColumns {
    const val TABLENAME = "fave_posts"
    const val POST_ID = "post_id"
    const val OWNER_ID = "owner_id"
    const val POST = "post"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_POST_ID = "$TABLENAME.$POST_ID"
    const val FULL_OWNER_ID = "$TABLENAME.$OWNER_ID"
    const val FULL_POST = "$TABLENAME.$POST"
}