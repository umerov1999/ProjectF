package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.util.VKStringUtils
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement

class UserDtoAdapter : AbsAdapter<VKApiUser>("VKApiUser") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiUser {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiUser()
        val root = json.asJsonObject
        dto.id = optInt(root, "id")
        if (dto.id == 0) dto.id = optInt(root, "user_id")
        dto.first_name = optString(root, Fields.USER_FIELDS.FIRST_NAME)
        dto.last_name = optString(root, Fields.USER_FIELDS.LAST_NAME)
        dto.online = optBoolean(root, Fields.USER_FIELDS.ONLINE)
        dto.online_mobile = optBoolean(root, Fields.USER_FIELDS.ONLINE_MOBILE)
        dto.online_app = optInt(root, Fields.USER_FIELDS.ONLINE_APP)
        dto.photo_50 = optString(root, Fields.USER_FIELDS.PHOTO_50, VKApiUser.CAMERA_50)
        dto.photo_100 = optString(root, Fields.USER_FIELDS.PHOTO_100)
        dto.photo_200 = optString(root, Fields.USER_FIELDS.PHOTO_200)
        if (hasObject(root, Fields.USER_FIELDS.LAST_SEEN)) {
            val lastSeenRoot = root.getAsJsonObject(Fields.USER_FIELDS.LAST_SEEN)
            dto.last_seen = optLong(lastSeenRoot, "time")
            dto.platform = optInt(lastSeenRoot, Fields.USER_FIELDS.PLATFORM)
        }
        dto.photo_max_orig = optString(root, Fields.USER_FIELDS.PHOTO_MAX_ORIG)
        dto.status = VKStringUtils.unescape(optString(root, Fields.USER_FIELDS.STATUS))
        dto.bdate = optString(root, Fields.USER_FIELDS.BDATE)
        if (hasObject(root, Fields.USER_FIELDS.CITY)) {
            dto.city = root[Fields.USER_FIELDS.CITY]?.let {
                kJson.decodeFromJsonElement(VKApiCity.serializer(), it)
            }
        }
        if (hasObject(root, Fields.USER_FIELDS.COUNTRY)) {
            dto.country =
                root[Fields.USER_FIELDS.COUNTRY]?.let {
                    kJson.decodeFromJsonElement(VKApiCountry.serializer(), it)
                }
        }
        dto.universities = parseArray(
            root[Fields.USER_FIELDS.UNIVERSITIES],
            null,
            VKApiUniversity.serializer()
        )
        dto.schools =
            parseArray(root[Fields.USER_FIELDS.SCHOOLS], null, VKApiSchool.serializer())
        dto.militaries =
            parseArray(root[Fields.USER_FIELDS.MILITARY], null, VKApiMilitary.serializer())
        dto.careers =
            parseArray(root[Fields.USER_FIELDS.CAREER], null, VKApiCareer.serializer())

        // status
        dto.activity = optString(root, Fields.USER_FIELDS.ACTIVITY)
        if (hasObject(root, "status_audio")) {
            dto.status_audio = root["status_audio"]?.let {
                kJson.decodeFromJsonElement(VKApiAudio.serializer(), it)
            }
        }
        if (hasObject(root, Fields.USER_FIELDS.PERSONAL)) {
            val personal = root.getAsJsonObject(Fields.USER_FIELDS.PERSONAL)
            dto.smoking = optInt(personal, "smoking")
            dto.alcohol = optInt(personal, "alcohol")
            dto.political = optInt(personal, "political")
            dto.life_main = optInt(personal, "life_main")
            dto.people_main = optInt(personal, "people_main")
            dto.inspired_by = optString(personal, "inspired_by")
            dto.religion = optString(personal, "religion")
            if (hasArray(personal, "langs")) {
                val langs = personal["langs"]?.asJsonArray
                dto.langs = Array(langs?.size.orZero()) { optString(langs, it) ?: "null" }
            }
        }

        // contacts
        dto.facebook = optString(root, "facebook")
        dto.facebook_name = optString(root, "facebook_name")
        dto.livejournal = optString(root, "livejournal")
        dto.site = optString(root, Fields.USER_FIELDS.SITE)
        dto.screen_name = optString(root, Fields.USER_FIELDS.SCREEN_NAME, "id" + dto.id)
        dto.skype = optString(root, "skype")
        dto.mobile_phone = optString(root, "mobile_phone")
        dto.home_phone = optString(root, "home_phone")
        dto.twitter = optString(root, "twitter")
        dto.instagram = optString(root, "instagram")

