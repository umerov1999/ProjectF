package dev.ragnarok.fenrir.db.column

import android.content.ContentValues
import android.provider.BaseColumns

object RelationshipsColumns : BaseColumns {
    const val TYPE_FRIEND = 1
    const val TYPE_FOLLOWER = 2
    const val TYPE_GROUP_MEMBER = 3
    const val TYPE_MEMBER = 4
    const val TYPE_REQUESTS = 5
    const val TABLENAME = "relationships"
    const val OBJECT_ID = "object_id"
    const val SUBJECT_ID = "subject_id"
    const val TYPE = "type"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_OBJECT_ID = "$TABLENAME.$OBJECT_ID"
    const val FULL_SUBJECT_ID = "$TABLENAME.$SUBJECT_ID"
    const val FULL_TYPE = "$TABLENAME.$TYPE"
    const val FOREIGN_SUBJECT_USER_FIRST_NAME = "subject_user_first_name"
    const val FOREIGN_SUBJECT_USER_LAST_NAME = "subject_user_last_name"
    const val FOREIGN_SUBJECT_USER_ONLINE = "subject_user_online"
    const val FOREIGN_SUBJECT_USER_ONLINE_MOBILE = "subject_user_online_mobile"
    const val FOREIGN_SUBJECT_USER_ONLINE_APP = "subject_user_online_app"
    const val FOREIGN_SUBJECT_USER_PHOTO_50 = "subject_user_photo_50"
    const val FOREIGN_SUBJECT_USER_PHOTO_100 = "subject_user_photo_100"
    const val FOREIGN_SUBJECT_USER_PHOTO_200 = "subject_user_photo_200"
    const val FOREIGN_SUBJECT_USER_PHOTO_MAX = "subject_user_photo_max"
    const val FOREIGN_SUBJECT_USER_HAS_UNSEEN_STORIES = "subject_user_has_unseen_stories"
    const val FOREIGN_SUBJECT_USER_LAST_SEEN = "subject_user_last_seen"
    const val FOREIGN_SUBJECT_USER_PLATFORM = "subject_user_platform"
    const val FOREIGN_SUBJECT_USER_STATUS = "subject_user_status"
    const val FOREIGN_SUBJECT_USER_SEX = "subject_user_sex"
    const val FOREIGN_SUBJECT_USER_IS_FRIEND = "subject_user_is_friend"
    const val FOREIGN_SUBJECT_USER_FRIEND_STATUS = "subject_user_friend_status"
    const val FOREIGN_SUBJECT_USER_WRITE_MESSAGE_STATUS = "subject_user_write_message_status"
    const val FOREIGN_SUBJECT_USER_BDATE = "subject_user_bdate"
    const val FOREIGN_SUBJECT_USER_IS_USER_BLACK_LIST = "subject_user_is_user_in_black_list"
    const val FOREIGN_SUBJECT_USER_IS_BLACK_LISTED = "subject_user_is_black_listed"
    const val FOREIGN_SUBJECT_USER_IS_CAN_ACCESS_CLOSED = "subject_user_is_can_access_closed"
    const val FOREIGN_SUBJECT_USER_IS_VERIFIED = "subject_user_is_verified"
    const val FOREIGN_SUBJECT_USER_MAIDEN_NAME = "subject_user_maiden_name"
    const val FOREIGN_SUBJECT_GROUP_NAME = "subject_group_name"
    const val FOREIGN_SUBJECT_GROUP_SCREEN_NAME = "subject_group_screen_name"
    const val FOREIGN_SUBJECT_GROUP_PHOTO_50 = "subject_group_photo_50"
    const val FOREIGN_SUBJECT_GROUP_PHOTO_100 = "subject_group_photo_100"
    const val FOREIGN_SUBJECT_GROUP_PHOTO_200 = "subject_group_photo_200"
    const val FOREIGN_SUBJECT_GROUP_IS_CLOSED = "subject_group_is_closed"
    const val FOREIGN_SUBJECT_GROUP_IS_BLACK_LISTED = "subject_group_is_blacklisted"
    const val FOREIGN_SUBJECT_GROUP_IS_VERIFIED = "subject_group_is_verified"
    const val FOREIGN_SUBJECT_GROUP_IS_ADMIN = "subject_group_is_admin"
    const val FOREIGN_SUBJECT_GROUP_ADMIN_LEVEL = "subject_group_admin_level"
    const val FOREIGN_SUBJECT_GROUP_IS_MEMBER = "subject_group_is_member"
    const val FOREIGN_SUBJECT_GROUP_MEMBERS_COUNT = "subject_group_members_count"
    const val FOREIGN_SUBJECT_GROUP_MEMBER_STATUS = "subject_group_member_status"
    const val FOREIGN_SUBJECT_GROUP_TYPE = "subject_group_type"
    const val FOREIGN_SUBJECT_GROUP_HAS_UNSEEN_STORIES = "subject_group_has_unseen_stories"

    fun getCV(objectId: Long, subjectId: Long, type: Int): ContentValues {
        val cv = ContentValues()
        cv.put(OBJECT_ID, objectId)
        cv.put(SUBJECT_ID, subjectId)
        cv.put(TYPE, type)
        return cv
    }
}