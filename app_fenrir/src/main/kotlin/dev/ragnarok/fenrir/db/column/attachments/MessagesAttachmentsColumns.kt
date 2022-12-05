package dev.ragnarok.fenrir.db.column.attachments

import android.provider.BaseColumns

object MessagesAttachmentsColumns : BaseColumns {
    const val TABLENAME = "messages_attachments"
    const val M_ID = "message_id"
    const val DATA = "data"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_M_ID = "$TABLENAME.$M_ID"
    const val FULL_DATA = "$TABLENAME.$DATA"
}