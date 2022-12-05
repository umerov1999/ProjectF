package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object DocColumns : BaseColumns {
    const val TABLENAME = "docs"
    const val DOC_ID = "doc_id"
    const val OWNER_ID = "owner_id"
    const val TITLE = "title"
    const val SIZE = "size"
    const val EXT = "ext"
    const val URL = "url"
    const val PHOTO = "photo"
    const val GRAFFITI = "graffiti"
    const val VIDEO = "video"
    const val DATE = "date"
    const val TYPE = "type"
    const val ACCESS_KEY = "access_key"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_DOC_ID = "$TABLENAME.$DOC_ID"
    const val FULL_OWNER_ID = "$TABLENAME.$OWNER_ID"
    const val FULL_TITLE = "$TABLENAME.$TITLE"
    const val FULL_SIZE = "$TABLENAME.$SIZE"
    const val FULL_EXT = "$TABLENAME.$EXT"
    const val FULL_URL = "$TABLENAME.$URL"
    const val FULL_PHOTO = "$TABLENAME.$PHOTO"
    const val FULL_GRAFFITI = "$TABLENAME.$GRAFFITI"
    const val FULL_VIDEO = "$TABLENAME.$VIDEO"
    const val FULL_DATE = "$TABLENAME.$DATE"
    const val FULL_TYPE = "$TABLENAME.$TYPE"
    const val FULL_ACCESS_KEY = "$TABLENAME.$ACCESS_KEY"
}