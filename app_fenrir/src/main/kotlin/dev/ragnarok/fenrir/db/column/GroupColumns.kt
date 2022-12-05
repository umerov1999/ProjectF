package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object GroupColumns : BaseColumns {
    const val TABLENAME = "groups"
    const val NAME = "name"
    const val SCREEN_NAME = "screen_name"
    const val IS_CLOSED = "is_closed"
    const val IS_ADMIN = "is_admin"
    const val ADMIN_LEVEL = "admin_level"
    const val IS_MEMBER = "is_member"
    const val MEMBER_STATUS = "member_status"
    const val MEMBERS_COUNT = "members_count"
    const val IS_VERIFIED = "is_verified"
    const val IS_BLACK_LISTED = "is_black_listed"
    const val TYPE = "type"
    const val PHOTO_50 = "photo_50"
    const val PHOTO_100 = "photo_100"
    const val PHOTO_200 = "photo_200"
    const val CAN_ADD_TOPICS = "can_add_topics"
    const val TOPICS_ORDER = "topics_order"
    const val API_FIELDS =
        "name,screen_name,is_closed,verified,members_count,is_admin,admin_level," +
                "is_member,member_status,type,photo_50,photo_100,photo_200,menu,ban_info"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_NAME = "$TABLENAME.$NAME"
    const val FULL_SCREEN_NAME = "$TABLENAME.$SCREEN_NAME"
    const val FULL_IS_CLOSED = "$TABLENAME.$IS_CLOSED"
    const val FULL_IS_ADMIN = "$TABLENAME.$IS_ADMIN"
    const val FULL_ADMIN_LEVEL = "$TABLENAME.$ADMIN_LEVEL"
    const val FULL_IS_MEMBER = "$TABLENAME.$IS_MEMBER"
    const val FULL_MEMBER_STATUS = "$TABLENAME.$MEMBER_STATUS"
    const val FULL_MEMBERS_COUNT = "$TABLENAME.$MEMBERS_COUNT"
    const val FULL_IS_VERIFIED = "$TABLENAME.$IS_VERIFIED"
    const val FULL_TYPE = "$TABLENAME.$TYPE"
    const val FULL_PHOTO_50 = "$TABLENAME.$PHOTO_50"
    const val FULL_PHOTO_100 = "$TABLENAME.$PHOTO_100"
    const val FULL_PHOTO_200 = "$TABLENAME.$PHOTO_200"
    const val FULL_CAN_ADD_TOPICS = "$TABLENAME.$CAN_ADD_TOPICS"
    const val FULL_TOPICS_ORDER = "$TABLENAME.$TOPICS_ORDER"
    const val FULL_IS_BLACK_LISTED = "$TABLENAME.$IS_BLACK_LISTED"
}