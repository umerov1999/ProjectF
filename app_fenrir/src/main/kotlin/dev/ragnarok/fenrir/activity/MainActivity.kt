package dev.ragnarok.fenrir.activity

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.BaseTransientBottomBar
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.Includes.proxySettings
import dev.ragnarok.fenrir.Includes.pushRegistrationResolver
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityUtils.checkInputExist
import dev.ragnarok.fenrir.activity.ActivityUtils.isMimeAudio
import dev.ragnarok.fenrir.activity.gifpager.GifPagerActivity
import dev.ragnarok.fenrir.activity.photopager.PhotoPagerActivity.Companion.newInstance
import dev.ragnarok.fenrir.activity.qr.CameraScanActivity
import dev.ragnarok.fenrir.activity.shortvideopager.ShortVideoPagerActivity
import dev.ragnarok.fenrir.activity.storypager.StoryPagerActivity
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.dialog.ResolveDomainDialog
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.impl.CountersInteractor
import dev.ragnarok.fenrir.fragment.BrowserFragment
import dev.ragnarok.fenrir.fragment.DocPreviewFragment
import dev.ragnarok.fenrir.fragment.PreferencesFragment
import dev.ragnarok.fenrir.fragment.PreferencesFragment.Companion.cleanCache
import dev.ragnarok.fenrir.fragment.SecurityPreferencesFragment
import dev.ragnarok.fenrir.fragment.accounts.processauthcode.ProcessAuthCodeFragment
import dev.ragnarok.fenrir.fragment.attachments.commentcreate.CommentCreateFragment
import dev.ragnarok.fenrir.fragment.attachments.commentedit.CommentEditFragment
import dev.ragnarok.fenrir.fragment.attachments.postcreate.PostCreateFragment
import dev.ragnarok.fenrir.fragment.attachments.postedit.PostEditFragment
import dev.ragnarok.fenrir.fragment.attachments.repost.RepostFragment
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment
import dev.ragnarok.fenrir.fragment.audio.AudiosTabsFragment
import dev.ragnarok.fenrir.fragment.audio.audios.AudiosFragment
import dev.ragnarok.fenrir.fragment.audio.audiosbyartist.AudiosByArtistFragment
import dev.ragnarok.fenrir.fragment.audio.audiosrecommendation.AudiosRecommendationFragment
import dev.ragnarok.fenrir.fragment.audio.catalog_v2.listedit.CatalogV2ListEditFragment
import dev.ragnarok.fenrir.fragment.audio.catalog_v2.lists.CatalogV2ListFragment
import dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.CatalogV2SectionFragment
import dev.ragnarok.fenrir.fragment.comments.CommentsFragment
import dev.ragnarok.fenrir.fragment.communities.CommunitiesFragment
import dev.ragnarok.fenrir.fragment.communities.communitycontrol.CommunityControlFragment
import dev.ragnarok.fenrir.fragment.communities.communitycontrol.communityban.CommunityBanEditFragment
import dev.ragnarok.fenrir.fragment.communities.communitycontrol.communityinfocontacts.CommunityInfoContactsFragment
import dev.ragnarok.fenrir.fragment.communities.communitycontrol.communityinfolinks.CommunityInfoLinksFragment
import dev.ragnarok.fenrir.fragment.communities.communitycontrol.communitymanageredit.CommunityManagerEditFragment
import dev.ragnarok.fenrir.fragment.communities.communitycontrol.communitymembers.CommunityMembersFragment
import dev.ragnarok.fenrir.fragment.communities.groupchats.GroupChatsFragment
import dev.ragnarok.fenrir.fragment.docs.DocsFragment
import dev.ragnarok.fenrir.fragment.docs.DocsListPresenter
import dev.ragnarok.fenrir.fragment.fave.FaveTabsFragment
import dev.ragnarok.fenrir.fragment.feed.FeedFragment
import dev.ragnarok.fenrir.fragment.feed.feedbanned.FeedBannedFragment
import dev.ragnarok.fenrir.fragment.feed.newsfeedcomments.NewsfeedCommentsFragment
import dev.ragnarok.fenrir.fragment.feed.newsfeedmentions.NewsfeedMentionsFragment
import dev.ragnarok.fenrir.fragment.feedback.FeedbackFragment
import dev.ragnarok.fenrir.fragment.feedback.feedbackvkofficial.FeedbackVKOfficialFragment
import dev.ragnarok.fenrir.fragment.friends.birthday.BirthDayFragment
import dev.ragnarok.fenrir.fragment.friends.friendsbyphones.FriendsByPhonesFragment
import dev.ragnarok.fenrir.fragment.friends.friendstabs.FriendsTabsFragment
import dev.ragnarok.fenrir.fragment.gifts.GiftsFragment
import dev.ragnarok.fenrir.fragment.likes.LikesFragment
import dev.ragnarok.fenrir.fragment.likes.storiesview.StoriesViewFragment
import dev.ragnarok.fenrir.fragment.localserver.filemanagerremote.FileManagerRemoteFragment
import dev.ragnarok.fenrir.fragment.localserver.photoslocalserver.PhotosLocalServerFragment
import dev.ragnarok.fenrir.fragment.logs.LogsFragment
import dev.ragnarok.fenrir.fragment.messages.chat.ChatFragment
import dev.ragnarok.fenrir.fragment.messages.chat.ChatFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.messages.chatmembers.ChatMembersFragment
import dev.ragnarok.fenrir.fragment.messages.conversationattachments.ConversationFragmentFactory
import dev.ragnarok.fenrir.fragment.messages.dialogs.DialogsFragment
import dev.ragnarok.fenrir.fragment.messages.fwds.FwdsFragment
import dev.ragnarok.fenrir.fragment.messages.importantmessages.ImportantMessagesFragment
import dev.ragnarok.fenrir.fragment.messages.messageslook.MessagesLookFragment
import dev.ragnarok.fenrir.fragment.messages.notreadmessages.NotReadMessagesFragment
import dev.ragnarok.fenrir.fragment.narratives.NarrativesFragment
import dev.ragnarok.fenrir.fragment.navigationedit.DrawerEditFragment
import dev.ragnarok.fenrir.fragment.navigationedit.SideDrawerEditFragment
import dev.ragnarok.fenrir.fragment.ownerarticles.OwnerArticlesFragment
import dev.ragnarok.fenrir.fragment.photos.createphotoalbum.CreatePhotoAlbumFragment
import dev.ragnarok.fenrir.fragment.photos.photoallcomment.PhotoAllCommentFragment
import dev.ragnarok.fenrir.fragment.photos.vkphotoalbums.VKPhotoAlbumsFragment
import dev.ragnarok.fenrir.fragment.photos.vkphotos.IVKPhotosView
import dev.ragnarok.fenrir.fragment.photos.vkphotos.VKPhotosFragment
import dev.ragnarok.fenrir.fragment.pin.createpin.CreatePinFragment
import dev.ragnarok.fenrir.fragment.poll.PollFragment
import dev.ragnarok.fenrir.fragment.poll.createpoll.CreatePollFragment
import dev.ragnarok.fenrir.fragment.poll.voters.VotersFragment
import dev.ragnarok.fenrir.fragment.products.ProductsFragment
import dev.ragnarok.fenrir.fragment.products.marketview.MarketViewFragment
import dev.ragnarok.fenrir.fragment.products.productalbums.ProductAlbumsFragment
import dev.ragnarok.fenrir.fragment.requestexecute.RequestExecuteFragment
import dev.ragnarok.fenrir.fragment.search.AudioSearchTabsFragment
import dev.ragnarok.fenrir.fragment.search.SearchTabsFragment
import dev.ragnarok.fenrir.fragment.search.SingleTabSearchFragment
import dev.ragnarok.fenrir.fragment.shortcutsview.ShortcutsViewFragment
import dev.ragnarok.fenrir.fragment.shortedlinks.ShortedLinksFragment
import dev.ragnarok.fenrir.fragment.theme.ThemeFragment
import dev.ragnarok.fenrir.fragment.topics.TopicsFragment
import dev.ragnarok.fenrir.fragment.userbanned.UserBannedFragment
import dev.ragnarok.fenrir.fragment.videos.IVideosListView
import dev.ragnarok.fenrir.fragment.videos.VideosFragment
import dev.ragnarok.fenrir.fragment.videos.VideosTabsFragment
import dev.ragnarok.fenrir.fragment.videos.videoalbumsbyvideo.VideoAlbumsByVideoFragment
import dev.ragnarok.fenrir.fragment.videos.videopreview.VideoPreviewFragment
import dev.ragnarok.fenrir.fragment.wall.AbsWallFragment
import dev.ragnarok.fenrir.fragment.wall.userdetails.UserDetailsFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.wall.wallattachments.WallAttachmentsFragmentFactory
import dev.ragnarok.fenrir.fragment.wall.wallattachments.wallsearchcommentsattachments.WallSearchCommentsAttachmentsFragment
import dev.ragnarok.fenrir.fragment.wall.wallpost.WallPostFragment
import dev.ragnarok.fenrir.getParcelableArrayListCompat
import dev.ragnarok.fenrir.getParcelableArrayListExtraCompat
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.getParcelableExtraCompat
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.listener.CanBackPressedCallback
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.UpdatableNavigation
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.ServiceToken
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.bindToServiceWithoutStart
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.isPlaying
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.stop
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.unbindFromService
import dev.ragnarok.fenrir.media.music.MusicPlaybackService
import dev.ragnarok.fenrir.media.music.MusicPlaybackService.Companion.startForPlayList
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.Banned
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.SectionCounters
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UserDetails
import dev.ragnarok.fenrir.model.drawer.AbsMenuItem
import dev.ragnarok.fenrir.model.drawer.RecentChat
import dev.ragnarok.fenrir.model.drawer.SectionMenuItem
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.settings.CurrentTheme.getNavigationBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarNonColored
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.SwipesChatMode
import dev.ragnarok.fenrir.settings.theme.ThemesController.currentStyle
import dev.ragnarok.fenrir.settings.theme.ThemesController.nextRandom
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Accounts
import dev.ragnarok.fenrir.util.Action
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.MainActivityTransforms
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.hasVanillaIceCreamTarget
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.andThen
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayedFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.dummy
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView.NavigationDrawerCallbacks
import dev.ragnarok.fenrir.view.zoomhelper.ZoomHelper.Companion.getInstance
import kotlinx.coroutines.flow.filter
import java.util.regex.Pattern

