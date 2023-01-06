package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.util.Utils

object Fields {
    object USER_FIELDS {
        const val ABOUT = "about"
        const val ACTIVITIES = "activities"
        const val ACTIVITY = "activity"
        const val BDATE = "bdate"
        const val BLACKLISTED = "blacklisted"
        const val BLACKLISTED_BY_ME = "blacklisted_by_me"
        const val BOOKS = "books"
        const val CAN_ACCESS_CLOSED = "can_access_closed"
        const val CAN_POST = "can_post"
        const val CAN_SEE_ALL_POSTS = "can_see_all_posts"
        const val CAN_SEND_FRIEND_REQUEST = "can_send_friend_request"
        const val CAN_WRITE_PRIVATE_MESSAGE = "can_write_private_message"
        const val CAREER = "career"
        const val CITY = "city"
        const val COMMON_COUNT = "common_count"
        const val CONNECTIONS = "connections"
        const val CONTACTS = "contacts"
        const val COUNTERS = "counters"
        const val COUNTRY = "country"
        const val COVER = "cover"
        const val DOMAIN = "domain"
        const val EDUCATION = "education"
        const val FIRST_NAME = "first_name"
        const val FRIEND_STATUS = "friend_status"
        const val GAMES = "games"
        const val HAS_MOBILE = "has_mobile"
        const val HAS_UNSEEN_STORIES = "has_unseen_stories"
        const val HOME_TOWN = "home_town"
        const val INTERESTS = "interests"
        const val IS_CLOSED = "is_closed"
        const val IS_FAVORITE = "is_favorite"
        const val IS_FRIEND = "is_friend"
        const val IS_SUBSCRIBED = "is_subscribed"
        const val LAST_NAME = "last_name"
        const val LAST_SEEN = "last_seen"
        const val MAIDEN_NAME = "maiden_name"
        const val MILITARY = "military"
        const val MOVIES = "movies"
        const val MUSIC = "music"
        const val OCCUPATION = "occupation"
        const val ONLINE = "online"
        const val ONLINE_APP = "online_app"
        const val ONLINE_MOBILE = "online_mobile"
        const val PERSONAL = "personal"
        const val PHOTO_100 = "photo_100"
        const val PHOTO_200 = "photo_200"
        const val PHOTO_50 = "photo_50"
        const val PHOTO_ID = "photo_id"
        const val PHOTO_MAX_ORIG = "photo_max_orig"
        const val PLATFORM = "platform"
        const val QUOTES = "quotes"
        const val RELATION = "relation"
        const val RELATIVES = "relatives"
        const val SCHOOLS = "schools"
        const val SCREEN_NAME = "screen_name"
        const val SEX = "sex"
        const val SITE = "site"
        const val STATUS = "status"
        const val TIMEZONE = "timezone"
        const val TV = "tv"
        const val UNIVERSITIES = "universities"
        const val VERIFIED = "verified"
        const val WALL_DEFAULT = "wall_default"
    }

    object GROUP_FIELDS {
        const val ACTIVITY = "activity"
        const val ADMIN_LEVEL = "admin_level"
        const val BAN_INFO = "ban_info"
        const val BLACKLISTED = "blacklisted"
        const val CAN_CREATE_TOPIC = "can_create_topic"
        const val CAN_MESSAGE = "can_message"
        const val CAN_POST = "can_post"
        const val CAN_SEE_ALL_POSTS = "can_see_all_posts"
        const val CAN_UPLOAD_DOC = "can_upload_doc"
        const val CAN_UPLOAD_VIDEO = "can_upload_video"
        const val CHATS_STATUS = "chats_status"
        const val CITY = "city"
        const val CONTACTS = "contacts"
        const val COUNTERS = "counters"
        const val COUNTRY = "country"
        const val COVER = "cover"
        const val DESCRIPTION = "description"
        const val FINISH_DATE = "finish_date"
        const val FIXED_POST = "fixed_post"
        const val HAS_UNSEEN_STORIES = "has_unseen_stories"
        const val IS_ADMIN = "is_admin"
        const val IS_CLOSED = "is_closed"
        const val IS_FAVORITE = "is_favorite"
        const val IS_MEMBER = "is_member"
        const val IS_SUBSCRIBED = "is_subscribed"
        const val LINKS = "links"
        const val MAIN_ALBUM_ID = "main_album_id"
        const val MEMBERS_COUNT = "members_count"
        const val MEMBER_STATUS = "member_status"
        const val MENU = "menu"
        const val NAME = "name"
        const val PHOTO_100 = "photo_100"
        const val PHOTO_200 = "photo_200"
        const val PHOTO_50 = "photo_50"
        const val SCREEN_NAME = "screen_name"
        const val SITE = "site"
        const val START_DATE = "start_date"
        const val STATUS = "status"
        const val TYPE = "type"
        const val VERIFIED = "verified"
    }

