package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object NotificationColumns : BaseColumns {
    const val TABLENAME = "notifications"
    const val DATE = "date"
    const val CONTENT_PACK = "content_pack"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_DATE = "$TABLENAME.$DATE"
    const val FULL_CONTENT_PACK = "$TABLENAME.$CONTENT_PACK"
}