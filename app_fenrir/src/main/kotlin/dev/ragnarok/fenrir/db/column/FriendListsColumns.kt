package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object FriendListsColumns : BaseColumns {
    const val TABLENAME = "friend_lists"
    const val USER_ID = "user_id"
    const val LIST_ID = "list_id"
    const val NAME = "name"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_USER_ID = "$TABLENAME.$USER_ID"
    const val FULL_LIST_ID = "$TABLENAME.$LIST_ID"
    const val FULL_NAME = "$TABLENAME.$NAME"
}