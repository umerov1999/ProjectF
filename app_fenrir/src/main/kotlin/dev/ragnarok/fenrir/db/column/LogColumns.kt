package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object LogColumns : BaseColumns {
    const val TABLENAME = "logs"
    const val TYPE = "eventtype"
    const val DATE = "eventdate"
    const val TAG = "tag"
    const val BODY = "body"
}