package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object NewsColumns : BaseColumns {
    // Переменные для определения в какой массив добавлять вложения
    // (attachments, copy_history, photos, photo_tags и notes из API),
    // на них ссылаются идентификаторы из table NEWS_ATTACHMENTS, поле ARRAY_TYPE_CODE;
    //public static final int TYPE_CODE_ATTACHMENTS = 1;
    //public static final int TYPE_CODE_COPY_HISTORY = 2;
    //public static final int TYPE_CODE_PHOTOS = 3;
    //public static final int TYPE_CODE_PHOTOS_TAGS = 4;
    //public static final int TYPE_CODE_NOTES = 5;
    const val TABLENAME = "news"
    const val TYPE = "type"
    const val SOURCE_ID = "source_id"
    const val DATE = "date"
    const val POST_ID = "post_id"
    const val POST_TYPE = "post_type"
    const val FINAL_POST = "final_post"
    const val COPY_OWNER_ID = "copy_owner_id"
    const val COPY_POST_ID = "copy_post_id"
    const val COPY_POST_DATE = "copy_post_date"
    const val TEXT = "text"
    const val CAN_EDIT = "can_edit"
    const val CAN_DELETE = "can_delete"
    const val COMMENT_COUNT = "comment_count"
    const val COMMENT_CAN_POST = "comment_can_post"
    const val LIKE_COUNT = "like_count"
    const val USER_LIKE = "user_like"
    const val CAN_LIKE = "can_like"
    const val CAN_PUBLISH = "can_publish"
    const val REPOSTS_COUNT = "reposts_count"
    const val USER_REPOSTED = "user_reposted"
    const val COPYRIGHT_BLOB = "copyright_blob"
    const val IS_DONUT = "is_donut"

    const val TAG_FRIENDS = "friends_tag"
    const val ATTACHMENTS_BLOB = "attachments_blob"
    const val VIEWS = "views"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID

    //public static final String HAS_COPY_HISTORY = "has_copy_history";
    const val FULL_TYPE = "$TABLENAME.$TYPE"
    const val FULL_SOURCE_ID = "$TABLENAME.$SOURCE_ID"
    const val FULL_DATE = "$TABLENAME.$DATE"
    const val FULL_POST_ID = "$TABLENAME.$POST_ID"
    const val FULL_POST_TYPE = "$TABLENAME.$POST_TYPE"
    const val FULL_FINAL_POST = "$TABLENAME.$FINAL_POST"
    const val FULL_COPY_OWNER_ID = "$TABLENAME.$COPY_OWNER_ID"
    const val FULL_COPY_POST_ID = "$TABLENAME.$COPY_POST_ID"
    const val FULL_COPY_POST_DATE = "$TABLENAME.$COPY_POST_DATE"
    const val FULL_TEXT = "$TABLENAME.$TEXT"
    const val FULL_COPYRIGHT_BLOB = "$TABLENAME.$COPYRIGHT_BLOB"
    const val FULL_CAN_EDIT = "$TABLENAME.$CAN_EDIT"
    const val FULL_CAN_DELETE = "$TABLENAME.$CAN_DELETE"
    const val FULL_COMMENT_COUNT = "$TABLENAME.$COMMENT_COUNT"
    const val FULL_COMMENT_CAN_POST = "$TABLENAME.$COMMENT_CAN_POST"
    const val FULL_LIKE_COUNT = "$TABLENAME.$LIKE_COUNT"
    const val FULL_USER_LIKE = "$TABLENAME.$USER_LIKE"
    const val FULL_CAN_LIKE = "$TABLENAME.$CAN_LIKE"
    const val FULL_CAN_PUBLISH = "$TABLENAME.$CAN_PUBLISH"
    const val FULL_REPOSTS_COUNT = "$TABLENAME.$REPOSTS_COUNT"
    const val FULL_USER_REPOSTED = "$TABLENAME.$USER_REPOSTED"

    const val FULL_TAG_FRIENDS = "$TABLENAME.$TAG_FRIENDS"
    const val FULL_ATTACHMENTS_BLOB = "$TABLENAME.$ATTACHMENTS_BLOB"
    const val FULL_VIEWS = "$TABLENAME.$VIEWS"
    const val FULL_IS_DONUT = "$TABLENAME.$IS_DONUT"
}