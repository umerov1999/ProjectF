package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.UserDtoAdapter
import dev.ragnarok.fenrir.nonNullNoEmpty
import kotlinx.serialization.Serializable

/**
 * User object describes a user profile.
 */
@Serializable(with = UserDtoAdapter::class)
class VKApiUser
/**
 * Creates empty User instance.
 */
    () : VKApiOwner() {
    /**
     * First name of user.
     */
    var first_name: String? = null

    /**
     * Last name of user.
     */
    var last_name: String? = null

    /**
     * Last visit date(in Unix time).
     */
    var last_seen: Long = 0

    /**
     * Last visit paltform(in Unix time).
     * 1    mobile	Мобильная версия сайта или неопознанное мобильное приложение
     * 2	iphone	Официальное приложение для iPhone
     * 3	ipad	Официальное приложение для iPad
     * 4	android	Официальное приложение для Android
     * 5	wphone	Официальное приложение для Windows Phone
     * 6	windows	Официальное приложение для Windows 8
     * 7	web	    Полная версия сайта или неопознанное приложение
     */
    var platform = 0

    /**
     * Information whether the user is online.
     */
    var online = false

    /**
     * If user utilizes a mobile application or site mobile version, it returns online_mobile as additional.
     */
    var online_mobile = false
    var online_app = 0

    /**
     * URL of default square photo of the user with 50 pixels in width.
     */
    var photo_50: String? = null

    /**
     * URL of default square photo of the user with 100 pixels in width.
     */
    var photo_100: String? = null

    /**
     * URL of default square photo of the user with 200 pixels in width.
     */
    var photo_200: String? = null

    /**
     * URL of default square photo of the user with max pixels in width.
     */
    var photo_max_orig: String? = null

    /**
     * статус пользователя. Возвращается строка,
     * содержащая текст статуса, расположенного в профиле под именем пользователя.
     * Если у пользователя включена опция «Транслировать в статус играющую музыку»,
     * будет возвращено дополнительное поле status_audio,
     * содержащее информацию о транслируемой композиции.
     */
    var status: String? = null

    /**
     * Text of user status.
     */
    var activity: String? = null

    /**
     * Audio which broadcasting to status.
     */
    var status_audio: VKApiAudio? = null

    /**
     * User's date of birth.  Returned as DD.MM.YYYY or DD.MM (if birth year is hidden).
     */
    var bdate: String? = null

    /**
     * City specified on user's page in "Contacts" section.
     */
    var city: VKApiCity? = null

    /**
     * Country specified on user's page in "Contacts" section.
     */
    var country: VKApiCountry? = null

    /**
     * List of user's universities
     */
    var universities: List<VKApiUniversity>? = null

    /**
     * List of user's schools
     */
    var schools: List<VKApiSchool>? = null

    /**
     * List of user's schools
     */
    var militaries: List<VKApiMilitary>? = null

    /**
     * List of user's schools
     */
    var careers: List<VKApiCareer>? = null

    /**
     * Views on smoking.
     */
    var smoking = 0

    /**
     * Views on alcohol.
     */
    var alcohol = 0

    /**
     * Views on policy.
     */
    var political = 0

    /**
     * Life main stuffs.
     */
    var life_main = 0

    /**
     * People main stuffs.
     */
    var people_main = 0

    /**
     * Stuffs that inspire the user.
     */
    var inspired_by: String? = null

    /**
     * List of user's languages
     */
    var langs: Array<String>? = null

    /**
     * Religion of user
     */
    var religion: String? = null

    /**
     * Name of user's account in Facebook
     */
    var facebook: String? = null

    /**
     * ID of user's facebook
     */
    var facebook_name: String? = null

    /**
     * Name of user's account in LiveJournal
     */
    var livejournal: String? = null

    /**
     * Name of user's account in Skype
     */
    var skype: String? = null

    /**
     * URL of user's site
     */
    var site: String? = null

    /**
     * Name of user's account in Twitter
     */
    var twitter: String? = null

    /**
     * Name of user's account in Instagram
     */
    var instagram: String? = null

    /**
     * User's mobile phone number
     */
    var mobile_phone: String? = null

    /**
     * User's home phone number
     */
    var home_phone: String? = null

    /**
     * Page screen name.
     */
    var screen_name: String? = null

    /**
     * Nickname of user.
     */
    var nickname: String? = null

    /**
     * User's activities
     */
    var activities: String? = null

    /**
     * User's interests
     */
    var interests: String? = null

    /**
     * User's favorite movies
     */
    var movies: String? = null

    /**
     * User's favorite TV Shows
     */
    var tv: String? = null

    /**
     * User's favorite books
     */
    var books: String? = null

    /**
     * User's favorite games
     */
    var games: String? = null

    /**
     * User's about information
     */
    var about: String? = null

    /**
     * User's favorite quotes
     */
    var quotes: String? = null

    /**
     * Information whether others can posts on user's wall.
     */
    var can_post = false

    /**
     * Information whether others' posts on user's wall can be viewed
     */
    var can_see_all_posts = false

    /**
     * Information whether private messages can be sent to this user.
     */
    var can_write_private_message = false
    var can_access_closed = false
    var is_closed = false

    /**
     * Information whether user can comment wall posts.
     */
    var wall_comments = false

    /**
     * Information whether the user is banned in VK.
     */
    var is_banned = false

    /**
     * Information whether the user is deleted in VK.
     */
    var is_deleted = false

    /**
     * Information whether the user's post of wall shows by default.
     */
    var wall_default_owner = false

    /**
     * Information whether the user has a verified page in VK
     */
    var verified = false

    /**
     * User sex.
     */
    var sex = 0

    /**
     * Set of user's counters.
     */
    var counters: Counters? = null

    /**
     * Relationship status.
     */
    var relation = 0

    /**
     * List of user's relatives
     */
    var relatives: List<Relative>? = null

    /**
     * Information whether the current user has add this user to the blacklist.
     */
    var blacklisted_by_me = false

    /**
     * короткий адрес страницы.
     * Возвращается строка, содержащая короткий адрес страницы
     * (возвращается только сам поддомен, например, andrew).
     * Если он не назначен, возвращается "id"+uid, например, id35828305.
     */
    var domain: String? = null
    var home_town: String? = null

    /**
     * id главной фотографии профиля пользователя в формате user_id+photo_id,
     * например, 6492_192164258. В некоторых случаях (если фотография была установлена очень давно)
     * это поле не возвращается.
     */
    var photo_id: String? = null

    /**
     * возвращается 1, если текущий пользователь находится в черном списке у запрашиваемого.
     */
    var blacklisted = false

    /**
     * url фотографии пользователя, имеющей ширину 200 пикселей.
     * В случае отсутствия у пользователя фотографии возвращается http://vk.com/images/camera_a.gif.
     */
    var photo_200_orig: String? = null

    /**
     * url фотографии пользователя, имеющей ширину 400 пикселей.
     * Если у пользователя отсутствует фотография такого размера, ответ не будет содержать этого поля.
     */
    var photo_400_orig: String? = null

    /**
     * url квадратной фотографии пользователя с максимальной шириной.
     * Может быть возвращена фотография, имеющая ширину как 200, так и 100 пикселей.
     * В случае отсутствия у пользователя фотографии возвращается http://vk.com/images/camera_b.gif.
     */
    var photo_max: String? = null

    /**
     * информация о том, известен ли номер мобильного телефона пользователя.
     * Возвращаемые значения: 1 — известен, 0 — не известен.
     * Рекомендуется использовать перед вызовом метода secure.sendSMSNotification.
     */
    var has_mobile = false

    /**
     * информация о текущем роде занятия пользователя. Возвращаются
     */
    var occupation: Occupation? = null

    /**
     * Если в семейном положении указан другой пользователь,
     * дополнительно возвращается объект relation_partner, содержащий id и имя этого человека.
     */
    var relation_partner: VKApiUser? = null

    /**
     * любимая музыка.
     */
    var music: String? = null

    /**
     * информация о том, разрешено ли видеть чужие аудиозаписи на стене пользователя.
     * Возвращаемые значения: 1 —разрешено, 0 — не разрешено.
     */
    var can_see_audio = false

    /**
     * информация о том, будет ли отправлено уведомление пользователю о заявке в друзья.
     * Возвращаемые значения: 1 — уведомление будет отправлено, 0 — уведомление не будет оптравлено.
     */
    var can_send_friend_request = false

    /**
     * возвращается 1, если пользователь находится в закладках у текущего пользователя.
     */
    var is_favorite = false
    var is_subscribed = false

    /**
     * временная зона пользователя. Возвращается только при запросе информации о текущем пользователе.
     */
    var timezone = 0

    /**
     * девичья фамилия.
     */
    var maiden_name: String? = null

    /**
     * 1 – пользователь друг, 2 – пользователь не в друзьях.
     */
    var is_friend = false

    /**
     * статус дружбы с пользователем:
     * 0 – пользователь не является другом,
     * 1 – отправлена заявка/подписка пользователю,
     * 2 – имеется входящая заявка/подписка от пользователя,
     * 3 – пользователь является другом;
     */
    var friend_status = 0

    /**
     * Содержит уровень полномочий руководителя сообщества
     */
    var role: String? = null

    constructor(id: Int) : this() {
        this.id = id
    }

    override val maxSquareAvatar: String?
        get() = if (photo_200.nonNullNoEmpty()) {
            photo_200
        } else if (photo_100.nonNullNoEmpty()) {
            photo_100
        } else {
            photo_50
        }
    override val fullName: String
        get() = if (first_name.isNullOrEmpty() && last_name.isNullOrEmpty()) "[id $id]" else "$first_name $last_name"

    object Field {
        /**
         * Field name for [.online] param.
         */
        const val ONLINE = "online"

        /**
         * Field name for [.online_mobile] param.
         */
        const val ONLINE_MOBILE = "online_mobile"

        /**
         * Field name for [.photo_50] param.
         */
        const val PHOTO_50 = "photo_50"

        /**
         * Field name for [.photo_100] param.
         */
        const val PHOTO_100 = "photo_100"

        /**
         * Field name for [.photo_200] param.
         */
        const val PHOTO_200 = "photo_200"

        /**
         * Filed last_seen from VK fields set
         */
        const val LAST_SEEN = "last_seen"

        /**
         * Filed photo_max_orig
         */
        const val PHOTO_MAX_ORIG = "photo_max_orig"

        /**
         * Filed photo_max_orig
         */
        const val STATUS = "status"

        /**
         * Filed bdate from VK fields set
         */
        const val BDATE = "bdate"

        /**
         * Filed city from VK fields set
         */
        const val CITY = "city"

        /**
         * Filed country from VK fields set
         */
        const val COUNTRY = "country"

        /**
         * Filed universities from VK fields set
         */
        const val UNIVERSITIES = "universities"

        /**
         * Filed schools from VK fields set
         */
        const val SCHOOLS = "schools"

        /**
         * Filed military from VK fields set
         */
        const val MILITARY = "military"

        /**
         * Filed military from VK fields set
         */
        const val CAREER = "career"

        /**
         * Filed activity from VK fields set
         */
        const val ACTIVITY = "activity"

        /**
         * Filed personal from VK fields set
         */
        const val PERSONAL = "personal"

        /**
         * Filed sex from VK fields set
         */
        const val SEX = "sex"

        /**
         * Filed site from VK fields set
         */
        const val SITE = "site"

        /**
         * Filed contacts from VK fields set
         */
        const val CONTACTS = "contacts"

        /**
         * Filed can_post from VK fields set
         */
        const val CAN_POST = "can_post"

        /**
         * Filed can_see_all_posts from VK fields set
         */
        const val CAN_SEE_ALL_POSTS = "can_see_all_posts"

        /**
         * Filed can_write_private_message from VK fields set
         */
        const val CAN_WRITE_PRIVATE_MESSAGE = "can_write_private_message"

        /**
         * Filed relation from VK fields set
         */
        const val RELATION = "relation"

        /**
         * Filed counters from VK fields set
         */
        const val COUNTERS = "counters"

        /**
         * Filed activities from VK fields set
         */
        const val ACTIVITIES = "activities"

        /**
         * Filed interests from VK fields set
         */
        const val INTERESTS = "interests"

        /**
         * Filed movies from VK fields set
         */
        const val MOVIES = "movies"

        /**
         * Filed tv from VK fields set
         */
        const val TV = "tv"

        /**
         * Filed books from VK fields set
         */
        const val BOOKS = "books"

        /**
         * Filed games from VK fields set
         */
        const val GAMES = "games"

        /**
         * Filed about from VK fields set
         */
        const val ABOUT = "about"

        /**
         * Filed quotes from VK fields set
         */
        const val QUOTES = "quotes"

        /**
         * Filed connections from VK fields set
         */
        const val CONNECTIONS = "connections"

        /**
         * Filed relatives from VK fields set
         */
        const val RELATIVES = "relatives"

        /**
         * Filed wall_default from VK fields set
         */
        const val WALL_DEFAULT = "wall_default"

        /**
         * Filed verified from VK fields set
         */
        const val VERIFIED = "verified"

        /**
         * Filed screen_name from VK fields set
         */
        const val SCREEN_NAME = "screen_name"

        /**
         * Filed blacklisted_by_me from VK fields set
         */
        const val BLACKLISTED_BY_ME = "blacklisted_by_me"

        /**
         * Filed blacklisted_by_me from VK fields set
         */
        const val DOMAIN = "domain"

        /**
         * Filed blacklisted_by_me from VK fields set
         */
        const val HOME_TOWN = "home_town"
    }

    object Platform {
        const val MOBILE = 1
        const val IPHONE = 2
        const val IPAD = 3
        const val ANDROID = 4
        const val WPHONE = 5
        const val WINDOWS = 6
        const val WEB = 7
    }

    @Serializable
    class Occupation {
        /**
         * может принимать значения work, school, university
         */
        var type: String? = null

        /**
         * идентификатор школы, вуза, группы компании (в которой пользователь работает);
         */
        var id = 0

        /**
         * название школы, вуза или места работы;
         */
        var name: String? = null
    }

    @Serializable
    class Counters {
        var albums = NO_COUNTER
        var videos = NO_COUNTER
        var audios = NO_COUNTER
        var notes = NO_COUNTER
        var friends = NO_COUNTER
        var photos = NO_COUNTER
        var groups = NO_COUNTER
        var online_friends = NO_COUNTER
        var mutual_friends = NO_COUNTER
        var followers = NO_COUNTER
        var subscriptions = NO_COUNTER
        var pages = NO_COUNTER
        var all_wall = NO_COUNTER
        var owner_wall = NO_COUNTER
        var postponed_wall = NO_COUNTER
        var articles = NO_COUNTER
        var market = NO_COUNTER
        var market_services = NO_COUNTER
        var narratives = NO_COUNTER
        var gifts = NO_COUNTER

        companion object {
            /**
             * Count was not in server response.
             */
            const val NO_COUNTER = -1
        }
    }

    object Relation {
        const val SINGLE = 1
        const val RELATIONSHIP = 2
        const val ENGAGED = 3
        const val MARRIED = 4
        const val COMPLICATED = 5
        const val SEARCHING = 6
        const val IN_LOVE = 7
        const val IN_A_CIVIL_UNION = 8
    }

    object RelativeType {
        const val PARTNER = "partner"
        const val GRANDCHILD = "grandchild"
        const val GRANDPARENT = "grandparent"
        const val CHILD = "child"
        const val SUBLING = "sibling"
        const val PARENT = "parent"
    }

    @Serializable
    class Relative {
        var type: String? = null
        var id = 0
        var name: String? = null
    }

    companion object {
        const val SEX_MAN = 2
        const val SEX_WOMAN = 1
        const val CAMERA_50 = "http://vk.com/images/camera_c.gif"
        const val FRIEND_STATUS_IS_NOT_FRIEDND = 0
        const val FRIEND_STATUS_REQUEST_SENT = 1
        const val FRIEND_STATUS_HAS_INPUT_REQUEST = 2
        const val FRIEND_STATUS_IS_FRIEDND = 3
        const val ALL_FIELDS =
            "about,activities,bdate,blacklisted,blacklisted_by_me,books,can_access_closed,can_post,can_see_all_posts,can_see_audio,can_write_private_message,career,city,common_count,connections,contacts,counters,country,domain,first_name,education,friend_status,games,has_mobile,interests,is_closed,is_favorite,is_friend,is_subscribed,last_name,last_seen,maiden_name,military,movies,music,occupation,online,online_app,online_mobile,personal,photo_100,photo_200,photo_200_orig,photo_400_orig,photo_50,photo_id,photo_max,photo_max_orig,quotes,relation,relatives,schools,screen_name,sex,site,status,timezone,tv,universities,verified"

        fun create(id: Int): VKApiUser {
            val user = VKApiUser()
            user.id = id
            return user
        }
    }
}