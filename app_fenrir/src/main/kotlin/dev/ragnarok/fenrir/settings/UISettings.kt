package dev.ragnarok.fenrir.settings

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.fragment.PreferencesFragment
import dev.ragnarok.fenrir.fragment.fave.FaveTabsFragment
import dev.ragnarok.fenrir.fragment.friends.friendstabs.FriendsTabsFragment
import dev.ragnarok.fenrir.fragment.search.SearchTabsFragment
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getAudiosPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getBookmarksPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getCommunitiesPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getDialogsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getDocumentsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getFeedPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getFriendsFollowersPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getNewsfeedCommentsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getNotificationsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getPreferencesPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSearchPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVKPhotoAlbumsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVideosPlace
import dev.ragnarok.fenrir.settings.ISettings.IUISettings

internal class UISettings(context: Context) : IUISettings {
    private val app: Context = context.applicationContext
    override val avatarStyle: Int
        get() {
            val preferences = getPreferences(app)
            return preferences.getInt(PreferencesFragment.KEY_AVATAR_STYLE, AvatarStyle.CIRCLE)
        }

    override fun storeAvatarStyle(@AvatarStyle style: Int) {
        getPreferences(app)
            .edit()
            .putInt(PreferencesFragment.KEY_AVATAR_STYLE, style)
            .apply()
    }

    override val mainThemeKey: String
        get() {
            val preferences = getPreferences(app)
            return preferences.getString("app_theme", "cold")!!
        }

    override fun setMainTheme(key: String) {
        val preferences = getPreferences(app)
        preferences.edit().putString("app_theme", key).apply()
    }

    override fun switchNightMode(@AppCompatDelegate.NightMode key: Int) {
        val preferences = getPreferences(app)
        preferences.edit().putString("night_switch", key.toString()).apply()
    }

    override fun isDarkModeEnabled(context: Context): Boolean {
        val nightMode = (context.resources.configuration.uiMode
                and Configuration.UI_MODE_NIGHT_MASK)
        return nightMode == Configuration.UI_MODE_NIGHT_YES
    }

    @get:AppCompatDelegate.NightMode
    override val nightMode: Int
        get() = try {
            getPreferences(app)
                .getString("night_switch", AppCompatDelegate.MODE_NIGHT_YES.toString())!!
                .trim { it <= ' ' }
                .toInt()
        } catch (e: Exception) {
            AppCompatDelegate.MODE_NIGHT_YES
        }

    override fun getDefaultPage(accountId: Int): Place {
        val preferences = getPreferences(app)
        val page = preferences.getString(PreferencesFragment.KEY_DEFAULT_CATEGORY, "last_closed")
        if ("last_closed" == page) {
            when (getPreferences(app).getInt("last_closed_place_type", Place.DIALOGS)) {
                Place.DIALOGS -> return getDialogsPlace(accountId, accountId, null)
                Place.FEED -> return getFeedPlace(accountId)
                Place.FRIENDS_AND_FOLLOWERS -> return getFriendsFollowersPlace(
                    accountId,
                    accountId,
                    FriendsTabsFragment.TAB_ALL_FRIENDS,
                    null
                )
                Place.NOTIFICATIONS -> return getNotificationsPlace(accountId)
                Place.NEWSFEED_COMMENTS -> return getNewsfeedCommentsPlace(accountId)
                Place.COMMUNITIES -> return getCommunitiesPlace(accountId, accountId)
                Place.VK_PHOTO_ALBUMS -> return getVKPhotoAlbumsPlace(
                    accountId,
                    accountId,
                    null,
                    null
                )
                Place.AUDIOS -> return getAudiosPlace(accountId, accountId)
                Place.DOCS -> return getDocumentsPlace(accountId, accountId, null)
                Place.BOOKMARKS -> return getBookmarksPlace(accountId, FaveTabsFragment.TAB_PAGES)
                Place.SEARCH -> return getSearchPlace(accountId, SearchTabsFragment.TAB_PEOPLE)
                Place.VIDEOS -> return getVideosPlace(accountId, accountId, null)
                Place.PREFERENCES -> return getPreferencesPlace(accountId)
            }
        }
        return when (page) {
            "1" -> getFriendsFollowersPlace(
                accountId,
                accountId,
                FriendsTabsFragment.TAB_ALL_FRIENDS,
                null
            )
            "3" -> getFeedPlace(accountId)
            "4" -> getNotificationsPlace(accountId)
            "5" -> getCommunitiesPlace(accountId, accountId)
            "6" -> getVKPhotoAlbumsPlace(accountId, accountId, null, null)
            "7" -> getVideosPlace(accountId, accountId, null)
            "8" -> getAudiosPlace(accountId, accountId)
            "9" -> getDocumentsPlace(accountId, accountId, null)
            "10" -> getBookmarksPlace(accountId, FaveTabsFragment.TAB_PAGES)
            "11" -> getSearchPlace(accountId, SearchTabsFragment.TAB_PEOPLE)
            "12" -> getNewsfeedCommentsPlace(accountId)
            "13" -> getOwnerWallPlace(accountId, accountId, null)
            else -> getDialogsPlace(accountId, accountId, null)
        }
    }

    override fun notifyPlaceResumed(type: Int) {
        getPreferences(app).edit()
            .putInt("last_closed_place_type", type)
            .apply()
    }

    override val isSystemEmoji: Boolean
        get() = getPreferences(app).getBoolean("emojis_type", false)
    override val isEmojis_full_screen: Boolean
        get() = getPreferences(app).getBoolean("emojis_full_screen", false)
    override val isStickers_by_theme: Boolean
        get() = getPreferences(app).getBoolean("stickers_by_theme", true)
    override val isStickers_by_new: Boolean
        get() = getPreferences(app).getBoolean("stickers_by_new", false)
    override val isShow_profile_in_additional_page: Boolean
        get() = getPreferences(app).getBoolean("show_profile_in_additional_page", true)

    @get:SwipesChatMode
    override val swipes_chat_mode: Int
        get() = try {
            getPreferences(app).getString("swipes_for_chats", "1")!!
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            SwipesChatMode.SLIDR
        }
    override val isDisplay_writing: Boolean
        get() = getPreferences(app).getBoolean("display_writing", true)

}