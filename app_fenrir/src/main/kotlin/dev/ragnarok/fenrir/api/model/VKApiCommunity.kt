package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.CommunityDtoAdapter
import dev.ragnarok.fenrir.nonNullNoEmpty
import kotlinx.serialization.Serializable

/**
 * Community object describes a community.
 */
@Serializable(with = CommunityDtoAdapter::class)
class VKApiCommunity
/**
 * Creates empty Community instance.
 */
    : VKApiOwner() {
    /**
     * Community name
     */
    var name: String? = null

    override val fullName: String?
        get() = name

    /**
     * Screen name of the community page (e.g. apiclub or club1).
     */
    var screen_name: String? = null

    var menu: ArrayList<Menu>? = null

    /**
     * Whether the community is closed
     */
    var is_closed = 0

    /**
     * Whether a user is the community manager
     */
    var is_admin = false

    /**
     * Rights of the user
     */
    var admin_level = 0

    /**
     * Whether a user is a community member
     */
    var is_member = false

    /**
     * статус участника текущего пользователя в сообществе:
     * 0 — не является участником;
     * 1 — является участником;
     * 2 — не уверен; возможно not_sure опциональный параметр, учитываемый, если group_id принадлежит встрече. 1 — Возможно пойду. 0 — Точно пойду. По умолчанию 0.
     * 3 — отклонил приглашение;
     * 4 — подал заявку на вступление;
     * 5 — приглашен.
     */
    var member_status = 0

    /**
     * City specified in information about community.
     */
    var city: VKApiCity? = null

    /**
     * Country specified in information about community.
     */
    var country: VKApiCountry? = null

    /**
     * Audio which broadcasting to status.
     */
    var status_audio: VKApiAudio? = null

    /**
     * срок окончания блокировки в формате unixtime;
     */
    var ban_end_date: Long = 0

    /**
     * комментарий к блокировке.
     */
    var ban_comment: String? = null

    /**
     * Community description text.
     */
    var description: String? = null

    /**
     * Number of community members.
     */
    var members_count = 0

    /**
     * Counters object with community counters.
     */
    var counters: Counters? = null

    /**
     * Returned only for meeting and contain start time of the meeting as unixtime.
     */
    var start_date: Long = 0

    /**
     * Returned only for meeting and contain end time of the meeting as unixtime.
     */
    var finish_date: Long = 0

    /**
     * Whether the current user can post on the community's wall
     */
    var can_post = false

    /**
     * Whether others' posts on the community's wall can be viewed
     */
    var can_see_all_posts = false

    /**
     * Group status.
     */
    var status: String? = null

    /**
     * Information from public page contact module.
     */
    var contacts: List<Contact>? = null

    /**
     * Information from public page links module.
     */
    var links: List<Link>? = null

    /**
     * ID of canDelete post of this community.
     */
    var fixed_post = 0

    /**
     * идентификатор основного альбома сообщества.
     */
    var main_album_id = 0

    /**
     * Information whether the community has a verified page in VK
     */
    var verified = false

    /**
     * информация о том, может ли текущий пользователь загружать документы в группу.
     */
    var can_upload_doc = false

    /**
     * информация о том, может ли текущий пользователь загружать видеозаписи в группу
     */
    var can_upload_video = false

    /**
     * информация о том, может ли текущий пользователь создать тему обсуждения в группе, используя метод board.addTopic
     */
    var can_create_topic = false

    /**
     * возвращается 1, если сообщество находится в закладках у текущего пользователя.
     */
    var is_favorite = false
    var is_subscribed = false

    /**
     * URL of community site
     */
    var site: String? = null

    /**
     * строка состояния публичной страницы. У групп возвращается строковое значение, открыта ли группа или нет, а у событий дата начала.
     */
    var activity: String? = null

    /**
     * Information whether the current community has add current user to the blacklist.
     */
    var blacklisted = false

    /**
     * информация о том, может ли текущий пользователь написать сообщение сообществу. Возможные значения:
     */
    var can_message = false
    var has_unseen_stories = false
    var cover: VKApiCover? = null

    /**
     * Community type
     */
    var type = 0

    /**
     * URL of the 50px-wide community logo.
     */
    var photo_50: String? = null

    /**
     * URL of the 100px-wide community logo.
     */
    var photo_100: String? = null

    /**
     * URL of the 200px-wide community logo.
     */
    var photo_200: String? = null
    override val maxSquareAvatar: String?
        get() {
            if (photo_200.nonNullNoEmpty()) {
                return photo_200
            }
            return if (photo_100.nonNullNoEmpty()) {
                photo_100
            } else photo_50
        }

    override fun toString(): String {
        return "id: $id, name: '$fullName'"
    }

    /**
     * VKApiPrivacy status of the group.
     */
    object MemberStatus {
        const val IS_NOT_MEMBER = 0
        const val IS_MEMBER = 1
        const val NOT_SURE = 2
        const val DECLINED_INVITATION = 3
        const val SENT_REQUEST = 4
        const val INVITED = 5
    }

    /**
     * Access level to manage community.
     */
    object AdminLevel {
        const val MODERATOR = 1
        const val EDITOR = 2
        const val ADMIN = 3
    }

    /**
     * VKApiPrivacy status of the group.
     */
    object Status {
        const val OPEN = 0
        const val CLOSED = 1
        const val PRIVATE = 2
    }

    /**
     * Types of communities.
     */
    object Type {
        const val GROUP = 0
        const val PAGE = 1
        const val EVENT = 2
    }

    @Serializable
    class Counters {
        var photos = NO_COUNTER
        var albums = NO_COUNTER
        var audios = NO_COUNTER
        var videos = NO_COUNTER
        var topics = NO_COUNTER
        var donuts = NO_COUNTER
        var docs = NO_COUNTER
        var articles = NO_COUNTER
        var market = NO_COUNTER
        var market_services = NO_COUNTER
        var narratives = NO_COUNTER
        var chats = NO_COUNTER
        var all_wall = NO_COUNTER
        var owner_wall = NO_COUNTER
        var suggest_wall = NO_COUNTER
        var postponed_wall = NO_COUNTER

        companion object {
            /**
             * Значение в том случае, если счетчик не был явно указан.
             */
            const val NO_COUNTER = -1
        }
    }

    @Serializable
    class Contact : Identificable {
        var user_id = 0
        var email: String? = null
        var phone: String? = null
        var desc: String? = null
        override fun getObjectId(): Int {
            return user_id
        }
    }

    @Serializable
    class Link {
        var id = 0
        var url: String? = null
        var name: String? = null
        var desc: String? = null
        var photo_50: String? = null
        var photo_100: String? = null
    }

    class Menu {
        var id = 0
        var url: String? = null
        var title: String? = null
        var type: String? = null
        var cover: String? = null
    }

    companion object {
        const val TYPE_GROUP = "group"
        const val TYPE_PAGE = "page"
        const val TYPE_EVENT = "event"
        const val PHOTO_50 = "http://vk.com/images/community_50.gif"
        const val PHOTO_100 = "http://vk.com/images/community_100.gif"

        fun create(id: Int): VKApiCommunity {
            val community = VKApiCommunity()
            community.id = id
            return community
        }
    }
}