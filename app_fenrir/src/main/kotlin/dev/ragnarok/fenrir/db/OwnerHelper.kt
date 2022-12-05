package dev.ragnarok.fenrir.db

import android.content.Context
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getGroupsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getUserContentUriFor
import dev.ragnarok.fenrir.db.column.GroupColumns
import dev.ragnarok.fenrir.db.column.UserColumns
import dev.ragnarok.fenrir.getString

object OwnerHelper {
    fun loadOwnerFullName(context: Context, aid: Int, ownerId: Int): String? {
        if (ownerId == 0) return null
        var result: String? = null
        if (ownerId > 0) {
            val uCursor = context.contentResolver.query(
                getUserContentUriFor(aid),
                null, BaseColumns._ID + " = ?", arrayOf(ownerId.toString()), null
            )
            if (uCursor != null) {
                if (uCursor.moveToNext()) {
                    result =
                        uCursor.getString(UserColumns.FIRST_NAME) +
                                " " + uCursor.getString(UserColumns.LAST_NAME)
                }
                uCursor.close()
            }
        } else {
            val gCursor = context.contentResolver.query(
                getGroupsContentUriFor(aid),
                null, BaseColumns._ID + " = ?", arrayOf((-ownerId).toString()), null
            )
            if (gCursor != null) {
                if (gCursor.moveToNext()) {
                    result = gCursor.getString(GroupColumns.NAME)
                }
                gCursor.close()
            }
        }
        return result
    }
}