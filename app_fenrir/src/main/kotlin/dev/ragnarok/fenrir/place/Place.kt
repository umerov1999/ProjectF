package dev.ragnarok.fenrir.place

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.result.ActivityResultLauncher
import dev.ragnarok.fenrir.util.Utils

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
                Utils.finishActivityImmediate(context)
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
        const val VK_PHOTO_ALBUM_GALLERY_NATIVE = 27
        const val VK_PHOTO_ALBUM_GALLERY = 28
        const val VK_PHOTO_ALBUM_GALLERY_SAVED = 29
        const val VK_PHOTO_TMP_SOURCE = 30
        const val FAVE_PHOTOS_GALLERY = 31
        const val SIMPLE_PHOTO_GALLERY = 32
        const val SINGLE_PHOTO = 33
        const val POLL = 34
        const val PREFERENCES = 35
        const val DOCS = 36
        const val FEED = 37
        const val NOTIFICATIONS = 38
        const val BOOKMARKS = 39
        const val RESOLVE_DOMAIN = 40
        const val VK_INTERNAL_PLAYER = 41
        const val NOTIFICATION_SETTINGS = 42
        const val CREATE_PHOTO_ALBUM = 43
        const val EDIT_PHOTO_ALBUM = 44
        const val MESSAGE_LOOKUP = 45
        const val GIF_PAGER = 46
        const val SECURITY = 47
        const val CREATE_POLL = 48
        const val COMMENT_CREATE = 49
        const val LOGS = 50
        const val LOCAL_IMAGE_ALBUM = 51
        const val SINGLE_SEARCH = 52
        const val NEWSFEED_COMMENTS = 53
        const val COMMUNITY_CONTROL = 54
        const val COMMUNITY_BAN_EDIT = 55
        const val COMMUNITY_ADD_BAN = 56
        const val COMMUNITY_MANAGER_EDIT = 57
        const val COMMUNITY_MANAGER_ADD = 58
        const val REQUEST_EXECUTOR = 59
        const val USER_BLACKLIST = 60
        const val PROXY_ADD = 61
        const val DRAWER_EDIT = 62
        const val SIDE_DRAWER_EDIT = 63
        const val USER_DETAILS = 64
        const val AUDIOS_IN_ALBUM = 65
        const val COMMUNITY_INFO = 66
        const val COMMUNITY_INFO_LINKS = 67
        const val SETTINGS_THEME = 68
        const val SEARCH_BY_AUDIO = 69
        const val MENTIONS = 70
        const val OWNER_ARTICLES = 71
        const val WALL_ATTACHMENTS = 72
        const val STORY_PLAYER = 73
        const val ARTIST = 74
        const val SHORT_LINKS = 75
        const val IMPORTANT_MESSAGES = 76
        const val MARKET_ALBUMS = 77
        const val MARKETS = 78
        const val MARKET_VIEW = 79
        const val GIFTS = 80
        const val PHOTO_ALL_COMMENT = 81
        const val ALBUMS_BY_VIDEO = 82
        const val FRIENDS_BY_PHONES = 83
        const val UNREAD_MESSAGES = 84
        const val AUDIOS_SEARCH_TABS = 85
        const val GROUP_CHATS = 86
        const val LOCAL_SERVER_PHOTO = 87
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
