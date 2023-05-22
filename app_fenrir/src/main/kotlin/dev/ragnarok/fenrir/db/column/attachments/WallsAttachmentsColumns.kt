package dev.ragnarok.fenrir.db.column.attachments

import android.provider.BaseColumns

object WallsAttachmentsColumns : BaseColumns {
    const val TABLENAME = "walls_attachments"
    const val P_ID = "post_id"
    const val DATA = "data"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_P_ID = "$TABLENAME.$P_ID"
    const val FULL_DATA = "$TABLENAME.$DATA"
}