package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object UsersDetailsColumns : BaseColumns {
    const val TABLENAME = "users_details"
    const val DATA = "data"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_DATA = "$TABLENAME.$DATA"
}