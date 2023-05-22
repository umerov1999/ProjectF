package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object GroupsDetailsColumns : BaseColumns {
    const val TABLENAME = "groups_details"
    const val DATA = "data"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_DATA = "$TABLENAME.$DATA"
}