        // personal info
        dto.about = optString(root, Fields.USER_FIELDS.ABOUT)
        dto.activities = optString(root, Fields.USER_FIELDS.ACTIVITIES)
        dto.books = optString(root, Fields.USER_FIELDS.BOOKS)
        dto.games = optString(root, Fields.USER_FIELDS.GAMES)
        dto.interests = optString(root, Fields.USER_FIELDS.INTERESTS)
        dto.movies = optString(root, Fields.USER_FIELDS.MOVIES)
        dto.quotes = optString(root, Fields.USER_FIELDS.QUOTES)
        dto.tv = optString(root, Fields.USER_FIELDS.TV)

        // settings
        dto.domain = optString(root, Fields.USER_FIELDS.DOMAIN)
        dto.can_post = optBoolean(root, Fields.USER_FIELDS.CAN_POST)
        dto.can_see_all_posts = optBoolean(root, Fields.USER_FIELDS.CAN_SEE_ALL_POSTS)
        dto.blacklisted_by_me = optBoolean(root, Fields.USER_FIELDS.BLACKLISTED_BY_ME)
        dto.can_write_private_message =
            optBoolean(root, Fields.USER_FIELDS.CAN_WRITE_PRIVATE_MESSAGE)
        dto.wall_comments = optBoolean(root, Fields.USER_FIELDS.WALL_DEFAULT)
        val deactivated = optString(root, "deactivated")
        dto.is_deleted = "deleted" == deactivated
        dto.is_banned = "banned" == deactivated
        dto.wall_default_owner = "owner" == optString(root, Fields.USER_FIELDS.WALL_DEFAULT)
        dto.verified = optBoolean(root, Fields.USER_FIELDS.VERIFIED)
        dto.can_access_closed = optBoolean(root, Fields.USER_FIELDS.CAN_ACCESS_CLOSED)
        dto.is_closed = optBoolean(root, Fields.USER_FIELDS.IS_CLOSED)

        // other
        dto.sex = optInt(root, Fields.USER_FIELDS.SEX)
        if (hasObject(root, Fields.USER_FIELDS.COUNTERS)) {
            dto.counters =
                root[Fields.USER_FIELDS.COUNTERS]?.let {
                    kJson.decodeFromJsonElement(VKApiUser.Counters.serializer(), it)
                }
        }
        dto.relation = optInt(root, Fields.USER_FIELDS.RELATION)
        dto.relatives = parseArray(
            root[Fields.USER_FIELDS.RELATIVES], emptyList(), VKApiUser.Relative.serializer()
        )
        dto.home_town = optString(root, Fields.USER_FIELDS.HOME_TOWN)
        dto.photo_id = optString(root, Fields.USER_FIELDS.PHOTO_ID)
        dto.blacklisted = optBoolean(root, Fields.USER_FIELDS.BLACKLISTED)
        dto.has_mobile = optBoolean(root, Fields.USER_FIELDS.HAS_MOBILE)
        if (hasObject(root, Fields.USER_FIELDS.OCCUPATION)) {
            dto.occupation = root[Fields.USER_FIELDS.OCCUPATION]?.let {
                kJson.decodeFromJsonElement(VKApiUser.Occupation.serializer(), it)
            }
        }
        if (hasObject(root, "relation_partner")) {
            dto.relation_partner =
                root["relation_partner"]?.let { deserialize(it) }
        }
        dto.music = optString(root, Fields.USER_FIELDS.MUSIC)
        dto.can_send_friend_request = optBoolean(root, Fields.USER_FIELDS.CAN_SEND_FRIEND_REQUEST)
        dto.is_favorite = optBoolean(root, Fields.USER_FIELDS.IS_FAVORITE)
        dto.is_subscribed = optBoolean(root, Fields.USER_FIELDS.IS_SUBSCRIBED)
        dto.timezone = optInt(root, Fields.USER_FIELDS.TIMEZONE)
        dto.maiden_name = optString(root, Fields.USER_FIELDS.MAIDEN_NAME)
        dto.is_friend = optBoolean(root, Fields.USER_FIELDS.IS_FRIEND)
        dto.friend_status = optInt(root, Fields.USER_FIELDS.FRIEND_STATUS)
        dto.role = optString(root, "role")
        dto.has_unseen_stories = optBoolean(root, Fields.USER_FIELDS.HAS_UNSEEN_STORIES)
        if (hasObject(root, Fields.USER_FIELDS.COVER)) {
            dto.cover = root[Fields.USER_FIELDS.COVER]?.let {
                kJson.decodeFromJsonElement(VKApiCover.serializer(), it)
            }
        }
        return dto
    }

    companion object {
        private val TAG = UserDtoAdapter::class.java.simpleName
    }
}