package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object FaveVideosColumns : BaseColumns {
    const val TABLENAME = "fave_videos"
    const val VIDEO_ID = "video_id"
    const val OWNER_ID = "owner_id"
    const val VIDEO = "video"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_VIDEO_ID = "$TABLENAME.$VIDEO_ID"
    const val FULL_OWNER_ID = "$TABLENAME.$OWNER_ID"
    const val FULL_VIDEO = "$TABLENAME.$VIDEO"
}