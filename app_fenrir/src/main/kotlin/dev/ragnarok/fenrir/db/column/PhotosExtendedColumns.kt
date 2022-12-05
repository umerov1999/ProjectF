package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object PhotosExtendedColumns : BaseColumns {
    const val TABLENAME = "photos_extended"

    //Columns
    const val DB_OWNER_ID = "db_owner_id"
    const val DB_ALBUM_ID = "db_album_id"

    const val PHOTO_ID = "photo_id"
    const val ALBUM_ID = "album_id"
    const val OWNER_ID = "owner_id"
    const val WIDTH = "width"
    const val HEIGHT = "height"
    const val TEXT = "text"
    const val DATE = "date"
    const val SIZES = "sizes"
    const val USER_LIKES = "user_likes"
    const val CAN_COMMENT = "can_comment"
    const val LIKES = "likes"
    const val REPOSTS = "reposts"
    const val COMMENTS = "comments"
    const val TAGS = "tags"
    const val ACCESS_KEY = "access_key"
    const val DELETED = "deleted"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_DB_OWNER_ID = "$TABLENAME.$DB_OWNER_ID"
    const val FULL_DB_ALBUM_ID = "$TABLENAME.$DB_ALBUM_ID"
    const val FULL_PHOTO_ID = "$TABLENAME.$PHOTO_ID"
    const val FULL_ALBUM_ID = "$TABLENAME.$ALBUM_ID"
    const val FULL_OWNER_ID = "$TABLENAME.$OWNER_ID"
    const val FULL_WIDTH = "$TABLENAME.$WIDTH"
    const val FULL_HEIGHT = "$TABLENAME.$HEIGHT"
    const val FULL_TEXT = "$TABLENAME.$TEXT"
    const val FULL_DATE = "$TABLENAME.$DATE"
    const val FULL_SIZES = "$TABLENAME.$SIZES"
    const val FULL_USER_LIKES = "$TABLENAME.$USER_LIKES"
    const val FULL_CAN_COMMENT = "$TABLENAME.$CAN_COMMENT"
    const val FULL_LIKES = "$TABLENAME.$LIKES"
    const val FULL_REPOSTS = "$TABLENAME.$REPOSTS"
    const val FULL_COMMENTS = "$TABLENAME.$COMMENTS"
    const val FULL_TAGS = "$TABLENAME.$TAGS"
    const val FULL_ACCESS_KEY = "$TABLENAME.$ACCESS_KEY"
    const val FULL_DELETED = "$TABLENAME.$DELETED"
}