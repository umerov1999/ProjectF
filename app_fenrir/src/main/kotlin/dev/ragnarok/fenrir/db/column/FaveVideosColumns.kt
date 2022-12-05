package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object FaveVideosColumns : BaseColumns {
    const val TABLENAME = "fave_videos"
    const val VIDEO = "video"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_VIDEO = "$TABLENAME.$VIDEO"
}