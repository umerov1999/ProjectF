package dev.ragnarok.fenrir.place

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.result.ActivityResultLauncher

open class Place : Parcelable {
    val type: Int
    var isNeedFinishMain = false
        private set
    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    private var args: Bundle? = null

    constructor(type: Int) {
        this.type = type
    }

    protected constructor(p: Parcel) {
        type = p.readInt()
        args = p.readBundle(javaClass.classLoader)
    }

    fun tryOpenWith(context: Context) {
        if (context is PlaceProvider) {
            (context as PlaceProvider).openPlace(this)
        }
    }

    fun setActivityResultLauncher(activityResultLauncher: ActivityResultLauncher<Intent>): Place {
        this.activityResultLauncher = activityResultLauncher
        return this
    }

    fun setNeedFinishMain(needFinishMain: Boolean): Place {
        isNeedFinishMain = needFinishMain
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(type)
        dest.writeBundle(args)
    }

    fun setArguments(arguments: Bundle?): Place {
        args = arguments
        return this
    }

    fun withStringExtra(name: String, value: String?): Place {
        prepareArguments().putString(name, value)
        return this
    }

    fun withParcelableExtra(name: String, parcelableExtra: Parcelable?): Place {
        prepareArguments().putParcelable(name, parcelableExtra)
        return this
    }

    fun withIntExtra(name: String, value: Int): Place {
        prepareArguments().putInt(name, value)
        return this
    }

    fun withBoolExtra(name: String, value: Boolean): Place {
        prepareArguments().putBoolean(name, value)
        return this
    }

    fun withLongExtra(name: String, value: Long): Place {
        prepareArguments().putLong(name, value)
        return this
    }

    fun prepareArguments(): Bundle {
        if (args == null) {
            args = Bundle()
        }
        return args!!
    }

    fun safeArguments(): Bundle {
        return args ?: Bundle()
    }

    fun launchActivityForResult(context: Activity, intent: Intent) {
        if (activityResultLauncher != null && !isNeedFinishMain) {
            activityResultLauncher?.launch(intent)
        } else {
            context.startActivity(intent)
            if (isNeedFinishMain) {
                context.finish()
                context.overridePendingTransition(0, 0)
            }
        }
    }

    companion object {
        const val VIDEO_PREVIEW = 1
        const val FRIENDS_AND_FOLLOWERS = 2
        const val EXTERNAL_LINK = 3
        const val DOC_PREVIEW = 4
        const val WALL_POST = 5
        const val COMMENTS = 6
        const val WALL = 7
        const val CONVERSATION_ATTACHMENTS = 8
        const val PLAYER = 9
        const val SEARCH = 10
        const val CHAT = 11
        const val BUILD_NEW_POST = 12
        const val EDIT_COMMENT = 13
        const val EDIT_POST = 14
        const val REPOST = 15
        const val DIALOGS = 16
        const val FORWARD_MESSAGES = 17
        const val TOPICS = 18
        const val CHAT_MEMBERS = 19
        const val COMMUNITIES = 20
        const val LIKES_AND_COPIES = 21
        const val VIDEO_ALBUM = 22
        const val AUDIOS = 23
        const val VIDEOS = 24
        const val VK_PHOTO_ALBUMS = 25
        const val VK_PHOTO_ALBUM = 26
        const val VK_PHOTO_ALBUM_GALLERY = 27
        const val VK_PHOTO_ALBUM_GALLERY_SAVED = 28
        const val FAVE_PHOTOS_GALLERY = 29
        const val SIMPLE_PHOTO_GALLERY = 30
        const val POLL = 31
        const val PREFERENCES = 32
        const val DOCS = 33
        const val FEED = 34
        const val NOTIFICATIONS = 35
        const val BOOKMARKS = 36
        const val RESOLVE_DOMAIN = 37
        const val VK_INTERNAL_PLAYER = 38
        const val NOTIFICATION_SETTINGS = 39
        const val CREATE_PHOTO_ALBUM = 40
        const val EDIT_PHOTO_ALBUM = 41
        const val MESSAGE_LOOKUP = 42
        const val GIF_PAGER = 43
        const val SECURITY = 44
        const val CREATE_POLL = 45
        const val COMMENT_CREATE = 46
        const val LOGS = 47
        const val LOCAL_IMAGE_ALBUM = 48
        const val SINGLE_SEARCH = 49
        const val NEWSFEED_COMMENTS = 50
        const val COMMUNITY_CONTROL = 51
        const val COMMUNITY_BAN_EDIT = 52
        const val COMMUNITY_ADD_BAN = 53
        const val VK_PHOTO_TMP_SOURCE = 54
        const val COMMUNITY_MANAGER_EDIT = 55
        const val COMMUNITY_MANAGER_ADD = 56
        const val REQUEST_EXECUTOR = 57
        const val USER_BLACKLIST = 58
        const val PROXY_ADD = 59
        const val DRAWER_EDIT = 60
        const val SIDE_DRAWER_EDIT = 61
        const val USER_DETAILS = 62
        const val AUDIOS_IN_ALBUM = 63
        const val COMMUNITY_INFO = 64
        const val COMMUNITY_INFO_LINKS = 65
        const val SETTINGS_THEME = 66
        const val SEARCH_BY_AUDIO = 67
        const val MENTIONS = 68
        const val OWNER_ARTICLES = 69
        const val WALL_ATTACHMENTS = 70
        const val STORY_PLAYER = 71
        const val SINGLE_PHOTO = 72
        const val ARTIST = 73
        const val SHORT_LINKS = 74
        const val IMPORTANT_MESSAGES = 75
        const val MARKET_ALBUMS = 76
        const val MARKETS = 77
        const val MARKET_VIEW = 78
        const val GIFTS = 79
        const val PHOTO_ALL_COMMENT = 80
        const val ALBUMS_BY_VIDEO = 81
        const val FRIENDS_BY_PHONES = 82
        const val UNREAD_MESSAGES = 83
        const val AUDIOS_SEARCH_TABS = 84
        const val GROUP_CHATS = 85
        const val LOCAL_SERVER_PHOTO = 86
        const val VK_PHOTO_ALBUM_GALLERY_NATIVE = 87
        const val SEARCH_COMMENTS = 88
        const val SHORTCUTS = 89
        const val NARRATIVES = 90
        const val VOTERS = 91
        const val FEED_BAN = 92
        const val REMOTE_FILE_MANAGER = 93
        const val COMMUNITY_MEMBERS = 94
        const val FRIENDS_BIRTHDAYS = 95
        const val CATALOG_V2_AUDIO_CATALOG = 96
        const val CATALOG_V2_AUDIO_SECTION = 97
        const val CATALOG_V2_LIST_EDIT = 98
        const val STORIES_VIEWS = 99
        const val SHORT_VIDEOS = 100

        @JvmField
        val CREATOR: Parcelable.Creator<Place> = object : Parcelable.Creator<Place> {
            override fun createFromParcel(p: Parcel): Place {
                return Place(p)
            }

            override fun newArray(size: Int): Array<Place?> {
                return arrayOfNulls(size)
            }
        }
    }
}
