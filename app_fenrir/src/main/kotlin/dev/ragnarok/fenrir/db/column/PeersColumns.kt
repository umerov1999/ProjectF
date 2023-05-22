package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object PeersColumns : BaseColumns {
    const val TABLENAME = "peers_of_dialogs"
    const val UNREAD = "unread"
    const val TITLE = "title"
    const val IN_READ = "in_read"
    const val OUT_READ = "out_read"
    const val PHOTO_50 = "photo_50"
    const val PHOTO_100 = "photo_100"
    const val PHOTO_200 = "photo_200"
    const val KEYBOARD = "current_keyboard"
    const val MAJOR_ID = "major_id"
    const val MINOR_ID = "minor_id"
    const val PINNED = "pinned"
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
    const val FULL_KEYBOARD = "$TABLENAME.$KEYBOARD"
    const val FULL_MAJOR_ID = "$TABLENAME.$MAJOR_ID"
    const val FULL_MINOR_ID = "$TABLENAME.$MINOR_ID"
    const val FULL_PINNED = "$TABLENAME.$PINNED"
    const val FULL_LAST_MESSAGE_ID = "$TABLENAME.$LAST_MESSAGE_ID"
    const val FULL_ACL = "$TABLENAME.$ACL"
    const val FULL_IS_GROUP_CHANNEL = "$TABLENAME.$IS_GROUP_CHANNEL"
}