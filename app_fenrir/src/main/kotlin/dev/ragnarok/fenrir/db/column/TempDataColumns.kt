package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object TempDataColumns : BaseColumns {
    const val TABLENAME = "temp_app_data"
    const val OWNER_ID = "owner_id"
    const val SOURCE_ID = "source_id"
    const val DATA = "data"
}