open class MainActivity : AppCompatActivity(), NavigationDrawerCallbacks, OnSectionResumeCallback,
    AppStyleable, PlaceProvider, ServiceConnection, UpdatableNavigation,
    NavigationBarView.OnItemSelectedListener {
    private val mCompositeJob = CompositeJob()
    private val postResumeActions: MutableList<Action<MainActivity>> = ArrayList(0)
    private val requestEnterPin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            finish()
        }
    }
    protected var mAccountId = 0L
    private val requestEnterPinZero = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            finish()
        } else {
            Settings.get().ui().getDefaultPage(mAccountId).tryOpenWith(this)
        }
    }
    private val requestQRScan = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanner = result.data?.extras?.getString(Extra.URL)
            if (scanner.nonNullNoEmpty()) {
                val PATTERN: Pattern =
                    Pattern.compile("qr\\.vk\\.com/ca[?]q=(\\w+)")
                val matcher = PATTERN.matcher(scanner)
                try {
                    if (matcher.find()) {
                        matcher.group(1)
                            ?.let {
                                PlaceFactory.getProcessAuthCodePlace(mAccountId, it)
                                    .tryOpenWith(this)
                                return@registerForActivityResult
                            }
                    }
                } catch (_: NumberFormatException) {
                }

                MaterialAlertDialogBuilder(this)
                    .setIcon(R.drawable.qr_code)
                    .setMessage(scanner)
                    .setTitle(getString(R.string.scan_qr))
                    .setPositiveButton(R.string.open) { _, _ ->
                        LinkHelper.openUrl(
                            this,
                            mAccountId,
                            scanner, false
                        )
                    }
                    .setNeutralButton(R.string.copy_text) { _, _ ->
                        val clipboard = getSystemService(
                            CLIPBOARD_SERVICE
                        ) as ClipboardManager?
                        val clip = ClipData.newPlainText("response", scanner)
                        clipboard?.setPrimaryClip(clip)
                        createCustomToast(this).showToast(R.string.copied_to_clipboard)
                    }
                    .setCancelable(true)
                    .create().show()
            }
        }
    }
    private val requestLogin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        mAccountId = Settings.get()
            .accounts()
            .current
        if (mAccountId == ISettings.IAccountsSettings.INVALID_ID) {
            supportFinishAfterTransition()
        }
    }

    @get:LayoutRes
    protected open val mainContentView: Int
        get() = if (Settings.get().main().is_side_navigation) {
            if (Settings.get().main().isSnow_mode) {
                R.layout.activity_main_side_with_snow
            } else {
                R.layout.activity_main_side
            }
        } else {
            if (Settings.get().main().isSnow_mode) {
                R.layout.activity_main_with_snow
            } else {
                R.layout.activity_main
            }
        }

    @get:IdRes
    protected open val mainContainerViewId: Int
        get() = R.id.fragment

    @get:MainActivityTransforms
    protected open val mainActivityTransform: Int
        get() = MainActivityTransforms.MAIN

    protected var mLastBackPressedTime: Long = 0

    private val requestCreatePin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val values = CreatePinFragment.extractValueFromIntent(result.data)
            Settings.get()
                .security()
                .setPin(values)
            Settings.get().security().isUsePinForSecurity = true
        }
    }

    private fun startCreatePinActivity() {
        val o = Intent(this, CreatePinActivity::class.java)
        requestCreatePin.launch(o)
    }

    /**
     * Атрибуты секции, которая на данный момент находится на главном контейнере экрана
     */
    private var mCurrentFrontSection: AbsMenuItem? = null
    private var mToolbar: Toolbar? = null
    private var mBottomNavigation: BottomNavigationView? = null
    private var mBottomNavigationContainer: ViewGroup? = null
    private var mViewFragment: FragmentContainerView? = null
    private val requestLoginZero = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        mAccountId = Settings.get()
            .accounts()
            .current
        if (mAccountId == ISettings.IAccountsSettings.INVALID_ID) {
            supportFinishAfterTransition()
        } else {
            Settings.get().ui().getDefaultPage(mAccountId).tryOpenWith(this)
            checkFCMRegistration(true)

            if (!Settings.get().security().isUsePinForSecurity) {
                startCreatePinActivity()
            }
        }
    }
    private val mOnBackStackChangedListener = FragmentManager.OnBackStackChangedListener {
        resolveToolbarNavigationIcon()
        keyboardHide()
    }
    private var mAudioPlayServiceToken: ServiceToken? = null

    /**
     * First - DrawerItem, second - Clear back stack before adding
     */
    private var mTargetPage: Pair<AbsMenuItem, Boolean>? = null
    private var resumed = false
    private var isZoomPhoto = false

    private fun postResume(action: Action<MainActivity>) {
        if (resumed) {
            action.call(this)
        } else {
            postResumeActions.add(action)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.updateActivityContext(newBase))
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (navigationView?.checkCloseByClick(ev) == true) {
            return true
        }
        return if (!isZoomPhoto) {
            super.dispatchTouchEvent(ev)
        } else getInstance()?.dispatchTouchEvent(ev, this) == true || super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.applyDayNight()
        if (savedInstanceState == null && mainActivityTransform == MainActivityTransforms.MAIN) {
            nextRandom()
        }
        setTheme(currentStyle())
        Utils.prepareDensity(this)
        Utils.registerColorsThorVG(this)

        super.onCreate(savedInstanceState)
        isZoomPhoto = Settings.get().main().isDo_zoom_photo
        mCompositeJob.add(
            Settings.get()
                .accounts()
                .observeChanges
                .sharedFlowToMain { onCurrentAccountChange(it) })
        mCompositeJob.add(
            proxySettings
                .observeActive.sharedFlowToMain { stop() })
        mCompositeJob.add(
            Stores.instance
                .dialogs()
                .observeUnreadDialogsCount()
                .filter { it.first == mAccountId }
                .sharedFlowToMain { updateMessagesBagde(it.second) })
        bindToAudioPlayService()
        setContentView(mainContentView)
        mAccountId = Settings.get()
            .accounts()
            .current
        setStatusbarColored(true, Settings.get().ui().isDarkModeEnabled(this))
        val mDrawerLayout = findViewById<DrawerLayout>(R.id.my_drawer_layout)

        mViewFragment = findViewById(mainContainerViewId)
        val anim: ObjectAnimator
        if (mDrawerLayout != null && Settings.get().main().is_side_navigation) {
            navigationView?.setUp(mDrawerLayout)
            anim = ObjectAnimator.ofPropertyValuesHolder(
                mViewFragment, PropertyValuesHolder.ofFloat(
                    View.ALPHA, 1f, 1f, 0.6f
                ),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, 0f, 100f)
            )
            anim.interpolator = LinearInterpolator()
        } else {
            anim = ObjectAnimator.ofPropertyValuesHolder(
                mViewFragment, PropertyValuesHolder.ofFloat(
                    View.ALPHA, 1f, 1f, 0.6f
                ),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, 0f, -100f)
            )
            anim.interpolator = LinearInterpolator()
        }
        navigationView?.setStatesCallback(object : AbsNavigationView.NavigationStatesCallbacks {
            override fun onMove(slideOffset: Float) {
                anim.setCurrentFraction(slideOffset)
            }

            override fun onOpened() {
                anim.start()
            }

            override fun onClosed() {
                anim.cancel()
            }

            override fun closeKeyboard() {
                keyboardHide()
            }

        })
        mBottomNavigation = findViewById(R.id.bottom_navigation_menu)
        mBottomNavigation?.setOnItemSelectedListener(this)
        mBottomNavigationContainer = findViewById(R.id.bottom_navigation_menu_container)
        supportFragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener)
        resolveToolbarNavigationIcon()
        updateMessagesBagde(
            Stores.instance
                .dialogs()
                .getUnreadDialogsCount(mAccountId)
        )
        if (savedInstanceState == null) {
            val intentWasHandled = handleIntent(intent, true)
            if (!isAuthValid) {
                if (intentWasHandled) {
                    startAccountsActivity()
                } else {
                    startAccountsActivityZero()
                }
            } else {
                if (mainActivityTransform == MainActivityTransforms.MAIN) {
                    checkFCMRegistration(false)
                    mCompositeJob.add(
                        MusicPlaybackController.tracksExist.findAllAudios(this)
                            .andThen(
                                InteractorFactory.createStickersInteractor()
                                    .placeToStickerCache(this)
                            )
                            .fromIOToMain(dummy()) { t ->
                                if (Settings.get().main().isDeveloper_mode) {
                                    createCustomToast(this).showToastThrowable(t)
                                }
                            })
                    Settings.get().main().last_audio_sync.let {
                        if (it > 0 && (System.currentTimeMillis() / 1000L) - it > 900) {
                            Settings.get().main().set_last_audio_sync(-1)
                            mCompositeJob.add(
                                Includes.stores.tempStore().deleteAudios()
                                    .hiddenIO()
                            )
                        }
                    }
                    Settings.get().main().appStoredVersionEqual
                    if (Settings.get().main().isDelete_cache_images) {
                        cleanCache(this, false)
                    }
                }
                updateNotificationCount(mAccountId)
                val needPin = (Settings.get().security().isUsePinForEntrance
                        && !intent.getBooleanExtra(EXTRA_NO_REQUIRE_PIN, false) && !Settings.get()
                    .security().isDelayedAllow)
                if (needPin) {
                    if (!intentWasHandled) {
                        startEnterPinActivityZero()
                    } else {
                        startEnterPinActivity()
                    }
                } else {
                    if (!intentWasHandled) {
                        Settings.get().ui().getDefaultPage(mAccountId).tryOpenWith(this)
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navigationView?.isSheetOpen == true) {
                    navigationView?.closeSheet()
                    return
                }
                val front = frontFragment
                if (front is BackPressCallback) {
                    if (!(front as BackPressCallback).onBackPressed()) {
                        return
                    }
                }
                if (supportFragmentManager.backStackEntryCount == 1 || supportFragmentManager.backStackEntryCount <= 0) {
                    if (mainActivityTransform != MainActivityTransforms.SWIPEBLE) {
                        if (isFragmentWithoutNavigation) {
                            openNavigationPage(AbsNavigationView.SECTION_ITEM_FEED, false)
                            return
                        }
                        if (isChatFragment) {
                            openNavigationPage(AbsNavigationView.SECTION_ITEM_DIALOGS, false)
                            return
                        }
                    }
                    if (mLastBackPressedTime < 0
                        || mLastBackPressedTime + DOUBLE_BACK_PRESSED_TIMEOUT > System.currentTimeMillis()
                    ) {
                        supportFinishAfterTransition()
                        return
                    }
                    mLastBackPressedTime = System.currentTimeMillis()
                    CustomSnackbars.createCustomSnackbars(mViewFragment, mBottomNavigationContainer)
                        ?.setDurationSnack(BaseTransientBottomBar.LENGTH_SHORT)
                        ?.defaultSnack(R.string.click_back_to_exit)?.show()
                } else {
                    supportFragmentManager.popBackStack()
                }
            }
        })
        //CurrentTheme.dumpDynamicColors(this)
    }

    private fun updateNotificationCount(account: Long) {
        if (account == ISettings.IAccountsSettings.INVALID_ID) {
            removeNotificationsBadge()
            return
        }
        mCompositeJob.add(
            CountersInteractor(networkInterfaces).getApiCounters(account)
                .fromIOToMain({ counters -> updateNotificationsBadge(counters) }) { removeNotificationsBadge() })
    }

    override fun onPause() {
        resumed = false
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        for (action in postResumeActions) {
            action.call(this)
        }
        postResumeActions.clear()
    }

    private fun startEnterPinActivity() {
        requestEnterPin.launch(EnterPinActivity.getIntent(this))
    }

    private fun startEnterPinActivityZero() {
        requestEnterPinZero.launch(EnterPinActivity.getIntent(this))
    }

    private fun checkFCMRegistration(onlyCheckGMS: Boolean) {
        if (!checkPlayServices(this)) {
            if (!Settings.get().main().isDisabledErrorFCM) {
                mViewFragment?.let {
                    CustomSnackbars.createCustomSnackbars(mViewFragment, mBottomNavigationContainer)
                        ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                        ?.themedSnack(R.string.this_device_does_not_support_fcm)
                        ?.setAction(R.string.button_access) {
                            Settings.get().main().setDisableErrorFCM(true)
                        }
                        ?.show()
                }
            }
            return
        }
        if (onlyCheckGMS) {
            return
        }
        mCompositeJob.add(
            pushRegistrationResolver.resolvePushRegistration(mAccountId, this)
                .hiddenIO()
        )

        //RequestHelper.checkPushRegistration(this);
    }

    private fun bindToAudioPlayService() {
        if (mAudioPlayServiceToken == null) {
            mAudioPlayServiceToken = bindToServiceWithoutStart(this, this)
        }
    }

    private fun resolveToolbarNavigationIcon() {
        mToolbar ?: return
        val manager = supportFragmentManager
        if (manager.backStackEntryCount > 1 || frontFragment is CanBackPressedCallback && (frontFragment as CanBackPressedCallback?)?.canBackPressed() == true) {
            mToolbar?.setNavigationIcon(R.drawable.arrow_left)
            mToolbar?.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        } else {
            if (!isFragmentWithoutNavigation) {
                mToolbar?.setNavigationIcon(
                    if (Settings.get()
                            .main().isRunes_show
                    ) R.drawable.client_round else R.drawable.client_round_vk
                )
                mToolbar?.setNavigationOnClickListener {
                    val menus = ModalBottomSheetDialogFragment.Builder()
                    menus.add(
                        OptionRequest(
                            0,
                            getString(R.string.set_offline),
                            R.drawable.offline,
                            true
                        )
                    )
                    menus.add(
                        OptionRequest(
                            1,
                            getString(R.string.open_clipboard_url),
                            R.drawable.web,
                            false
                        )
                    )
                    menus.add(
                        OptionRequest(
                            2,
                            getString(R.string.stories),
                            R.drawable.story_outline,
                            true
                        )
                    )
                    if (Utils.isOfficialVKCurrent) {
                        menus.add(
                            OptionRequest(
                                3,
                                getString(R.string.clips),
                                R.drawable.clip_outline,
                                true
                            )
                        )
                    }
                    menus.add(
                        OptionRequest(
                            4,
                            getString(R.string.settings),
                            R.drawable.preferences,
                            true
                        )
                    )
                    menus.add(
                        OptionRequest(
                            5,
                            getString(R.string.scan_qr),
                            R.drawable.qr_code,
                            false
                        )
                    )
                    menus.show(
                        supportFragmentManager,
                        "main_activity_options"
                    ) { _, option ->
                        when (option.id) {
                            0 -> {
                                mCompositeJob.add(
                                    InteractorFactory.createAccountInteractor()
                                        .setOffline(
                                            Settings.get().accounts().current
                                        )
                                        .fromIOToMain({ onSetOffline(it) }) {
                                            onSetOffline(
                                                false
                                            )
                                        })
                            }

                            1 -> {
                                val clipBoard =
                                    getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
                                if (clipBoard != null && clipBoard.primaryClip != null && (clipBoard.primaryClip?.itemCount
                                        ?: 0) > 0 && (clipBoard.primaryClip
                                        ?: return@show).getItemAt(0).text != null
                                ) {
                                    val temp =
                                        clipBoard.primaryClip?.getItemAt(0)?.text.toString()
                                    LinkHelper.openUrl(
                                        this@MainActivity,
                                        mAccountId,
                                        temp,
                                        false
                                    )
                                }
                            }

                            2 -> {
                                mCompositeJob.add(
                                    InteractorFactory.createStoriesInteractor()
                                        .getStories(
                                            Settings.get().accounts().current,
                                            null
                                        )
                                        .fromIOToMain({
                                            if (it.isEmpty()) {
                                                createCustomToast(this@MainActivity).showToastError(
                                                    R.string.list_is_empty
                                                )
                                            }
                                            PlaceFactory.getHistoryVideoPreviewPlace(
                                                mAccountId,
                                                ArrayList(it),
                                                0
                                            ).tryOpenWith(this@MainActivity)
                                        }) {
                                            createCustomToast(this@MainActivity).showToastThrowable(
                                                it
                                            )
                                        })
                            }

                            3 -> {
                                PlaceFactory.getShortVideoPlace(mAccountId, null)
                                    .tryOpenWith(this@MainActivity)
                            }

                            4 -> {
                                PlaceFactory.getPreferencesPlace(mAccountId)
                                    .tryOpenWith(this@MainActivity)
                            }

                            5 -> {
                                val intent =
                                    Intent(
                                        this@MainActivity,
                                        CameraScanActivity::class.java
                                    )
                                requestQRScan.launch(intent)
                            }
                        }
                    }
                }
            } else {
                mToolbar?.setNavigationIcon(R.drawable.arrow_left)
                if (mainActivityTransform != MainActivityTransforms.SWIPEBLE) {
                    mToolbar?.setNavigationOnClickListener {
                        openNavigationPage(
                            AbsNavigationView.SECTION_ITEM_FEED,
                            false
                        )
                    }
                } else {
                    mToolbar?.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
                }
            }
        }
    }

    private fun onSetOffline(success: Boolean) {
        if (success) createCustomToast(this).showToast(R.string.succ_offline) else createCustomToast(
            this
        ).showToastError(R.string.err_offline)
    }

    private fun onCurrentAccountChange(newAccountId: Long) {
        mAccountId = newAccountId
        navigationView?.onAccountChange(newAccountId)
        Accounts.showAccountSwitchedToast(this)
        updateNotificationCount(newAccountId)
        if (!Settings.get().main().isDeveloper_mode) {
            stop()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.d(TAG, "onNewIntent, intent: $intent")
        handleIntent(intent, false)
    }

    private fun handleIntent(intent: Intent?, isMain: Boolean): Boolean {
        if (intent == null) {
            return false
        }
        Logger.d(TAG, "handleIntent, extras: ${intent.extras}, action: ${intent.action}")
        when {
            ACTION_OPEN_WALL == intent.action -> {
                val owner_id = intent.extras!!.getLong(Extra.OWNER_ID)
                PlaceFactory.getOwnerWallPlace(mAccountId, owner_id, null).tryOpenWith(this)
                return true
            }

            ACTION_SWITCH_ACCOUNT == intent.action -> {
                val newAccountId = intent.extras!!.getLong(Extra.ACCOUNT_ID)
                if (Settings.get().accounts().current != newAccountId) {
                    if (!Settings.get().accounts().registered.contains(newAccountId)) {
                        createCustomToast(this).showToastError(R.string.account_not_registered)
                    } else {
                        Settings.get()
                            .accounts().current = newAccountId
                        mAccountId = newAccountId
                    }
                }
                intent.action = ACTION_MAIN
            }

            ACTION_SHORTCUT_WALL == intent.action -> {
                var newAccountId = intent.extras!!.getLong(Extra.ACCOUNT_ID)
                val prefsAid = Settings.get()
                    .accounts()
                    .current
                val ownerId = intent.extras!!.getLong(Extra.OWNER_ID)
                if (prefsAid != newAccountId) {
                    if (!Settings.get().accounts().registered.contains(newAccountId)) {
                        newAccountId = prefsAid
                        createCustomToast(this).showToastError(R.string.account_not_registered)
                    } else {
                        Settings.get()
                            .accounts().current = newAccountId
                        mAccountId = newAccountId
                    }
                }
                clearBackStack()
                openPlace(PlaceFactory.getOwnerWallPlace(newAccountId, ownerId, null))
                return true
            }

            ACTION_CHAT_FROM_SHORTCUT == intent.action -> {
                var aid = intent.extras!!.getLong(Extra.ACCOUNT_ID)
                val prefsAid = Settings.get()
                    .accounts()
                    .current
                if (prefsAid != aid) {
                    if (!Settings.get().accounts().registered.contains(aid)) {
                        aid = prefsAid
                        createCustomToast(this).showToastError(R.string.account_not_registered)
                    } else {
                        Settings.get()
                            .accounts().current = aid
                    }
                }
                val peerId = intent.extras!!.getLong(Extra.PEER_ID)
                val title = intent.getStringExtra(Extra.TITLE)
                val imgUrl = intent.getStringExtra(Extra.IMAGE)
                val peer = Peer(peerId).setTitle(title).setAvaUrl(imgUrl)
                PlaceFactory.getChatPlace(aid, aid, peer).tryOpenWith(this)
                return Settings.get()
                    .ui().swipes_chat_mode != SwipesChatMode.SLIDR || Settings.get()
                    .ui().swipes_chat_mode == SwipesChatMode.DISABLED
            }

            Intent.ACTION_SEND_MULTIPLE == intent.action -> {
                val mime = intent.type
                if (mainActivityTransform == MainActivityTransforms.MAIN && intent.extras != null && mime.nonNullNoEmpty() && isMimeAudio(
                        mime
                    ) && intent.extras?.containsKey(Intent.EXTRA_STREAM) == true
                ) {
                    val uris = intent.getParcelableArrayListExtraCompat<Uri>(Intent.EXTRA_STREAM)
                    if (uris.nonNullNoEmpty()) {
                        val playlist = ArrayList<Audio>(
                            uris.size
                        )
                        for (i in uris) {
                            val track = UploadUtils.findFileName(this, i) ?: return false
                            var TrackName = track.replace(".mp3", "")
                            var Artist = ""
                            val arr = TrackName.split(Regex(" - ")).toTypedArray()
                            if (arr.size > 1) {
                                Artist = arr[0]
                                TrackName = TrackName.replace("$Artist - ", "")
                            }
                            val tmp = Audio().setIsLocal().setThumb_image_big(
                                "share_$i"
                            ).setThumb_image_little("share_$i").setUrl(i.toString())
                                .setOwnerId(mAccountId).setArtist(Artist).setTitle(TrackName)
                                .setId(i.toString().hashCode())
                            playlist.add(tmp)
                        }
                        intent.removeExtra(Intent.EXTRA_STREAM)
                        startForPlayList(this, playlist, 0, false)
                        PlaceFactory.getPlayerPlace(mAccountId).tryOpenWith(this)
                    }
                }
            }

            ACTION_SEND_ATTACHMENTS == intent.action -> {
                mCurrentFrontSection = AbsNavigationView.SECTION_ITEM_DIALOGS
                openNavigationPage(AbsNavigationView.SECTION_ITEM_DIALOGS, false)
                return true
            }

            ACTION_OPEN_PLACE == intent.action -> {
                val place: Place = intent.getParcelableExtraCompat(Extra.PLACE) ?: return false
                openPlace(place)
                return if (place.type == Place.CHAT) {
                    Settings.get().ui().swipes_chat_mode != SwipesChatMode.SLIDR || Settings.get()
                        .ui().swipes_chat_mode == SwipesChatMode.DISABLED
                } else true
            }

            ACTION_OPEN_AUDIO_PLAYER == intent.action -> {
                openPlace(PlaceFactory.getPlayerPlace(mAccountId))
                return false
            }

            Intent.ACTION_VIEW == intent.action -> {
                val data = intent.data
                val mime = intent.type ?: ""
                if (mainActivityTransform == MainActivityTransforms.MAIN && mime.nonNullNoEmpty() && isMimeAudio(
                        mime
                    )
                ) {
                    val track = UploadUtils.findFileName(this, data) ?: return false
                    var TrackName = track.replace(".mp3", "")
                    var Artist = ""
                    val arr = TrackName.split(Regex(" - ")).toTypedArray()
                    if (arr.size > 1) {
                        Artist = arr[0]
                        TrackName = TrackName.replace("$Artist - ", "")
                    }
                    val tmp =
                        Audio().setIsLocal().setThumb_image_big("share_$data")
                            .setThumb_image_little(
                                "share_$data"
                            ).setUrl(data.toString()).setOwnerId(mAccountId).setArtist(Artist)
                            .setTitle(TrackName).setId(data.toString().hashCode())
                    startForPlayList(this, ArrayList(listOf(tmp)), 0, false)
                    PlaceFactory.getPlayerPlace(mAccountId).tryOpenWith(this)
                    return false
                }
                LinkHelper.openUrl(this, mAccountId, data.toString(), isMain)
                return true
            }

            intent.extras != null && checkInputExist(this) -> {
                mCurrentFrontSection = AbsNavigationView.SECTION_ITEM_DIALOGS
                openNavigationPage(AbsNavigationView.SECTION_ITEM_DIALOGS, false)
                return true
            }
        }
        return false
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        mToolbar?.setNavigationOnClickListener(null)
        mToolbar?.setOnMenuItemClickListener(null)
        super.setSupportActionBar(toolbar)
        mToolbar = toolbar
        resolveToolbarNavigationIcon()
    }

    private fun openChat(accountId: Long, messagesOwnerId: Long, peer: Peer, closeMain: Boolean) {
        if (Settings.get().main().isEnable_show_recent_dialogs) {
            val recentChat = RecentChat(accountId, peer.id, peer.getTitle(), peer.avaUrl)
            navigationView?.appendRecentChat(recentChat)
            navigationView?.refreshNavigationItems()
            navigationView?.selectPage(recentChat)
        }
        if (Settings.get().ui().swipes_chat_mode == SwipesChatMode.DISABLED) {
            val chatFragment = newInstance(accountId, messagesOwnerId, peer)
            attachToFront(chatFragment)
        } else {
            if (Settings.get()
                    .ui().swipes_chat_mode == SwipesChatMode.SLIDR && mainActivityTransform == MainActivityTransforms.MAIN
            ) {
                val intent = Intent(this, ChatActivity::class.java)
                intent.action = ChatActivity.ACTION_OPEN_PLACE
                intent.putExtra(
                    Extra.PLACE,
                    PlaceFactory.getChatPlace(accountId, messagesOwnerId, peer)
                )
                startActivity(intent)
                if (closeMain) {
                    Utils.finishActivityImmediate(this)
                }
            } else if (Settings.get()
                    .ui().swipes_chat_mode == SwipesChatMode.SLIDR && mainActivityTransform != MainActivityTransforms.MAIN
            ) {
                val chatFragment = newInstance(accountId, messagesOwnerId, peer)
                attachToFront(chatFragment)
            } else {
                throw UnsupportedOperationException()
            }
        }
    }

    private fun openRecentChat(chat: RecentChat) {
        val accountId = mAccountId
        val messagesOwnerId = mAccountId
        openChat(
            accountId,
            messagesOwnerId,
            Peer(chat.peerId).setAvaUrl(chat.iconUrl).setTitle(chat.title),
            false
        )
    }

    internal fun openTargetPage() {
        if (mTargetPage == null) {
            return
        }
        val item = mTargetPage?.first ?: return
        val clearBackStack = mTargetPage?.second == true
        if (item == mCurrentFrontSection) {
            return
        }
        if (item.type == AbsMenuItem.TYPE_ICON) {
            openNavigationPage(item, clearBackStack, true)
        }
        if (item.type == AbsMenuItem.TYPE_RECENT_CHAT) {
            openRecentChat(item as RecentChat)
        }
        mTargetPage = null
    }

    internal val navigationView: AbsNavigationView?
        get() {
            return findViewById(R.id.additional_navigation_menu)
        }

    internal fun openNavigationPage(item: AbsMenuItem, menu: Boolean) {
        openNavigationPage(item, true, menu)
    }

    private fun startAccountsActivity() {
        val intent = Intent(this, AccountsActivity::class.java)
        requestLogin.launch(intent)
    }

    private fun startAccountsActivityZero() {
        val intent = Intent(this, AccountsActivity::class.java)
        requestLoginZero.launch(intent)
    }

    private fun clearBackStack() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        Logger.d(TAG, "Back stack was cleared")
    }

    private fun openNavigationPage(item: AbsMenuItem, clearBackStack: Boolean, menu: Boolean) {
        var doClearBackStack = clearBackStack
        if (item.type == AbsMenuItem.TYPE_RECENT_CHAT) {
            openRecentChat(item as RecentChat)
            return
        }
        val sectionDrawerItem = item as SectionMenuItem
        if (sectionDrawerItem.section == AbsNavigationView.PAGE_ACCOUNTS) {
            startAccountsActivity()
            return
        }
        mCurrentFrontSection = item
        navigationView?.selectPage(item)
        if (Settings.get().main().isDo_not_clear_back_stack && menu && isPlaying) {
            doClearBackStack = !doClearBackStack
        }
        if (doClearBackStack) {
            clearBackStack()
        }
        val aid = mAccountId
        when (sectionDrawerItem.section) {
            AbsNavigationView.PAGE_DIALOGS -> openPlace(
                PlaceFactory.getDialogsPlace(
                    aid,
                    aid,
                    null
                )
            )

            AbsNavigationView.PAGE_FRIENDS -> openPlace(
                PlaceFactory.getFriendsFollowersPlace(
                    aid,
                    aid,
                    FriendsTabsFragment.TAB_ALL_FRIENDS,
                    null
                )
            )

            AbsNavigationView.PAGE_GROUPS -> openPlace(
                PlaceFactory.getCommunitiesPlace(
                    aid,
                    aid
                )
            )

            AbsNavigationView.PAGE_PREFERENSES -> openPlace(PlaceFactory.getPreferencesPlace(aid))
            AbsNavigationView.PAGE_MUSIC -> openPlace(PlaceFactory.getAudiosPlace(aid, aid))
            AbsNavigationView.PAGE_DOCUMENTS -> openPlace(
                PlaceFactory.getDocumentsPlace(
                    aid,
                    aid,
                    DocsListPresenter.ACTION_SHOW
                )
            )

            AbsNavigationView.PAGE_FEED -> openPlace(PlaceFactory.getFeedPlace(aid))
            AbsNavigationView.PAGE_NOTIFICATION -> openPlace(
                PlaceFactory.getNotificationsPlace(
                    aid
                )
            )

            AbsNavigationView.PAGE_PHOTOS -> openPlace(
                PlaceFactory.getVKPhotoAlbumsPlace(
                    aid,
                    aid,
                    IVKPhotosView.ACTION_SHOW_PHOTOS,
                    null
                )
            )

            AbsNavigationView.PAGE_VIDEOS -> openPlace(
                PlaceFactory.getVideosPlace(
                    aid,
                    aid,
                    IVideosListView.ACTION_SHOW
                )
            )

            AbsNavigationView.PAGE_BOOKMARKS -> openPlace(
                PlaceFactory.getBookmarksPlace(
                    aid,
                    FaveTabsFragment.TAB_PAGES
                )
            )

            AbsNavigationView.PAGE_SEARCH -> openPlace(
                PlaceFactory.getSearchPlace(
                    aid,
                    SearchTabsFragment.TAB_PEOPLE
                )
            )

            AbsNavigationView.PAGE_NEWSFEED_COMMENTS -> openPlace(
                PlaceFactory.getNewsfeedCommentsPlace(
                    aid
                )
            )

            else -> throw IllegalArgumentException("Unknown place!!! $item")
        }
    }

    override fun onSheetItemSelected(item: AbsMenuItem, longClick: Boolean) {
        if (mCurrentFrontSection != null && mCurrentFrontSection == item) {
            return
        }
        mTargetPage = create(item, !longClick)
        //после закрытия бокового меню откроется данная страница
    }

    override fun onSheetClosed() {
        postResume(object : Action<MainActivity> {
            override fun call(target: MainActivity) {
                target.openTargetPage()
            }
        })
    }

    override fun onDestroy() {
        mCompositeJob.cancel()
        supportFragmentManager.removeOnBackStackChangedListener(mOnBackStackChangedListener)

        unbindFromAudioPlayService()
        super.onDestroy()
    }

    private fun unbindFromAudioPlayService() {
        if (mAudioPlayServiceToken != null) {
            if (isChangingConfigurations) {
                MusicPlaybackController.doNotDestroyWhenActivityRecreated()
            }
            unbindFromService(mAudioPlayServiceToken)
            mAudioPlayServiceToken = null
        }
    }

    private val isAuthValid: Boolean
        get() = mAccountId != ISettings.IAccountsSettings.INVALID_ID

    /*
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){
        SwipeTouchListener.getGestureDetector().onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }
     */
    fun keyboardHide() {
        try {
            val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
            inputManager?.hideSoftInputFromWindow(
                window.decorView.rootView.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        } catch (_: Exception) {
        }
    }

    internal val frontFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(mainContainerViewId)

    internal val isChatFragment: Boolean
        get() = frontFragment is ChatFragment
    internal val isFragmentWithoutNavigation: Boolean
        get() = frontFragment is CommentsFragment ||
                frontFragment is PostCreateFragment

    override fun onNavigateUp(): Boolean {
        supportFragmentManager.popBackStack()
        return true
    }

    /* Убрать выделение в боковом меню */
    private fun resetNavigationSelection() {
        mCurrentFrontSection = null
        navigationView?.selectPage(null)
    }

    override fun onSectionResume(sectionDrawerItem: SectionMenuItem) {
        navigationView?.selectPage(sectionDrawerItem)
        if (mBottomNavigation != null) {
            when (sectionDrawerItem.section) {
                AbsNavigationView.PAGE_FEED -> mBottomNavigation?.menu?.get(0)?.isChecked =
                    true

                AbsNavigationView.PAGE_SEARCH -> mBottomNavigation?.menu?.get(1)?.isChecked =
                    true

                AbsNavigationView.PAGE_DIALOGS -> mBottomNavigation?.menu?.get(2)?.isChecked =
                    true

                AbsNavigationView.PAGE_NOTIFICATION -> mBottomNavigation?.menu?.get(3)?.isChecked =
                    true

                else -> mBottomNavigation?.menu?.get(4)?.isChecked = true
            }
        }
        mCurrentFrontSection = sectionDrawerItem
    }

    override fun onChatResume(accountId: Long, peerId: Long, title: String?, imgUrl: String?) {
        if (Settings.get().main().isEnable_show_recent_dialogs) {
            val recentChat = RecentChat(accountId, peerId, title, imgUrl)
            navigationView?.appendRecentChat(recentChat)
            navigationView?.refreshNavigationItems()
            navigationView?.selectPage(recentChat)
            mCurrentFrontSection = recentChat
        }
    }

    override fun onClearSelection() {
        resetNavigationSelection()
        mCurrentFrontSection = null
    }

    override fun readAllNotifications() {
        if (Utils.isHiddenAccount(mAccountId)) return
        mCompositeJob.add(
            InteractorFactory.createFeedbackInteractor()
                .markAsViewed(mAccountId)
                .delayedFlow(1000)
                .fromIOToMain {
                    if (it) {
                        mBottomNavigation?.removeBadge(R.id.menu_feedback)
                        navigationView?.onUnreadNotificationsCountChange(0)
                    }
                }
        )
    }

    private fun attachToFront(fragment: Fragment, animate: Boolean = true) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (animate) fragmentTransaction.setCustomAnimations(
            R.anim.fragment_enter,
            R.anim.fragment_exit
        )
        fragmentTransaction
            .replace(mainContainerViewId, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    override fun setStatusbarColored(colored: Boolean, invertIcons: Boolean) {
        val w = window
        @Suppress("deprecation")
        if (!hasVanillaIceCreamTarget()) {
            w.statusBarColor =
                if (colored) getStatusBarColor(this) else getStatusBarNonColored(
                    this
                )
            w.navigationBarColor =
                if (colored) getNavigationBarColor(this) else Color.BLACK
        }
        val ins = WindowInsetsControllerCompat(w, w.decorView)
        ins.isAppearanceLightStatusBars = invertIcons
        ins.isAppearanceLightNavigationBars = invertIcons
    }

    override fun hideMenu(hide: Boolean) {
        if (hide) {
            navigationView?.closeSheet()
            navigationView?.blockSheet()
            mBottomNavigationContainer?.visibility = View.GONE
            if (Settings.get().main().is_side_navigation) {
                findViewById<MaterialCardView>(R.id.miniplayer_side_root)?.visibility = View.GONE
            }
        } else {
            mBottomNavigationContainer?.visibility = View.VISIBLE
            if (Settings.get().main().is_side_navigation) {
                findViewById<MaterialCardView>(R.id.miniplayer_side_root)?.visibility = View.VISIBLE
            }
            navigationView?.unblockSheet()
        }
    }

    override fun openMenu(open: Boolean) {
//        if (open) {
//            getNavigationFragment().openSheet();
//        } else {
//            getNavigationFragment().closeSheet();
//        }
    }

    override fun openPlace(place: Place) {
        val args = place.safeArguments()
        when (place.type) {
            Place.VIDEO_PREVIEW -> attachToFront(VideoPreviewFragment.newInstance(args))
            Place.STORY_PLAYER -> place.launchActivityForResult(
                this,
                StoryPagerActivity.newInstance(this, args)
            )

            Place.SHORT_VIDEOS -> place.launchActivityForResult(
                this,
                ShortVideoPagerActivity.newInstance(this, args)
            )

            Place.FRIENDS_AND_FOLLOWERS -> attachToFront(FriendsTabsFragment.newInstance(args))
            Place.EXTERNAL_LINK -> attachToFront(BrowserFragment.newInstance(args))
            Place.DOC_PREVIEW -> {
                val document: Document? = args.getParcelableCompat(Extra.DOC)
                if (document != null && document.hasValidGifVideoLink()) {
                    val aid = args.getLong(Extra.ACCOUNT_ID)
                    val documents = ArrayList(listOf(document))
                    val extra = GifPagerActivity.buildArgs(aid, documents, 0)
                    place.launchActivityForResult(this, GifPagerActivity.newInstance(this, extra))
                } else {
                    attachToFront(DocPreviewFragment.newInstance(args))
                }
            }

            Place.WALL_POST -> attachToFront(WallPostFragment.newInstance(args))
            Place.COMMENTS -> attachToFront(CommentsFragment.newInstance(place))
            Place.WALL -> attachToFront(AbsWallFragment.newInstance(args))
            Place.CONVERSATION_ATTACHMENTS -> attachToFront(
                ConversationFragmentFactory.newInstance(
                    args
                )
            )

            Place.PLAYER -> {
                val player = supportFragmentManager.findFragmentByTag("audio_player")
                if (player is AudioPlayerFragment) player.dismiss()
                AudioPlayerFragment.newInstance(args).show(supportFragmentManager, "audio_player")
            }

            Place.CHAT -> {
                val peer: Peer = args.getParcelableCompat(Extra.PEER) ?: return
                openChat(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.OWNER_ID),
                    peer,
                    place.isNeedFinishMain
                )
            }

            Place.SEARCH -> attachToFront(SearchTabsFragment.newInstance(args))
            Place.AUDIOS_SEARCH_TABS -> attachToFront(AudioSearchTabsFragment.newInstance(args))
            Place.GROUP_CHATS -> attachToFront(GroupChatsFragment.newInstance(args))
            Place.BUILD_NEW_POST -> {
                val postCreateFragment = PostCreateFragment.newInstance(args)
                attachToFront(postCreateFragment)
            }

            Place.EDIT_COMMENT -> {
                val comment: Comment? = args.getParcelableCompat(Extra.COMMENT)
                val accountId = args.getLong(Extra.ACCOUNT_ID)
                val commentId = args.getInt(Extra.COMMENT_ID)
                attachToFront(CommentEditFragment.newInstance(accountId, comment, commentId))
            }

            Place.EDIT_POST -> {
                val postEditFragment = PostEditFragment.newInstance(args)
                attachToFront(postEditFragment)
            }

            Place.REPOST -> {
                val repostFragment = RepostFragment.newInstance(args)
                attachToFront(repostFragment)
            }

            Place.DIALOGS -> attachToFront(
                DialogsFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.OWNER_ID),
                    args.getString(Extra.SUBTITLE)
                )
            )

            Place.FORWARD_MESSAGES -> attachToFront(FwdsFragment.newInstance(args))
            Place.TOPICS -> attachToFront(TopicsFragment.newInstance(args))
            Place.CHAT_MEMBERS -> attachToFront(ChatMembersFragment.newInstance(args))
            Place.FEED_BAN -> attachToFront(FeedBannedFragment.newInstance(args))
            Place.REMOTE_FILE_MANAGER -> attachToFront(FileManagerRemoteFragment())
            Place.COMMUNITIES -> {
                val communitiesFragment = CommunitiesFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.USER_ID)
                )
                attachToFront(communitiesFragment)
            }

            Place.AUDIOS -> attachToFront(
                if (Settings.get().main().isAudio_catalog_v2) CatalogV2ListFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.OWNER_ID)
                ) else AudiosTabsFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.OWNER_ID)
                )
            )

            Place.MENTIONS -> attachToFront(
                NewsfeedMentionsFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID), args.getLong(
                        Extra.OWNER_ID
                    )
                )
            )

            Place.CATALOG_V2_AUDIO_SECTION -> attachToFront(
                CatalogV2SectionFragment.newInstance(
                    args
                )
            )

            Place.CATALOG_V2_AUDIO_CATALOG -> attachToFront(CatalogV2ListFragment.newInstance(args))
            Place.AUDIOS_IN_ALBUM -> attachToFront(AudiosFragment.newInstance(args))
            Place.SEARCH_BY_AUDIO -> attachToFront(
                AudiosRecommendationFragment.newInstance(
                    args.getLong(
                        Extra.ACCOUNT_ID
                    ), args.getLong(Extra.OWNER_ID), false, args.getInt(Extra.ID)
                )
            )

            Place.LOCAL_SERVER_PHOTO -> attachToFront(
                PhotosLocalServerFragment.newInstance(
                    args.getLong(
                        Extra.ACCOUNT_ID
                    )
                )
            )

            Place.VIDEO_ALBUM -> attachToFront(VideosFragment.newInstance(args))
            Place.VIDEOS -> attachToFront(VideosTabsFragment.newInstance(args))
            Place.VK_PHOTO_ALBUMS -> attachToFront(
                VKPhotoAlbumsFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.OWNER_ID),
                    args.getString(Extra.ACTION),
                    args.getParcelableCompat(Extra.OWNER), false
                )
            )

            Place.VK_PHOTO_ALBUM -> attachToFront(VKPhotosFragment.newInstance(args))
            Place.VK_PHOTO_ALBUM_GALLERY, Place.FAVE_PHOTOS_GALLERY, Place.SIMPLE_PHOTO_GALLERY, Place.SIMPLE_PHOTO_GALLERY_NATIVE, Place.VK_PHOTO_TMP_SOURCE, Place.VK_PHOTO_ALBUM_GALLERY_SAVED, Place.VK_PHOTO_ALBUM_GALLERY_NATIVE -> newInstance(
                this,
                place.type,
                args
            )?.let {
                place.launchActivityForResult(
                    this,
                    it
                )
            }

            Place.SINGLE_PHOTO -> place.launchActivityForResult(
                this,
                SinglePhotoActivity.newInstance(this, args)
            )

            Place.GIF_PAGER -> place.launchActivityForResult(
                this,
                GifPagerActivity.newInstance(this, args)
            )

            Place.POLL -> attachToFront(PollFragment.newInstance(args))
            Place.BOOKMARKS -> attachToFront(FaveTabsFragment.newInstance(args))
            Place.DOCS -> attachToFront(DocsFragment.newInstance(args))
            Place.FEED -> attachToFront(FeedFragment.newInstance(args))
            Place.NOTIFICATIONS -> {
                if (Utils.isOfficialVKAccount(mAccountId)) {
                    attachToFront(
                        FeedbackVKOfficialFragment.newInstance(
                            Settings.get().accounts().current
                        )
                    )
                } else {
                    attachToFront(FeedbackFragment.newInstance(args))
                }
            }

            Place.PREFERENCES -> attachToFront(PreferencesFragment.newInstance(args))
            Place.RESOLVE_DOMAIN -> {
                val domainDialog = ResolveDomainDialog.newInstance(args)
                domainDialog.show(supportFragmentManager, "resolve-domain")
            }

            Place.VK_INTERNAL_PLAYER -> {
                val videoActivity = VideoPlayerActivity.newInstance(this, args)
                place.launchActivityForResult(
                    this,
                    videoActivity
                )
            }

            Place.LIKES_AND_COPIES -> attachToFront(LikesFragment.newInstance(args))
            Place.STORIES_VIEWS -> attachToFront(StoriesViewFragment.newInstance(args))
            Place.CREATE_PHOTO_ALBUM, Place.EDIT_PHOTO_ALBUM -> {
                val createPhotoAlbumFragment = CreatePhotoAlbumFragment.newInstance(args)
                attachToFront(createPhotoAlbumFragment)
            }

            Place.MESSAGE_LOOKUP -> attachToFront(MessagesLookFragment.newInstance(args))
            Place.SEARCH_COMMENTS -> attachToFront(
                WallSearchCommentsAttachmentsFragment.newInstance(
                    args
                )
            )

            Place.COMMUNITY_MEMBERS -> attachToFront(
                CommunityMembersFragment.newInstance(args)
            )

            Place.UNREAD_MESSAGES -> attachToFront(NotReadMessagesFragment.newInstance(args))
            Place.SECURITY -> attachToFront(SecurityPreferencesFragment())
            Place.CREATE_POLL -> {
                attachToFront(CreatePollFragment.newInstance(args))
            }

            Place.COMMENT_CREATE -> openCommentCreatePlace(place)
            Place.LOGS -> attachToFront(LogsFragment.newInstance())
            Place.SINGLE_SEARCH -> {
                val singleTabSearchFragment = SingleTabSearchFragment.newInstance(args)
                attachToFront(singleTabSearchFragment)
            }

            Place.NEWSFEED_COMMENTS -> {
                val newsfeedCommentsFragment = NewsfeedCommentsFragment.newInstance(
                    args.getLong(
                        Extra.ACCOUNT_ID
                    )
                )
                attachToFront(newsfeedCommentsFragment)
            }

            Place.COMMUNITY_CONTROL -> {
                val communityControlFragment = CommunityControlFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getParcelableCompat(Extra.OWNER),
                    args.getParcelableCompat(Extra.SETTINGS)
                )
                attachToFront(communityControlFragment)
            }

            Place.COMMUNITY_INFO -> {
                val communityInfoFragment = CommunityInfoContactsFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getParcelableCompat(Extra.OWNER)
                )
                attachToFront(communityInfoFragment)
            }

            Place.COMMUNITY_INFO_LINKS -> {
                val communityLinksFragment = CommunityInfoLinksFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getParcelableCompat(Extra.OWNER)
                )
                attachToFront(communityLinksFragment)
            }

            Place.SETTINGS_THEME -> {
                val themes = ThemeFragment()
                attachToFront(themes)
                if (navigationView?.isSheetOpen == true) {
                    navigationView?.closeSheet()
                    return
                }
            }

            Place.COMMUNITY_BAN_EDIT -> {
                val communityBanEditFragment = CommunityBanEditFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.GROUP_ID),
                    args.getParcelableCompat<Banned>(Extra.BANNED)
                )
                attachToFront(communityBanEditFragment)
            }

            Place.COMMUNITY_ADD_BAN -> attachToFront(
                CommunityBanEditFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.GROUP_ID),
                    args.getParcelableArrayListCompat(Extra.USERS)
                )
            )

            Place.COMMUNITY_MANAGER_ADD -> attachToFront(
                CommunityManagerEditFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.GROUP_ID),
                    args.getParcelableArrayListCompat(Extra.USERS)
                )
            )

            Place.COMMUNITY_MANAGER_EDIT -> attachToFront(
                CommunityManagerEditFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.GROUP_ID),
                    args.getParcelableCompat<Manager>(Extra.MANAGER)
                )
            )

            Place.REQUEST_EXECUTOR -> attachToFront(
                RequestExecuteFragment.newInstance(
                    args.getLong(
                        Extra.ACCOUNT_ID
                    )
                )
            )

            Place.USER_BLACKLIST -> attachToFront(UserBannedFragment.newInstance(args.getLong(Extra.ACCOUNT_ID)))
            Place.FRIENDS_BIRTHDAYS -> attachToFront(BirthDayFragment.newInstance(args))
            Place.DRAWER_EDIT -> attachToFront(DrawerEditFragment.newInstance())
            Place.SIDE_DRAWER_EDIT -> attachToFront(SideDrawerEditFragment.newInstance())
            Place.CATALOG_V2_LIST_EDIT -> attachToFront(CatalogV2ListEditFragment.newInstance())
            Place.ARTIST -> attachToFront(AudiosByArtistFragment.newInstance(args))
            Place.SHORT_LINKS -> attachToFront(ShortedLinksFragment.newInstance(args.getLong(Extra.ACCOUNT_ID)))
            Place.SHORTCUTS -> attachToFront(ShortcutsViewFragment())
            Place.AUTH_BY_CODE -> attachToFront(ProcessAuthCodeFragment.newInstance(args))
            Place.IMPORTANT_MESSAGES -> attachToFront(
                ImportantMessagesFragment.newInstance(
                    args.getLong(
                        Extra.ACCOUNT_ID
                    )
                )
            )

            Place.OWNER_ARTICLES -> attachToFront(
                OwnerArticlesFragment.newInstance(
                    args.getLong(
                        Extra.ACCOUNT_ID
                    ), args.getLong(Extra.OWNER_ID)
                )
            )

            Place.USER_DETAILS -> {
                val accountId = args.getLong(Extra.ACCOUNT_ID)
                val user: User = args.getParcelableCompat(Extra.USER) ?: return
                val details: UserDetails = args.getParcelableCompat("details") ?: return
                attachToFront(newInstance(accountId, user, details))
            }

            Place.WALL_ATTACHMENTS -> {
                val wall_attachments = WallAttachmentsFragmentFactory.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.OWNER_ID),
                    args.getString(Extra.TYPE)
                )
                    ?: throw IllegalArgumentException("wall_attachments cant bee null")
                attachToFront(wall_attachments)
            }

            Place.MARKET_ALBUMS -> attachToFront(
                ProductAlbumsFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.OWNER_ID)
                )
            )

            Place.NARRATIVES -> attachToFront(
                NarrativesFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.OWNER_ID)
                )
            )

            Place.MARKETS -> attachToFront(
                ProductsFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.OWNER_ID),
                    args.getInt(Extra.ALBUM_ID),
                    args.getBoolean(Extra.SERVICE)
                )
            )

            Place.PHOTO_ALL_COMMENT -> attachToFront(
                PhotoAllCommentFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.OWNER_ID)
                )
            )

            Place.GIFTS -> attachToFront(
                GiftsFragment.newInstance(
                    args.getLong(Extra.ACCOUNT_ID),
                    args.getLong(Extra.OWNER_ID)
                )
            )

            Place.MARKET_VIEW -> attachToFront(MarketViewFragment.newInstance(args))
            Place.ALBUMS_BY_VIDEO -> attachToFront(VideoAlbumsByVideoFragment.newInstance(args))
            Place.FRIENDS_BY_PHONES -> attachToFront(FriendsByPhonesFragment.newInstance(args))
            Place.VOTERS -> attachToFront(VotersFragment.newInstance(args))
            else -> throw IllegalArgumentException("Main activity can't open this place, type: " + place.type)
        }
    }

    private fun openCommentCreatePlace(place: Place) {
        val args = place.safeArguments()
        val fragment = CommentCreateFragment.newInstance(
            args.getLong(Extra.ACCOUNT_ID),
            args.getInt(Extra.COMMENT_ID),
            args.getLong(Extra.OWNER_ID),
            args.getString(Extra.BODY)
        )
        attachToFront(fragment)
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        if (name.className == MusicPlaybackService::class.java.name) {
            Logger.d(TAG, "Connected to MusicPlaybackService")
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        if (mAudioPlayServiceToken == null) return
        if (name.className == MusicPlaybackService::class.java.name) {
            Logger.d(TAG, "Disconnected from MusicPlaybackService")
            mAudioPlayServiceToken = null
            bindToAudioPlayService()
        }
    }

    private fun openPageAndCloseSheet(item: AbsMenuItem) {
        if (navigationView?.isSheetOpen == true) {
            navigationView?.closeSheet()
            onSheetItemSelected(item, false)
        } else {
            openNavigationPage(item, true)
        }
    }

    private fun updateMessagesBagde(count: Int) {
        navigationView?.onUnreadDialogsCountChange(count)
        if (mBottomNavigation != null) {
            if (count > 0) {
                val badgeDrawable = mBottomNavigation?.getOrCreateBadge(R.id.menu_messages)
                badgeDrawable?.isBadgeNotSaveColor = true
                badgeDrawable?.number = count
            } else {
                mBottomNavigation?.removeBadge(R.id.menu_messages)
            }
        }
    }

    private fun updateNotificationsBadge(counters: SectionCounters) {
        navigationView?.onUnreadDialogsCountChange(counters.messages)
        navigationView?.onUnreadNotificationsCountChange(counters.notifications)
        if (mBottomNavigation != null) {
            if (counters.notifications > 0) {
                val badgeDrawable = mBottomNavigation?.getOrCreateBadge(R.id.menu_feedback)
                badgeDrawable?.isBadgeNotSaveColor = true
                badgeDrawable?.number = counters.notifications
            } else {
                mBottomNavigation?.removeBadge(R.id.menu_feedback)
            }
            if (counters.messages > 0) {
                val badgeDrawable = mBottomNavigation?.getOrCreateBadge(R.id.menu_messages)
                badgeDrawable?.isBadgeNotSaveColor = true
                badgeDrawable?.number = counters.messages
            } else {
                mBottomNavigation?.removeBadge(R.id.menu_messages)
            }
        }
    }

    private fun removeNotificationsBadge() {
        navigationView?.onUnreadDialogsCountChange(0)
        navigationView?.onUnreadNotificationsCountChange(0)
        mBottomNavigation?.removeBadge(R.id.menu_feedback)
        mBottomNavigation?.removeBadge(R.id.menu_messages)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_feed -> {
                openPageAndCloseSheet(AbsNavigationView.SECTION_ITEM_FEED)
                return true
            }

            R.id.menu_search -> {
                openPageAndCloseSheet(AbsNavigationView.SECTION_ITEM_SEARCH)
                return true
            }

            R.id.menu_messages -> {
                openPageAndCloseSheet(AbsNavigationView.SECTION_ITEM_DIALOGS)
                return true
            }

            R.id.menu_feedback -> {
                openPageAndCloseSheet(AbsNavigationView.SECTION_ITEM_FEEDBACK)
                return true
            }

            R.id.menu_other -> {
                if (navigationView?.isSheetOpen == true) {
                    navigationView?.closeSheet()
                } else {
                    navigationView?.openSheet()
                }
                return true
            }

            else -> return false
        }
    }

    override fun onUpdateNavigation() {
        resolveToolbarNavigationIcon()
    }

    protected val DOUBLE_BACK_PRESSED_TIMEOUT = 2000

    companion object {
        const val ACTION_MAIN = "android.intent.action.MAIN"
        const val ACTION_OPEN_PLACE = "dev.ragnarok.fenrir.activity.MainActivity.openPlace"
        const val ACTION_OPEN_AUDIO_PLAYER =
            "dev.ragnarok.fenrir.activity.MainActivity.openAudioPlayer"
        const val ACTION_SEND_ATTACHMENTS = "dev.ragnarok.fenrir.ACTION_SEND_ATTACHMENTS"

        const val ACTION_SWITCH_ACCOUNT = "dev.ragnarok.fenrir.ACTION_SWITCH_ACCOUNT"
        const val ACTION_SHORTCUT_WALL = "dev.ragnarok.fenrir.ACTION_SHORTCUT_WALL"
        const val ACTION_OPEN_WALL = "dev.ragnarok.fenrir.ACTION_OPEN_WALL"
        const val ACTION_CHAT_FROM_SHORTCUT = "dev.ragnarok.fenrir.ACTION_CHAT_FROM_SHORTCUT"

        const val EXTRA_NO_REQUIRE_PIN = "no_require_pin"

        /**
         * Extra with type [dev.ragnarok.fenrir.model.ModelsBundle] only
         */
        const val EXTRA_INPUT_ATTACHMENTS = "input_attachments"
        private const val TAG = "MainActivity_LOG"

        /**
         * Check the device to make sure it has the Google Play Services APK. If
         * it doesn't, display a dialog that allows users to download the APK from
         * the Google Play Store or enable it in the device's system settings.
         */
        fun checkPlayServices(context: Context): Boolean {
            val apiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = apiAvailability.isGooglePlayServicesAvailable(context)
            return resultCode == ConnectionResult.SUCCESS
        }
    }
}
