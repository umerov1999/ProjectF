package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object FavePhotosColumns : BaseColumns {
    const val TABLENAME = "fave_photos"
    const val PHOTO_ID = "photo_id"
    const val OWNER_ID = "owner_id"
    const val POST_ID = "post_id"
    const val PHOTO = "photo"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_PHOTO_ID = "$TABLENAME.$PHOTO_ID"
    const val FULL_OWNER_ID = "$TABLENAME.$OWNER_ID"
    const val FULL_POST_ID = "$TABLENAME.$POST_ID"
    const val FULL_PHOTO = "$TABLENAME.$PHOTO"
}