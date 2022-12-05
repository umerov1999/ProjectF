package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object FavePageColumns : BaseColumns {
    const val TABLENAME = "fave_pages"
    const val GROUPSTABLENAME = "fave_groups"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_GROUPS_ID = GROUPSTABLENAME + "." + BaseColumns._ID
    const val DESCRIPTION = "description"
    const val UPDATED_TIME = "updated_time"
    const val FAVE_TYPE = "fave_type"
}