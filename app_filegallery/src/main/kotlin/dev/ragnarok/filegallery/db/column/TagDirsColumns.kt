package dev.ragnarok.filegallery.db.column

import android.provider.BaseColumns

object TagDirsColumns : BaseColumns {
    const val TABLENAME = "tag_dirs"
    const val OWNER_ID = "owner_id"
    const val NAME = "name"
    const val PATH = "path"
    const val TYPE = "type"
}