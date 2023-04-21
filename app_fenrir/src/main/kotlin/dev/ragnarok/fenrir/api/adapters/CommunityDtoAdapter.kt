package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.util.VKStringUtils
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject
import java.util.Locale
import kotlin.math.abs

class CommunityDtoAdapter : AbsDtoAdapter<VKApiCommunity>("VKApiCommunity") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiCommunity {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val root = json.jsonObject
        val dto = VKApiCommunity()
        dto.id = optLong(root, "id")
        dto.name = optString(root, Fields.GROUP_FIELDS.NAME)
        dto.screen_name = optString(
            root,
            Fields.GROUP_FIELDS.SCREEN_NAME,
            String.format(Locale.getDefault(), "club%d", abs(dto.id))
        )
        if (hasObject(root, Fields.GROUP_FIELDS.MENU)) {
            val pMenu = root[Fields.GROUP_FIELDS.MENU]?.jsonObject
            if (hasArray(pMenu, "items")) {
                dto.menu = ArrayList()
                for (i in pMenu["items"]?.jsonArray.orEmpty()) {
                    if (!checkObject(i)) {
                        continue
                    }
                    val p = i.jsonObject
                    val o = VKApiCommunity.Menu()
                    o.id = optInt(p, "id")
                    o.title = optString(p, "title")
                    o.url = optString(p, "url")
                    o.type = optString(p, "type")
                    if (hasArray(p, "cover")) {
                        var wd = 0
                        var hd = 0
                        for (s in p["cover"]?.jsonArray.orEmpty()) {
                            if (!checkObject(s)) {
                                continue
                            }
                            val f = s.jsonObject
                            if (optInt(f, "width") > wd || optInt(f, "height") > hd) {
                                wd = optInt(f, "width")
                                hd = optInt(f, "height")
                                o.cover = optString(f, "url")
                            }
                        }
                    }
                    dto.menu?.add(o)
                }
            }
        }
        dto.is_closed = optInt(root, Fields.GROUP_FIELDS.IS_CLOSED)
        dto.is_admin = optBoolean(root, Fields.GROUP_FIELDS.IS_ADMIN)
        dto.admin_level = optInt(root, Fields.GROUP_FIELDS.ADMIN_LEVEL)
        dto.is_member = optBoolean(root, Fields.GROUP_FIELDS.IS_MEMBER)
        dto.member_status = optInt(root, Fields.GROUP_FIELDS.MEMBER_STATUS)
        dto.photo_50 = optString(root, Fields.GROUP_FIELDS.PHOTO_50, VKApiCommunity.PHOTO_50)
        dto.photo_100 = optString(root, Fields.GROUP_FIELDS.PHOTO_100, VKApiCommunity.PHOTO_100)
        dto.photo_200 = optString(root, Fields.GROUP_FIELDS.PHOTO_200, null)
        when (optString(root, Fields.GROUP_FIELDS.TYPE, "group")) {
            VKApiCommunity.TYPE_GROUP -> {
                dto.type = VKApiCommunity.Type.GROUP
            }

            VKApiCommunity.TYPE_PAGE -> {
                dto.type = VKApiCommunity.Type.PAGE
            }

            VKApiCommunity.TYPE_EVENT -> {
                dto.type = VKApiCommunity.Type.EVENT
            }
        }
        if (hasObject(root, Fields.GROUP_FIELDS.CITY)) {
            dto.city = root[Fields.GROUP_FIELDS.CITY]?.let {
                kJson.decodeFromJsonElement(VKApiCity.serializer(), it)
            }
        }
        if (hasObject(root, Fields.GROUP_FIELDS.COUNTRY)) {
            dto.country =
                root[Fields.GROUP_FIELDS.COUNTRY]?.let {
                    kJson.decodeFromJsonElement(VKApiCountry.serializer(), it)
                }
        }
        if (hasObject(root, Fields.GROUP_FIELDS.BAN_INFO)) {
            val banInfo = root[Fields.GROUP_FIELDS.BAN_INFO]?.jsonObject
            dto.blacklisted = true
            dto.ban_end_date = optLong(banInfo, "end_date")
            dto.ban_comment = optString(banInfo, "comment")
        }
        dto.description = optString(root, Fields.GROUP_FIELDS.DESCRIPTION)
        dto.members_count = optInt(root, Fields.GROUP_FIELDS.MEMBERS_COUNT)
        if (hasObject(root, Fields.GROUP_FIELDS.COUNTERS)) {
            val counters = root[Fields.GROUP_FIELDS.COUNTERS]?.jsonObject
            dto.counters = counters?.let {
                kJson.decodeFromJsonElement(VKApiCommunity.Counters.serializer(), it)
            }
        }
        if (hasObject(root, Fields.GROUP_FIELDS.CHATS_STATUS)) {
            if (dto.counters == null) {
                dto.counters = VKApiCommunity.Counters()
            }
            dto.counters?.chats = optInt(
                root[Fields.GROUP_FIELDS.CHATS_STATUS]?.jsonObject,
                "count",
                VKApiCommunity.Counters.NO_COUNTER
            )
        }
        dto.start_date = optLong(root, Fields.GROUP_FIELDS.START_DATE)
        dto.finish_date = optLong(root, Fields.GROUP_FIELDS.FINISH_DATE)
        dto.can_post = optBoolean(root, Fields.GROUP_FIELDS.CAN_POST)
        dto.can_see_all_posts = optBoolean(root, Fields.GROUP_FIELDS.CAN_SEE_ALL_POSTS)
        dto.can_upload_doc = optBoolean(root, Fields.GROUP_FIELDS.CAN_UPLOAD_DOC)
        dto.can_upload_video = optBoolean(root, Fields.GROUP_FIELDS.CAN_UPLOAD_VIDEO)
        dto.can_create_topic = optBoolean(root, Fields.GROUP_FIELDS.CAN_CREATE_TOPIC)
        dto.is_favorite = optBoolean(root, Fields.GROUP_FIELDS.IS_FAVORITE)
        dto.is_subscribed = optBoolean(root, Fields.GROUP_FIELDS.IS_SUBSCRIBED)
        dto.status = VKStringUtils.unescape(optString(root, Fields.GROUP_FIELDS.STATUS))
        if (hasObject(root, "status_audio")) {
            dto.status_audio = root["status_audio"]?.let {
                kJson.decodeFromJsonElement(VKApiAudio.serializer(), it)
            }
        }
        dto.contacts = parseArray(
            root[Fields.GROUP_FIELDS.CONTACTS]?.jsonArray,
            null,
            VKApiCommunity.Contact.serializer()
        )
        dto.links = parseArray(
            root[Fields.GROUP_FIELDS.LINKS]?.jsonArray,
            null,
            VKApiCommunity.Link.serializer()
        )
        dto.fixed_post = optInt(root, Fields.GROUP_FIELDS.FIXED_POST)
        dto.main_album_id = optInt(root, Fields.GROUP_FIELDS.MAIN_ALBUM_ID)
        dto.verified = optBoolean(root, Fields.GROUP_FIELDS.VERIFIED)
        dto.site = optString(root, Fields.GROUP_FIELDS.SITE)
        dto.activity = optString(root, Fields.GROUP_FIELDS.ACTIVITY)
        dto.can_message = optBoolean(root, Fields.GROUP_FIELDS.CAN_MESSAGE)
        dto.has_unseen_stories = optBoolean(root, Fields.GROUP_FIELDS.HAS_UNSEEN_STORIES)
        if (hasObject(root, Fields.GROUP_FIELDS.COVER)) {
            dto.cover = root[Fields.GROUP_FIELDS.COVER]?.let {
                kJson.decodeFromJsonElement(VKApiCover.serializer(), it)
            }
        }
        return dto
    }

    companion object {
        private val TAG = CommunityDtoAdapter::class.java.simpleName
    }
}