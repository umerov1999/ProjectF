package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object CommentsColumns : BaseColumns {
    /* Код комментария, который получен не с сервиса, а создан на устройстве и не отправлен */
    const val PROCESSING_COMMENT_ID = -1
    const val TABLENAME = "comments"
    const val COMMENT_ID = "comment_id"
    const val FROM_ID = "from_id"
    const val DATE = "date"
    const val TEXT = "text"
    const val REPLY_TO_USER = "reply_to_user"
    const val THREADS_COUNT = "threads_count"
    const val THREADS = "threads"
    const val REPLY_TO_COMMENT = "reply_to_comment"
    const val LIKES = "likes"
    const val USER_LIKES = "user_likes"
    const val CAN_LIKE = "can_like"
    const val CAN_EDIT = "can_edit"
    const val ATTACHMENTS_COUNT = "attachment_count"
    const val DELETED = "deleted"
    const val SOURCE_ID = "source_id"
    const val SOURCE_OWNER_ID = "source_owner_id"
    const val SOURCE_TYPE = "source_type"
    const val SOURCE_ACCESS_KEY = "source_access_key"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_COMMENT_ID = "$TABLENAME.$COMMENT_ID"
    const val FULL_FROM_ID = "$TABLENAME.$FROM_ID"
    const val FULL_DATE = "$TABLENAME.$DATE"
    const val FULL_TEXT = "$TABLENAME.$TEXT"
    const val FULL_REPLY_TO_USER = "$TABLENAME.$REPLY_TO_USER"
    const val FULL_REPLY_TO_COMMENT = "$TABLENAME.$REPLY_TO_COMMENT"
    const val FULL_THREADS_COUNT = "$TABLENAME.$THREADS_COUNT"
    const val FULL_THREADS = "$TABLENAME.$THREADS"
    const val FULL_LIKES = "$TABLENAME.$LIKES"
    const val FULL_USER_LIKES = "$TABLENAME.$USER_LIKES"
    const val FULL_CAN_LIKE = "$TABLENAME.$CAN_LIKE"
    const val FULL_CAN_EDIT = "$TABLENAME.$CAN_EDIT"
    const val FULL_ATTACHMENTS_COUNT = "$TABLENAME.$ATTACHMENTS_COUNT"
    const val FULL_DELETED = "$TABLENAME.$DELETED"
    const val FULL_SOURCE_ID = "$TABLENAME.$SOURCE_ID"
    const val FULL_SOURCE_OWNER_ID = "$TABLENAME.$SOURCE_OWNER_ID"
    const val FULL_SOURCE_TYPE = "$TABLENAME.$SOURCE_TYPE"
    const val FULL_SOURCE_ACCESS_KEY = "$TABLENAME.$SOURCE_ACCESS_KEY"
}