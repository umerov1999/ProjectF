package dev.ragnarok.fenrir.view.navigation

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.DrawerCategory
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.SwitchableCategory
import dev.ragnarok.fenrir.model.drawer.AbsMenuItem
import dev.ragnarok.fenrir.model.drawer.RecentChat
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceFactory.settingsThemePlace
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.disposables.CompositeDisposable

class AdditionalNavigationView : AbsNavigationView, MenuListAdapter.ActionListener {
    private val mCompositeDisposable = CompositeDisposable()
    private var mCallbacks: NavigationDrawerCallbacks? = null
    private var mBottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var ivHeaderAvatar: ImageView? = null
    private var tvUserName: TextView? = null
    private var tvDomain: TextView? = null
    private var mRecentChats: MutableList<RecentChat>? = null
    private var mAdapter: MenuListAdapter? = null
    private var mDrawerItems: ArrayList<AbsMenuItem>? = null
    private var mAccountId = 0L
    private var ownersRepository: IOwnersRepository = owners
    private var statesCallback: NavigationStatesCallbacks? = null
    private val navCallback = NavigationBottomSheetCallback()

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    ) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(context)
    }

    private inner class NavigationBottomSheetCallback : BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                statesCallback?.closeKeyboard()
            }
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                statesCallback?.onOpened()
            } else {
                statesCallback?.onClosed()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            statesCallback?.onMove(slideOffset)
        }

    }

    override fun setStatesCallback(callback: NavigationStatesCallbacks?) {
        mBottomSheetBehavior?.removeBottomSheetCallback(navCallback)
        statesCallback = callback
        mBottomSheetBehavior?.addBottomSheetCallback(navCallback)
    }

    private fun init(context: Context) {
        if (isInEditMode) {
            return
        }
        mAccountId = Settings.get()
            .accounts()
            .current
        mRecentChats = Settings.get()
            .recentChats()[mAccountId]
        mDrawerItems = ArrayList()
        mDrawerItems?.addAll(generateNavDrawerItems())
        mCompositeDisposable.add(
            Settings.get().drawerSettings()
                .observeChanges
                .observeOn(provideMainThreadScheduler())
                .subscribe { refreshNavigationItems(it) })

        val root = LayoutInflater.from(context).inflate(R.layout.fragment_navigation_drawer, this)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        val vgProfileContainer = root.findViewById<ViewGroup>(R.id.content_root)
        if (!Settings.get()
                .ui().isShow_profile_in_additional_page
        ) root.findViewById<View>(R.id.profile_view).visibility =
            View.GONE else root.findViewById<View>(R.id.profile_view).visibility = View.VISIBLE
        ivHeaderAvatar = root.findViewById(R.id.header_navi_menu_avatar)
        tvUserName = root.findViewById(R.id.header_navi_menu_username)
        tvDomain = root.findViewById(R.id.header_navi_menu_usernick)
        val ivHeaderDayNight = root.findViewById<ImageView>(R.id.header_navi_menu_day_night)
        val ivHeaderNotifications =
            root.findViewById<ImageView>(R.id.header_navi_menu_notifications)
        ivHeaderDayNight.setOnClickListener {
            if (Settings.get().ui().nightMode == AppCompatDelegate.MODE_NIGHT_YES || Settings.get()
                    .ui().nightMode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY || Settings.get()
                    .ui().nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ) {
                Settings.get().ui().switchNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                Settings.get().ui().switchNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
        ivHeaderNotifications.setOnClickListener {
            val rs = !Settings.get().other().isDisable_notifications
            Settings.get().other().isDisable_notifications = rs
            ivHeaderNotifications.setImageResource(if (rs) R.drawable.notification_disable else R.drawable.feed)
        }
        ivHeaderNotifications.setImageResource(
            if (Settings.get()
                    .other().isDisable_notifications
            ) R.drawable.notification_disable else R.drawable.feed
        )
        ivHeaderDayNight.setOnLongClickListener {
            settingsThemePlace.tryOpenWith(context)
            true
        }
        ivHeaderDayNight.setImageResource(
            if (Settings.get().ui().nightMode == AppCompatDelegate.MODE_NIGHT_YES || Settings.get()
                    .ui().nightMode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY || Settings.get()
                    .ui().nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ) R.drawable.ic_outline_nights_stay else R.drawable.ic_outline_wb_sunny
        )
        mAdapter = MenuListAdapter(context, mDrawerItems ?: mutableListOf(), this, true)
        mBottomSheetBehavior = BottomSheetBehavior.from(root.findViewById(R.id.bottom_sheet))
        mBottomSheetBehavior?.skipCollapsed = true
        mBottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset == -1f) {
                    mCallbacks?.onSheetClosed()
                }
            }
        })
        closeSheet()
        recyclerView.adapter = mAdapter
        refreshUserInfo()
        vgProfileContainer.setOnClickListener {
            closeSheet()
            openMyWall()
        }
    }

    private fun refreshUserInfo() {
        if (mAccountId != ISettings.IAccountsSettings.INVALID_ID) {
            mCompositeDisposable.add(
                ownersRepository.getBaseOwnerInfo(
                    mAccountId,
                    mAccountId,
                    IOwnersRepository.MODE_ANY
                )
                    .fromIOToMain()
                    .subscribe({ user -> refreshHeader(user) }, ignore())
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInEditMode) {
            return
        }
        mCallbacks = try {
            context as NavigationDrawerCallbacks
        } catch (ignored: ClassCastException) {
            throw ClassCastException("Activity must implement NavigationDrawerCallbacks.")
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isInEditMode) {
            return
        }
        mCompositeDisposable.dispose()
        mCallbacks = null
    }

    private fun openMyWall() {
        if (mAccountId == ISettings.IAccountsSettings.INVALID_ID) {
            return
        }
        getOwnerWallPlace(mAccountId, mAccountId, null).tryOpenWith(context)
    }

    override fun refreshNavigationItems(list: List<DrawerCategory>?) {
        mDrawerItems?.clear()
        if (list.isNullOrEmpty()) {
            mDrawerItems?.addAll(generateNavDrawerItems())
        } else {
            for (category in list) {
                if (category.isActive()) {
                    getItemBySwitchableCategory(category.getId())?.let { mDrawerItems?.add(it) }
                }
            }

//        items.add(new DividerMenuItem());
            mDrawerItems?.add(SECTION_ITEM_SETTINGS)
            mDrawerItems?.add(SECTION_ITEM_ACCOUNTS)
            if (mRecentChats.nonNullNoEmpty() && Settings.get()
                    .other().isEnable_show_recent_dialogs
            ) {
                mRecentChats?.let { mDrawerItems?.addAll(it) }
            }
        }
        safellyNotifyDataSetChanged()
        backupRecentChats()
    }

    private fun generateNavDrawerItems(): ArrayList<AbsMenuItem> {
        val settings = Settings.get().drawerSettings()
        @SwitchableCategory val categories = settings.categoriesOrder
        val items = ArrayList<AbsMenuItem>()
        for (category in categories) {
            if (category.isActive()) {
                getItemBySwitchableCategory(category.getId())?.let { items.add(it) }
            }
        }

//        items.add(new DividerMenuItem());
        items.add(SECTION_ITEM_SETTINGS)
        items.add(SECTION_ITEM_ACCOUNTS)
        if (mRecentChats.nonNullNoEmpty() && Settings.get().other().isEnable_show_recent_dialogs) {
            mRecentChats?.let { items.addAll(it) }
        }
        return items
    }

    /**
     * Добавить новый "недавний чат" в боковую панель
     * Если там уже есть более 4-х елементов, то удаляем последний
     *
     * @param recentChat новый чат
     */
    override fun appendRecentChat(recentChat: RecentChat) {
        if (mRecentChats == null) {
            mRecentChats = ArrayList(1)
        }
        mRecentChats?.let {
            val index = it.indexOf(recentChat)
            if (index != -1) {
                val old = it[index]

                // если вдруг мы дабавляем чат без иконки или названия, то сохраним эти
                // значения из пердыдущего (c тем же peer_id) елемента
                recentChat.iconUrl = firstNonEmptyString(recentChat.iconUrl, old.iconUrl)
                recentChat.title = firstNonEmptyString(recentChat.title, old.title)
                it[index] = recentChat
            } else {
                while (it.size >= Constants.MAX_RECENT_CHAT_COUNT) {
                    it.removeAt(it.size - 1)
                }
                it.add(0, recentChat)
            }
            refreshNavigationItems()
        }
    }

    private fun refreshHeader(user: Owner) {
        val avaUrl = user.maxSquareAvatar
        val transformation = CurrentTheme.createTransformationForAvatar()
        if (avaUrl != null) {
            ivHeaderAvatar?.let {
                with()
                    .load(avaUrl)
                    .transform(transformation)
                    .into(it)
            }
        } else {
            ivHeaderAvatar?.setImageResource(R.drawable.ic_avatar_unknown)
        }
        val domailText = "@" + user.domain.nonNullNoEmpty({
            it
        }, { user.ownerId.toString() })
        tvDomain?.text = domailText
        tvUserName?.text = user.fullName
    }

    override val isSheetOpen: Boolean
        get() = mBottomSheetBehavior != null && mBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED

    override fun openSheet() {
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun closeSheet() {
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun unblockSheet() {
        visibility = View.VISIBLE
    }

    override fun blockSheet() {
        visibility = View.GONE
    }

    override fun setUp(drawerLayout: DrawerLayout) {}
    override fun onUnreadDialogsCountChange(count: Int) {}
    override fun onUnreadNotificationsCountChange(count: Int) {}
    override fun checkCloseByClick(ev: MotionEvent): Boolean {
        if (!isSheetOpen) {
            return false
        }
        return if (ev.action == MotionEvent.ACTION_DOWN && ev.y < y) {
            closeSheet()
            true
        } else {
            false
        }
    }

    private fun selectItem(item: AbsMenuItem, longClick: Boolean) {
        closeSheet()
        mCallbacks?.onSheetItemSelected(item, longClick)
    }

    override fun selectPage(item: AbsMenuItem?) {
        mDrawerItems?.let {
            for (i in it) {
                i.isSelected = (i == item)
            }
            safellyNotifyDataSetChanged()
        }
    }

    private fun backupRecentChats() {
        val chats: MutableList<RecentChat> = ArrayList(5)
        mDrawerItems?.let {
            for (item in it) {
                if (item is RecentChat) {
                    chats.add(item)
                }
            }
        }
        Settings.get()
            .recentChats()
            .store(mAccountId, chats)
    }

    override fun onAccountChange(newAccountId: Long) {
        backupRecentChats()
        mAccountId = newAccountId
        //        SECTION_ITEM_DIALOGS.setCount(Stores.getInstance()
//                .dialogs()
//                .getUnreadDialogsCount(mAccountId));
        mRecentChats = Settings.get()
            .recentChats()[mAccountId]
        refreshNavigationItems()
        if (mAccountId != ISettings.IAccountsSettings.INVALID_ID) {
            refreshUserInfo()
        }
    }

    private fun safellyNotifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun onDrawerItemClick(item: AbsMenuItem) {
        selectItem(item, false)
    }

    override fun onDrawerItemLongClick(item: AbsMenuItem) {
        selectItem(item, true)
    }

    private fun getItemBySwitchableCategory(@SwitchableCategory type: String): AbsMenuItem? {
        return when (type) {
            SwitchableCategory.FRIENDS -> SECTION_ITEM_FRIENDS
            SwitchableCategory.NEWSFEED_COMMENTS -> SECTION_ITEM_NEWSFEED_COMMENTS
            SwitchableCategory.GROUPS -> SECTION_ITEM_GROUPS
            SwitchableCategory.PHOTOS -> SECTION_ITEM_PHOTOS
            SwitchableCategory.VIDEOS -> SECTION_ITEM_VIDEOS
            SwitchableCategory.MUSIC -> SECTION_ITEM_AUDIOS
            SwitchableCategory.DOCS -> SECTION_ITEM_DOCS
            SwitchableCategory.FAVES -> SECTION_ITEM_BOOKMARKS
            else -> null
        }
    }
}