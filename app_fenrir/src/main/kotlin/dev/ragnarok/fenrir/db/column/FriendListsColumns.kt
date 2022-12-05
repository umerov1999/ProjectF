package dev.ragnarok.fenrir.db.column

import android.content.ContentValues
import android.provider.BaseColumns
import dev.ragnarok.fenrir.api.model.VKApiFriendList

object FriendListsColumns : BaseColumns {
    const val TABLENAME = "friend_lists"
    const val USER_ID = "user_id"
    const val LIST_ID = "list_id"
    const val NAME = "name"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_USER_ID = "$TABLENAME.$USER_ID"
    const val FULL_LIST_ID = "$TABLENAME.$LIST_ID"
    const val FULL_NAME = "$TABLENAME.$NAME"
    fun getCv(userId: Int, list: VKApiFriendList): ContentValues {
        val cv = ContentValues()
        cv.put(USER_ID, userId)
        cv.put(LIST_ID, list.id)
        cv.put(NAME, list.name)
        return cv
    }
}