    private val LIST_FIELDS_BASE_USER =
        listOf(
            USER_FIELDS.BDATE,
            USER_FIELDS.BLACKLISTED,
            USER_FIELDS.BLACKLISTED_BY_ME,
            USER_FIELDS.CAN_ACCESS_CLOSED,
            USER_FIELDS.CAN_POST,
            USER_FIELDS.CAN_SEE_ALL_POSTS,
            USER_FIELDS.CAN_SEND_FRIEND_REQUEST,
            USER_FIELDS.CAN_WRITE_PRIVATE_MESSAGE,
            USER_FIELDS.DOMAIN,
            USER_FIELDS.FIRST_NAME,
            USER_FIELDS.FRIEND_STATUS,
            USER_FIELDS.HAS_UNSEEN_STORIES,
            USER_FIELDS.IS_CLOSED,
            USER_FIELDS.IS_FAVORITE,
            USER_FIELDS.IS_FRIEND,
            USER_FIELDS.IS_SUBSCRIBED,
            USER_FIELDS.LAST_NAME,
            USER_FIELDS.LAST_SEEN,
            USER_FIELDS.MAIDEN_NAME,
            USER_FIELDS.ONLINE,
            USER_FIELDS.ONLINE_APP,
            USER_FIELDS.ONLINE_MOBILE,
            USER_FIELDS.PHOTO_100,
            USER_FIELDS.PHOTO_200,
            USER_FIELDS.PHOTO_50,
            USER_FIELDS.PHOTO_MAX_ORIG,
            USER_FIELDS.PLATFORM,
            USER_FIELDS.SCREEN_NAME,
            USER_FIELDS.SEX,
            USER_FIELDS.STATUS,
            USER_FIELDS.VERIFIED
        )

    private val LIST_FIELDS_EXT_USER = listOf(
        USER_FIELDS.ABOUT,
        USER_FIELDS.ACTIVITIES,
        USER_FIELDS.BOOKS,
        USER_FIELDS.CAREER,
        USER_FIELDS.CITY,
        USER_FIELDS.COMMON_COUNT,
        USER_FIELDS.CONNECTIONS,
        USER_FIELDS.CONTACTS,
        USER_FIELDS.COUNTERS,
        USER_FIELDS.COUNTRY,
        USER_FIELDS.COVER,
        USER_FIELDS.EDUCATION,
        USER_FIELDS.GAMES,
        USER_FIELDS.HAS_MOBILE,
        USER_FIELDS.INTERESTS,
        USER_FIELDS.MILITARY,
        USER_FIELDS.MOVIES,
        USER_FIELDS.MUSIC,
        USER_FIELDS.OCCUPATION,
        USER_FIELDS.PERSONAL,
        USER_FIELDS.PHOTO_ID,
        USER_FIELDS.QUOTES,
        USER_FIELDS.RELATION,
        USER_FIELDS.RELATIVES,
        USER_FIELDS.SCHOOLS,
        USER_FIELDS.SITE,
        USER_FIELDS.TIMEZONE,
        USER_FIELDS.TV,
        USER_FIELDS.UNIVERSITIES
    )

