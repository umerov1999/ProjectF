package dev.ragnarok.fenrir.api.adapters

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
        dto.first_name = optString(root, "first_name")
        dto.last_name = optString(root, "last_name")
        dto.online = optBoolean(root, VKApiUser.Field.ONLINE)
        dto.online_mobile = optBoolean(root, VKApiUser.Field.ONLINE_MOBILE)
        dto.online_app = optInt(root, "online_app")
        dto.photo_50 = optString(root, VKApiUser.Field.PHOTO_50, VKApiUser.CAMERA_50)
        dto.photo_100 = optString(root, VKApiUser.Field.PHOTO_100)
        dto.photo_200 = optString(root, VKApiUser.Field.PHOTO_200)
        if (hasObject(root, VKApiUser.Field.LAST_SEEN)) {
            val lastSeenRoot = root.getAsJsonObject(VKApiUser.Field.LAST_SEEN)
            dto.last_seen = optLong(lastSeenRoot, "time")
            dto.platform = optInt(lastSeenRoot, "platform")
        }
        dto.photo_max_orig = optString(root, VKApiUser.Field.PHOTO_MAX_ORIG)
        dto.status = VKStringUtils.unescape(optString(root, VKApiUser.Field.STATUS))
        dto.bdate = optString(root, VKApiUser.Field.BDATE)
        if (hasObject(root, VKApiUser.Field.CITY)) {
            dto.city = root[VKApiUser.Field.CITY]?.let {
                kJson.decodeFromJsonElement(VKApiCity.serializer(), it)
            }
        }
        if (hasObject(root, VKApiUser.Field.COUNTRY)) {
            dto.country =
                root[VKApiUser.Field.COUNTRY]?.let {
                    kJson.decodeFromJsonElement(VKApiCountry.serializer(), it)
                }
        }
        dto.universities = parseArray(
            root[VKApiUser.Field.UNIVERSITIES],
            null,
            VKApiUniversity.serializer()
        )
        dto.schools =
            parseArray(root[VKApiUser.Field.SCHOOLS], null, VKApiSchool.serializer())
        dto.militaries =
            parseArray(root[VKApiUser.Field.MILITARY], null, VKApiMilitary.serializer())
        dto.careers =
            parseArray(root[VKApiUser.Field.CAREER], null, VKApiCareer.serializer())

        // status
        dto.activity = optString(root, VKApiUser.Field.ACTIVITY)
        if (hasObject(root, "status_audio")) {
            dto.status_audio = root["status_audio"]?.let {
                kJson.decodeFromJsonElement(VKApiAudio.serializer(), it)
            }
        }
        if (hasObject(root, VKApiUser.Field.PERSONAL)) {
            val personal = root.getAsJsonObject(VKApiUser.Field.PERSONAL)
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
        dto.site = optString(root, VKApiUser.Field.SITE)
        dto.screen_name = optString(root, "screen_name", "id" + dto.id)
        dto.skype = optString(root, "skype")
        dto.mobile_phone = optString(root, "mobile_phone")
        dto.home_phone = optString(root, "home_phone")
        dto.twitter = optString(root, "twitter")
        dto.instagram = optString(root, "instagram")

        // personal info
        dto.about = optString(root, VKApiUser.Field.ABOUT)
        dto.activities = optString(root, VKApiUser.Field.ACTIVITIES)
        dto.books = optString(root, VKApiUser.Field.BOOKS)
        dto.games = optString(root, VKApiUser.Field.GAMES)
        dto.interests = optString(root, VKApiUser.Field.INTERESTS)
        dto.movies = optString(root, VKApiUser.Field.MOVIES)
        dto.quotes = optString(root, VKApiUser.Field.QUOTES)
        dto.tv = optString(root, VKApiUser.Field.TV)

        // settings
        dto.nickname = optString(root, "nickname")
        dto.domain = optString(root, "domain")
        dto.can_post = optBoolean(root, VKApiUser.Field.CAN_POST)
        dto.can_see_all_posts = optBoolean(root, VKApiUser.Field.CAN_SEE_ALL_POSTS)
        dto.blacklisted_by_me = optBoolean(root, VKApiUser.Field.BLACKLISTED_BY_ME)
        dto.can_write_private_message = optBoolean(root, VKApiUser.Field.CAN_WRITE_PRIVATE_MESSAGE)
        dto.wall_comments = optBoolean(root, VKApiUser.Field.WALL_DEFAULT)
        val deactivated = optString(root, "deactivated")
        dto.is_deleted = "deleted" == deactivated
        dto.is_banned = "banned" == deactivated
        dto.wall_default_owner = "owner" == optString(root, VKApiUser.Field.WALL_DEFAULT)
        dto.verified = optBoolean(root, VKApiUser.Field.VERIFIED)
        dto.can_access_closed = optBoolean(root, "can_access_closed")
        dto.is_closed = optBoolean(root, "is_closed")

        // other
        dto.sex = optInt(root, VKApiUser.Field.SEX)
        if (hasObject(root, VKApiUser.Field.COUNTERS)) {
            dto.counters =
                root[VKApiUser.Field.COUNTERS]?.let {
                    kJson.decodeFromJsonElement(VKApiUser.Counters.serializer(), it)
                }
        }
        dto.relation = optInt(root, VKApiUser.Field.RELATION)
        dto.relatives = parseArray(
            root[VKApiUser.Field.RELATIVES], emptyList(), VKApiUser.Relative.serializer()
        )
        dto.home_town = optString(root, VKApiUser.Field.HOME_TOWN)
        dto.photo_id = optString(root, "photo_id")
        dto.blacklisted = optBoolean(root, "blacklisted")
        dto.photo_200_orig = optString(root, "photo_200_orig")
        dto.photo_400_orig = optString(root, "photo_400_orig")
        dto.photo_max = optString(root, "photo_max")
        dto.has_mobile = optBoolean(root, "has_mobile")
        if (hasObject(root, "occupation")) {
            dto.occupation = root["occupation"]?.let {
                kJson.decodeFromJsonElement(VKApiUser.Occupation.serializer(), it)
            }
        }
        if (hasObject(root, "relation_partner")) {
            dto.relation_partner =
                root["relation_partner"]?.let { deserialize(it) }
        }
        dto.music = optString(root, "music")
        dto.can_see_audio = optBoolean(root, "can_see_audio")
        dto.can_send_friend_request = optBoolean(root, "can_send_friend_request")
        dto.is_favorite = optBoolean(root, "is_favorite")
        dto.is_subscribed = optBoolean(root, "is_subscribed")
        dto.timezone = optInt(root, "timezone")
        dto.maiden_name = optString(root, "maiden_name")
        dto.is_friend = optBoolean(root, "is_friend")
        dto.friend_status = optInt(root, "friend_status")
        dto.role = optString(root, "role")
        return dto
    }

    companion object {
        private val TAG = UserDtoAdapter::class.java.simpleName
    }
}