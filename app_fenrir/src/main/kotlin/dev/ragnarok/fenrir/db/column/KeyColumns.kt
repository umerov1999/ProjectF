package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object KeyColumns : BaseColumns {
    const val TABLENAME = "keys"
    const val VERSION = "version"
    const val PEER_ID = "peer_id"
    const val SESSION_ID = "session_id"
    const val DATE = "date"
    const val START_SESSION_MESSAGE_ID = "start_mid"
    const val END_SESSION_MESSAGE_ID = "end_mid"
    const val OUT_KEY = "outkey"
    const val IN_KEY = "inkey"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_VERSION = "$TABLENAME.$VERSION"
    const val FULL_PEER_ID = "$TABLENAME.$PEER_ID"
    const val FULL_SESSION_ID = "$TABLENAME.$SESSION_ID"
    const val FULL_DATE = "$TABLENAME.$DATE"
    const val FULL_START_SESSION_MESSAGE_ID = "$TABLENAME.$START_SESSION_MESSAGE_ID"
    const val FULL_END_SESSION_MESSAGE_ID = "$TABLENAME.$END_SESSION_MESSAGE_ID"
    const val FULL_OUT_KEY = "$TABLENAME.$OUT_KEY"
    const val FULL_IN_KEY = "$TABLENAME.$IN_KEY"
}