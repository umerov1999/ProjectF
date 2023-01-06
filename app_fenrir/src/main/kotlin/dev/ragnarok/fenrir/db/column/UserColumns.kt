package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object UserColumns : BaseColumns {
    /**
     * The table name of books = "books"
     */
    const val TABLENAME = "users"

    //Columns
    const val FIRST_NAME = "first_name"
    const val LAST_NAME = "last_name"
    const val ONLINE = "online"
    const val ONLINE_MOBILE = "online_mobile"
    const val ONLINE_APP = "online_app"
    const val PHOTO_50 = "photo_50"
    const val PHOTO_100 = "photo_100"
    const val PHOTO_200 = "photo_200"
    const val PHOTO_MAX = "photo_max"
    const val LAST_SEEN = "last_seen"
    const val PLATFORM = "platform"
    const val USER_STATUS = "user_status"
    const val SEX = "sex"
    const val DOMAIN = "domain"
    const val IS_FRIEND = "is_friend"
    const val FRIEND_STATUS = "friend_status"
    const val WRITE_MESSAGE_STATUS = "write_message_status"
    const val BDATE = "bdate"
    const val IS_USER_BLACK_LIST = "is_user_in_black_list"
    const val IS_BLACK_LISTED = "is_black_listed"
    const val IS_CAN_ACCESS_CLOSED = "is_can_access_closed"
    const val IS_VERIFIED = "is_verified"
    const val MAIDEN_NAME = "maiden_name"
    const val HAS_UNSEEN_STORIES = "has_unseen_stories"

    /**
     * The id of the user, includes tablename prefix
     * <P>Type: INT</P>
     */
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_FIRST_NAME = "$TABLENAME.$FIRST_NAME"
    const val FULL_LAST_NAME = "$TABLENAME.$LAST_NAME"
    const val FULL_ONLINE = "$TABLENAME.$ONLINE"
    const val FULL_ONLINE_MOBILE = "$TABLENAME.$ONLINE_MOBILE"
    const val FULL_ONLINE_APP = "$TABLENAME.$ONLINE_APP"
    const val FULL_PHOTO_50 = "$TABLENAME.$PHOTO_50"
    const val FULL_PHOTO_100 = "$TABLENAME.$PHOTO_100"
    const val FULL_PHOTO_200 = "$TABLENAME.$PHOTO_200"
    const val FULL_PHOTO_MAX = "$TABLENAME.$PHOTO_MAX"
    const val FULL_LAST_SEEN = "$TABLENAME.$LAST_SEEN"
    const val FULL_PLATFORM = "$TABLENAME.$PLATFORM"
    const val FULL_USER_STATUS = "$TABLENAME.$USER_STATUS"
    const val FULL_SEX = "$TABLENAME.$SEX"
    const val FULL_DOMAIN = "$TABLENAME.$DOMAIN"
    const val FULL_IS_FRIEND = "$TABLENAME.$IS_FRIEND"
    const val FULL_FRIEND_STATUS = "$TABLENAME.$FRIEND_STATUS"
    const val FULL_WRITE_MESSAGE_STATUS = "$TABLENAME.$WRITE_MESSAGE_STATUS"
    const val FULL_IS_USER_BLACK_LIST = "$TABLENAME.$IS_USER_BLACK_LIST"
    const val FULL_IS_BLACK_LISTED = "$TABLENAME.$IS_BLACK_LISTED"
    const val FULL_IS_CAN_ACCESS_CLOSED = "$TABLENAME.$IS_CAN_ACCESS_CLOSED"
    const val FULL_IS_VERIFIED = "$TABLENAME.$IS_VERIFIED"
    const val FULL_MAIDEN_NAME = "$TABLENAME.$MAIDEN_NAME"
    const val FULL_BDATE = "$TABLENAME.$BDATE"
    const val FULL_HAS_UNSEEN_STORIES = "$TABLENAME.$HAS_UNSEEN_STORIES"
}