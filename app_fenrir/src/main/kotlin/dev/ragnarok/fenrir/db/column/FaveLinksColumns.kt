package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object FaveLinksColumns : BaseColumns {
    const val TABLENAME = "fave_link"
    const val LINK_ID = "link_id"
    const val URL = "url"
    const val TITLE = "title"
    const val DESCRIPTION = "description"
    const val PHOTO = "photo"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_LINK_ID = "$TABLENAME.$LINK_ID"
    const val FULL_URL = "$TABLENAME.$URL"
    const val FULL_TITLE = "$TABLENAME.$TITLE"
    const val FULL_DESCRIPTION = "$TABLENAME.$DESCRIPTION"
    const val FULL_PHOTO = "$TABLENAME.$PHOTO"
}