    private val LIST_FIELDS_BASE_GROUP =
        listOf(
            GROUP_FIELDS.ADMIN_LEVEL,
            GROUP_FIELDS.BAN_INFO,
            GROUP_FIELDS.HAS_UNSEEN_STORIES,
            GROUP_FIELDS.IS_ADMIN,
            GROUP_FIELDS.IS_CLOSED,
            GROUP_FIELDS.IS_MEMBER,
            GROUP_FIELDS.MEMBERS_COUNT,
            GROUP_FIELDS.MEMBER_STATUS,
            GROUP_FIELDS.MENU,
            GROUP_FIELDS.NAME,
            GROUP_FIELDS.PHOTO_100,
            GROUP_FIELDS.PHOTO_200,
            GROUP_FIELDS.PHOTO_50,
            GROUP_FIELDS.SCREEN_NAME,
            GROUP_FIELDS.TYPE,
            GROUP_FIELDS.VERIFIED
        )

    private val LIST_FIELDS_EXT_GROUP = listOf(
        GROUP_FIELDS.ACTIVITY,
        GROUP_FIELDS.BLACKLISTED,
        GROUP_FIELDS.CAN_CREATE_TOPIC,
        GROUP_FIELDS.CAN_MESSAGE,
        GROUP_FIELDS.CAN_POST,
        GROUP_FIELDS.CAN_SEE_ALL_POSTS,
        GROUP_FIELDS.CAN_UPLOAD_DOC,
        GROUP_FIELDS.CAN_UPLOAD_VIDEO,
        GROUP_FIELDS.CHATS_STATUS,
        GROUP_FIELDS.CITY,
        GROUP_FIELDS.CONTACTS,
        GROUP_FIELDS.COUNTERS,
        GROUP_FIELDS.COUNTRY,
        GROUP_FIELDS.COVER,
        GROUP_FIELDS.DESCRIPTION,
        GROUP_FIELDS.FINISH_DATE,
        GROUP_FIELDS.FIXED_POST,
        GROUP_FIELDS.IS_FAVORITE,
        GROUP_FIELDS.IS_SUBSCRIBED,
        GROUP_FIELDS.LINKS,
        GROUP_FIELDS.MAIN_ALBUM_ID,
        GROUP_FIELDS.SITE,
        GROUP_FIELDS.START_DATE,
        GROUP_FIELDS.STATUS
    )

    private fun buildFields(vararg elements: List<String>): String {
        var counts = 0
        for (i in elements) {
            counts += i.size
        }
        val list = ArrayList<String>(counts)
        for (i in elements) {
            for (s in i) {
                if (!list.contains(s)) {
                    list.add(s)
                }
            }
        }
        return Utils.join(",", list)
    }

    val FIELDS_BASE_USER: String by lazy {
        buildFields(LIST_FIELDS_BASE_USER)
    }

    val FIELDS_BASE_GROUP: String by lazy {
        buildFields(LIST_FIELDS_BASE_GROUP)
    }

    val FIELDS_BASE_OWNER: String by lazy {
        buildFields(LIST_FIELDS_BASE_USER, LIST_FIELDS_BASE_GROUP)
    }

    val FIELDS_FULL_USER: String by lazy {
        buildFields(LIST_FIELDS_BASE_USER, LIST_FIELDS_EXT_USER)
    }

    val FIELDS_FULL_GROUP: String by lazy {
        buildFields(LIST_FIELDS_BASE_GROUP, LIST_FIELDS_EXT_GROUP)
    }

    val FIELDS_FULL_OWNER: String by lazy {
        buildFields(
            LIST_FIELDS_BASE_USER,
            LIST_FIELDS_EXT_USER,
            LIST_FIELDS_BASE_GROUP,
            LIST_FIELDS_EXT_GROUP
        )
    }
}