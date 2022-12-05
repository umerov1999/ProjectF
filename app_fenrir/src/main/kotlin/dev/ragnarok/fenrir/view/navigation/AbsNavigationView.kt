package dev.ragnarok.fenrir.view.navigation

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.drawerlayout.widget.DrawerLayout
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.DrawerCategory
import dev.ragnarok.fenrir.model.drawer.AbsMenuItem
import dev.ragnarok.fenrir.model.drawer.IconMenuItem
import dev.ragnarok.fenrir.model.drawer.RecentChat
import dev.ragnarok.fenrir.model.drawer.SectionMenuItem

abstract class AbsNavigationView : FrameLayout {
    abstract fun refreshNavigationItems(list: List<DrawerCategory>? = null)
    abstract fun appendRecentChat(recentChat: RecentChat)
    abstract val isSheetOpen: Boolean
    abstract fun openSheet()
    abstract fun closeSheet()
    abstract fun unblockSheet()
    abstract fun blockSheet()
    abstract fun selectPage(item: AbsMenuItem?)
    abstract fun setUp(drawerLayout: DrawerLayout)
    abstract fun onAccountChange(newAccountId: Int)
    abstract fun onUnreadDialogsCountChange(count: Int)
    abstract fun onUnreadNotificationsCountChange(count: Int)
    abstract fun checkCloseByClick(ev: MotionEvent): Boolean
    interface NavigationDrawerCallbacks {
        fun onSheetItemSelected(item: AbsMenuItem, longClick: Boolean)
        fun onSheetClosed()
    }

    abstract fun setStatesCallback(callback: NavigationStatesCallbacks?)
    interface NavigationStatesCallbacks {
        fun onMove(slideOffset: Float)
        fun onOpened()
        fun onClosed()
        fun closeKeyboard()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    companion object {
        const val PAGE_FRIENDS = 0
        const val PAGE_DIALOGS = 1
        const val PAGE_FEED = 2
        const val PAGE_MUSIC = 3
        const val PAGE_DOCUMENTS = 4
        const val PAGE_PHOTOS = 5
        const val PAGE_PREFERENSES = 6
        const val PAGE_ACCOUNTS = 7
        const val PAGE_GROUPS = 8
        const val PAGE_VIDEOS = 9
        const val PAGE_BOOKMARKS = 10
        const val PAGE_NOTIFICATION = 11
        const val PAGE_SEARCH = 12
        const val PAGE_NEWSFEED_COMMENTS = 13

        val SECTION_ITEM_FRIENDS: SectionMenuItem =
            IconMenuItem(PAGE_FRIENDS, R.drawable.friends, R.string.friends)

        val SECTION_ITEM_DIALOGS: SectionMenuItem =
            IconMenuItem(PAGE_DIALOGS, R.drawable.email, R.string.dialogs)

        val SECTION_ITEM_FEED: SectionMenuItem =
            IconMenuItem(PAGE_FEED, R.drawable.rss, R.string.feed)

        val SECTION_ITEM_FEEDBACK: SectionMenuItem =
            IconMenuItem(PAGE_NOTIFICATION, R.drawable.feed, R.string.drawer_feedback)

        val SECTION_ITEM_NEWSFEED_COMMENTS: SectionMenuItem = IconMenuItem(
            PAGE_NEWSFEED_COMMENTS,
            R.drawable.comment,
            R.string.drawer_newsfeed_comments
        )

        val SECTION_ITEM_GROUPS: SectionMenuItem =
            IconMenuItem(PAGE_GROUPS, R.drawable.groups, R.string.groups)

        val SECTION_ITEM_PHOTOS: SectionMenuItem =
            IconMenuItem(PAGE_PHOTOS, R.drawable.photo_album, R.string.photos)

        val SECTION_ITEM_VIDEOS: SectionMenuItem =
            IconMenuItem(PAGE_VIDEOS, R.drawable.video, R.string.videos)

        val SECTION_ITEM_BOOKMARKS: SectionMenuItem =
            IconMenuItem(PAGE_BOOKMARKS, R.drawable.star, R.string.bookmarks)

        val SECTION_ITEM_AUDIOS: SectionMenuItem =
            IconMenuItem(PAGE_MUSIC, R.drawable.music, R.string.music)

        val SECTION_ITEM_DOCS: SectionMenuItem =
            IconMenuItem(PAGE_DOCUMENTS, R.drawable.file, R.string.attachment_documents)

        val SECTION_ITEM_SEARCH: SectionMenuItem =
            IconMenuItem(PAGE_SEARCH, R.drawable.magnify, R.string.search)

        val SECTION_ITEM_SETTINGS: SectionMenuItem =
            IconMenuItem(PAGE_PREFERENSES, R.drawable.preferences, R.string.settings)

        val SECTION_ITEM_ACCOUNTS: SectionMenuItem =
            IconMenuItem(PAGE_ACCOUNTS, R.drawable.account_circle, R.string.accounts)
    }
}