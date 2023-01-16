package dev.ragnarok.fenrir.view.navigation

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import dev.ragnarok.fenrir.model.drawer.DividerMenuItem
import dev.ragnarok.fenrir.model.drawer.RecentChat
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.BlurTransformation
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceFactory.settingsThemePlace
import dev.ragnarok.fenrir.settings.AvatarStyle
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.dp
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.Utils.getVerifiedColor
import dev.ragnarok.fenrir.util.Utils.setBackgroundTint
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import io.reactivex.rxjava3.disposables.CompositeDisposable

class SideNavigationView : AbsNavigationView, MenuListAdapter.ActionListener {
    private val mCompositeDisposable = CompositeDisposable()
    private var mCallbacks: NavigationDrawerCallbacks? = null
    private var mDrawerLayout: DrawerLayout? = null
    private var ivHeaderAvatar: ImageView? = null
    private var ivVerified: ImageView? = null
    private var bDonate: RLottieImageView? = null
    private var tvUserName: TextView? = null
    private var tvDomain: TextView? = null
    private var mRecentChats: MutableList<RecentChat>? = null
    private var mAdapter: MenuListAdapter? = null
    private var mDrawerItems: MutableList<AbsMenuItem>? = null
    private var backgroundImage: ImageView? = null
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

    private inner class NavigationBottomSheetCallback : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            statesCallback?.onMove(slideOffset)
        }

        override fun onDrawerOpened(drawerView: View) {
            statesCallback?.onOpened()
        }

        override fun onDrawerClosed(drawerView: View) {
            statesCallback?.onClosed()
        }

        override fun onDrawerStateChanged(newState: Int) {
            if (newState != DrawerLayout.STATE_IDLE || mDrawerLayout?.isDrawerOpen(
                    GravityCompat.START
                ) == true
            ) {
                statesCallback?.closeKeyboard()
            }
        }
    }

    override fun setStatesCallback(callback: NavigationStatesCallbacks?) {
        mDrawerLayout?.removeDrawerListener(navCallback)
        statesCallback = callback
        mDrawerLayout?.addDrawerListener(navCallback)
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
            Settings.get().sideDrawerSettings()
                .observeChanges()
                .observeOn(provideMainThreadScheduler())
                .subscribe { refreshNavigationItems(it) })
        val inflater = LayoutInflater.from(context)
        val root = inflater.inflate(R.layout.fragment_side_navigation_drawer, this)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val vHeader = inflater.inflate(R.layout.side_header_navi_menu, recyclerView, false)
        if (!Settings.get().ui().isShow_profile_in_additional_page) vHeader.visibility =
            View.GONE else vHeader.visibility = View.VISIBLE
        backgroundImage = vHeader.findViewById(R.id.header_navi_menu_background)
        ivHeaderAvatar = vHeader.findViewById(R.id.header_navi_menu_avatar)
        ivHeaderAvatar?.setBackgroundResource(
            if (Settings.get()
                    .ui().avatarStyle == AvatarStyle.OVAL
            ) R.drawable.sel_button_square_small_white else R.drawable.sel_button_round_5_white
        )
        tvUserName = vHeader.findViewById(R.id.header_navi_menu_username)
        tvDomain = vHeader.findViewById(R.id.header_navi_menu_usernick)
        ivVerified = vHeader.findViewById(R.id.item_verified)
        bDonate = vHeader.findViewById(R.id.donated_anim)
        val ivHeaderDayNight = vHeader.findViewById<ImageView>(R.id.header_navi_menu_day_night)
        val ivHeaderNotifications =
            vHeader.findViewById<ImageView>(R.id.header_navi_menu_notifications)
        val ivHeaderThemes = vHeader.findViewById<ImageView>(R.id.header_navi_menu_themes)
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
        ivHeaderThemes.setOnClickListener {
            settingsThemePlace.tryOpenWith(
                context
            )
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
        mAdapter = MenuListAdapter(context, mDrawerItems ?: mutableListOf(), this, false)
        mAdapter?.addHeader(vHeader)
        recyclerView.adapter = mAdapter
        refreshUserInfo()
        ivHeaderAvatar?.setOnClickListener {
            closeSheet()
            openMyWall()
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

    override fun onUnreadDialogsCountChange(count: Int) {
        if (SECTION_ITEM_DIALOGS.count != count) {
            SECTION_ITEM_DIALOGS.count = count
            safellyNotifyDataSetChanged()
        }
    }

    override fun onUnreadNotificationsCountChange(count: Int) {
        if (SECTION_ITEM_FEEDBACK.count != count) {
            SECTION_ITEM_FEEDBACK.count = count
            safellyNotifyDataSetChanged()
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
            mDrawerItems?.add(DividerMenuItem())
            if (mRecentChats.nonNullNoEmpty() && Settings.get()
                    .other().isEnable_show_recent_dialogs
            ) {
                mRecentChats?.let { mDrawerItems?.addAll(it) }
                mDrawerItems?.add(DividerMenuItem())
            }
            mDrawerItems?.add(SECTION_ITEM_SETTINGS)
            mDrawerItems?.add(SECTION_ITEM_ACCOUNTS)
        }
        safellyNotifyDataSetChanged()
        backupRecentChats()
    }

    private fun generateNavDrawerItems(): ArrayList<AbsMenuItem> {
        val settings = Settings.get().sideDrawerSettings()
        @SwitchableCategory val categories = settings.categoriesOrder
        val items = ArrayList<AbsMenuItem>()
        for (category in categories) {
            if (category.isActive()) {
                getItemBySwitchableCategory(category.getId())?.let { items.add(it) }
            }
        }
        items.add(DividerMenuItem())
        if (mRecentChats.nonNullNoEmpty() && Settings.get().other().isEnable_show_recent_dialogs) {
            mRecentChats?.let { items.addAll(it) }
            items.add(DividerMenuItem())
        }
        items.add(SECTION_ITEM_SETTINGS)
        items.add(SECTION_ITEM_ACCOUNTS)
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
            backgroundImage?.let {
                with()
                    .load(avaUrl)
                    .transform(BlurTransformation(6f, context))
                    .into(it)
            }
        } else {
            ivHeaderAvatar?.let { with().cancelRequest(it) }
            backgroundImage?.let { with().cancelRequest(it) }
            ivHeaderAvatar?.setImageResource(R.drawable.ic_avatar_unknown)
        }
        val domailText = "@" + user.domain
        tvDomain?.text = domailText
        tvUserName?.text = user.fullName
        tvUserName?.setTextColor(getVerifiedColor(context, user.isVerified))
        tvDomain?.setTextColor(getVerifiedColor(context, user.isVerified))
        val donate_anim = Settings.get().other().donate_anim_set
        if (donate_anim > 0 && user.isDonated) {
            bDonate?.visibility = View.VISIBLE
            bDonate?.setAutoRepeat(true)
            if (donate_anim == 2) {
                val cur = Settings.get().ui().mainThemeKey
                if ("fire" == cur || "orange" == cur || "orange_gray" == cur || "yellow_violet" == cur) {
                    tvUserName?.setTextColor(Color.parseColor("#df9d00"))
                    tvDomain?.setTextColor(Color.parseColor("#df9d00"))
                    setBackgroundTint(ivVerified, Color.parseColor("#df9d00"))
                    bDonate?.fromRes(
                        dev.ragnarok.fenrir_common.R.raw.donater_fire,
                        dp(100f),
                        dp(100f),
                        null
                    )
                } else {
                    tvUserName?.setTextColor(CurrentTheme.getColorPrimary(context))
                    tvDomain?.setTextColor(CurrentTheme.getColorPrimary(context))
                    setBackgroundTint(ivVerified, CurrentTheme.getColorPrimary(context))
                    bDonate?.fromRes(
                        dev.ragnarok.fenrir_common.R.raw.donater_fire,
                        dp(100f),
                        dp(100f),
                        intArrayOf(0xFF812E, CurrentTheme.getColorPrimary(context)),
                        true
                    )
                }
            } else {
                bDonate?.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.donater,
                    dp(100f),
                    dp(100f),
                    intArrayOf(
                        0xffffff,
                        CurrentTheme.getColorPrimary(context),
                        0x777777,
                        CurrentTheme.getColorSecondary(context)
                    )
                )
            }
            bDonate?.playAnimation()
        } else {
            bDonate?.setImageDrawable(null)
            bDonate?.visibility = View.GONE
        }
        ivVerified?.visibility = if (user.isVerified) View.VISIBLE else View.GONE
    }

    override fun checkCloseByClick(ev: MotionEvent): Boolean {
        if (!isSheetOpen) {
            return false
        }
        return if (ev.action == MotionEvent.ACTION_DOWN && ev.x > x + width) {
            closeSheet()
            true
        } else {
            false
        }
    }

    override val isSheetOpen: Boolean
        get() = mDrawerLayout?.isDrawerOpen(this) == true

    override fun openSheet() {
        mDrawerLayout?.openDrawer(this)
    }

    override fun closeSheet() {
        mDrawerLayout?.closeDrawer(this)
    }

    override fun unblockSheet() {
        mDrawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    override fun blockSheet() {
        mDrawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    override fun setUp(drawerLayout: DrawerLayout) {
        mDrawerLayout = drawerLayout
        mDrawerLayout?.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                mCallbacks?.onSheetClosed()
            }
        })
    }

    private fun selectItem(item: AbsMenuItem, longClick: Boolean) {
        mDrawerLayout?.closeDrawer(this)
        mCallbacks?.onSheetItemSelected(item, longClick)
    }

    override fun selectPage(item: AbsMenuItem?) {
        mDrawerItems?.let {
            for (i in it) {
                i.isSelected = (i == item)
            }
        }
        safellyNotifyDataSetChanged()
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

    companion object {
        internal fun getItemBySwitchableCategory(@SwitchableCategory type: String): AbsMenuItem? {
            return when (type) {
                SwitchableCategory.FRIENDS -> SECTION_ITEM_FRIENDS
                SwitchableCategory.DIALOGS -> SECTION_ITEM_DIALOGS
                SwitchableCategory.FEED -> SECTION_ITEM_FEED
                SwitchableCategory.FEEDBACK -> SECTION_ITEM_FEEDBACK
                SwitchableCategory.NEWSFEED_COMMENTS -> SECTION_ITEM_NEWSFEED_COMMENTS
                SwitchableCategory.GROUPS -> SECTION_ITEM_GROUPS
                SwitchableCategory.PHOTOS -> SECTION_ITEM_PHOTOS
                SwitchableCategory.VIDEOS -> SECTION_ITEM_VIDEOS
                SwitchableCategory.MUSIC -> SECTION_ITEM_AUDIOS
                SwitchableCategory.DOCS -> SECTION_ITEM_DOCS
                SwitchableCategory.FAVES -> SECTION_ITEM_BOOKMARKS
                SwitchableCategory.SEARCH -> SECTION_ITEM_SEARCH
                else -> null
            }
        }
    }
}