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
        const val SIMPLE_PHOTO_GALLERY_NATIVE = 33
        const val SINGLE_PHOTO = 34
        const val POLL = 35
        const val PREFERENCES = 36
        const val DOCS = 37
        const val FEED = 38
        const val NOTIFICATIONS = 39
        const val BOOKMARKS = 40
        const val RESOLVE_DOMAIN = 41
        const val VK_INTERNAL_PLAYER = 42
        const val NOTIFICATION_SETTINGS = 43
        const val CREATE_PHOTO_ALBUM = 44
        const val EDIT_PHOTO_ALBUM = 45
        const val MESSAGE_LOOKUP = 46
        const val GIF_PAGER = 47
        const val SECURITY = 48
        const val CREATE_POLL = 49
        const val COMMENT_CREATE = 50
        const val LOGS = 51
        const val LOCAL_IMAGE_ALBUM = 52
        const val SINGLE_SEARCH = 53
        const val NEWSFEED_COMMENTS = 54
        const val COMMUNITY_CONTROL = 55
        const val COMMUNITY_BAN_EDIT = 56
        const val COMMUNITY_ADD_BAN = 57
        const val COMMUNITY_MANAGER_EDIT = 58
        const val COMMUNITY_MANAGER_ADD = 59
        const val REQUEST_EXECUTOR = 60
        const val USER_BLACKLIST = 61
        const val PROXY_ADD = 62
        const val DRAWER_EDIT = 63
        const val SIDE_DRAWER_EDIT = 64
        const val USER_DETAILS = 65
        const val AUDIOS_IN_ALBUM = 66
        const val COMMUNITY_INFO = 67
        const val COMMUNITY_INFO_LINKS = 68
        const val SETTINGS_THEME = 69
        const val SEARCH_BY_AUDIO = 70
        const val MENTIONS = 71
        const val OWNER_ARTICLES = 72
        const val WALL_ATTACHMENTS = 73
        const val STORY_PLAYER = 74
        const val ARTIST = 75
        const val SHORT_LINKS = 76
        const val IMPORTANT_MESSAGES = 77
        const val MARKET_ALBUMS = 78
        const val MARKETS = 79
        const val MARKET_VIEW = 80
        const val GIFTS = 81
        const val PHOTO_ALL_COMMENT = 82
        const val ALBUMS_BY_VIDEO = 83
        const val FRIENDS_BY_PHONES = 84
        const val UNREAD_MESSAGES = 85
        const val AUDIOS_SEARCH_TABS = 86
        const val GROUP_CHATS = 87
        const val LOCAL_SERVER_PHOTO = 88
        const val SEARCH_COMMENTS = 89
        const val SHORTCUTS = 90
        const val NARRATIVES = 91
        const val VOTERS = 92
        const val FEED_BAN = 93
        const val REMOTE_FILE_MANAGER = 94
        const val COMMUNITY_MEMBERS = 95
        const val FRIENDS_BIRTHDAYS = 96
        const val CATALOG_V2_AUDIO_CATALOG = 97
        const val CATALOG_V2_AUDIO_SECTION = 98
        const val CATALOG_V2_LIST_EDIT = 99
        const val STORIES_VIEWS = 100
        const val SHORT_VIDEOS = 101

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
