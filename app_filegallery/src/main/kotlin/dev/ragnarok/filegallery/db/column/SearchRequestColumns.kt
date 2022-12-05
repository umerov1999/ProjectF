package dev.ragnarok.filegallery.db.column

import android.provider.BaseColumns

object SearchRequestColumns : BaseColumns {
    const val TABLENAME = "search_app_request"
    const val SOURCE_ID = "source_id"
    const val QUERY = "query"
}