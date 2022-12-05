package dev.ragnarok.fenrir.db.column

import android.content.ContentValues
import android.provider.BaseColumns
import dev.ragnarok.fenrir.api.model.VKApiChat
import dev.ragnarok.fenrir.api.model.VKApiMessage

object DialogsColumns : BaseColumns {
    const val TABLENAME = "dialogs"
    const val UNREAD = "unread"
    const val TITLE = "title"
    const val IN_READ = "in_read"
    const val OUT_READ = "out_read"
    const val PHOTO_50 = "photo_50"
    const val PHOTO_100 = "photo_100"
    const val PHOTO_200 = "photo_200"
    const val MAJOR_ID = "major_id"
    const val MINOR_ID = "minor_id"
    const val LAST_MESSAGE_ID = "last_message_id"
    const val ACL = "acl"
    const val IS_GROUP_CHANNEL = "is_group_channel"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_UNREAD = "$TABLENAME.$UNREAD"
    const val FULL_TITLE = "$TABLENAME.$TITLE"
    const val FULL_IN_READ = "$TABLENAME.$IN_READ"
    const val FULL_OUT_READ = "$TABLENAME.$OUT_READ"
    const val FULL_PHOTO_50 = "$TABLENAME.$PHOTO_50"
    const val FULL_PHOTO_100 = "$TABLENAME.$PHOTO_100"
    const val FULL_PHOTO_200 = "$TABLENAME.$PHOTO_200"
    const val FULL_LAST_MESSAGE_ID = "$TABLENAME.$LAST_MESSAGE_ID"
    const val FULL_ACL = "$TABLENAME.$ACL"
    const val FULL_IS_GROUP_CHANNEL = "$TABLENAME.$IS_GROUP_CHANNEL"
    const val FULL_MAJOR_ID = "$TABLENAME.$MAJOR_ID"
    const val FULL_MINOR_ID = "$TABLENAME.$MINOR_ID"
    const val FOREIGN_MESSAGE_FROM_ID = "message_from_id"
    const val FOREIGN_MESSAGE_BODY = "message_body"
    const val FOREIGN_MESSAGE_DATE = "message_date"
    const val FOREIGN_MESSAGE_OUT = "message_out"

    //public static final String FOREIGN_MESSAGE_READ_STATE = "message_read_state";
    const val FOREIGN_MESSAGE_HAS_ATTACHMENTS = "message_has_attachments"
    const val FOREIGN_MESSAGE_FWD_COUNT = "message_forward_count"
    const val FOREIGN_MESSAGE_ACTION = "message_action"
    const val FOREIGN_MESSAGE_ENCRYPTED = "message_encrypted"

    fun getCV(chat: VKApiChat): ContentValues {
        val cv = ContentValues()
        cv.put(BaseColumns._ID, VKApiMessage.CHAT_PEER + chat.id)
        cv.put(TITLE, chat.title)
        cv.put(PHOTO_200, chat.photo_200)
        cv.put(PHOTO_100, chat.photo_100)
        cv.put(PHOTO_50, chat.photo_50)
        return cv
    }
}