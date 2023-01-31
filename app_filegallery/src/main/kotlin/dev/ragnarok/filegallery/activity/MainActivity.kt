package dev.ragnarok.filegallery.activity

import android.Manifest
import android.app.Activity
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationBarView
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.filegallery.Extra
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.activity.EnterPinActivity.Companion.getClass
import dev.ragnarok.filegallery.activity.photopager.PhotoPagerActivity
import dev.ragnarok.filegallery.activity.qr.CameraScanActivity
import dev.ragnarok.filegallery.fragment.AudioPlayerFragment
import dev.ragnarok.filegallery.fragment.PreferencesFragment
import dev.ragnarok.filegallery.fragment.SecurityPreferencesFragment
import dev.ragnarok.filegallery.fragment.filemanager.FileManagerFragment
import dev.ragnarok.filegallery.fragment.localserver.LocalServerTabsFragment
import dev.ragnarok.filegallery.fragment.tagdir.TagDirFragment
import dev.ragnarok.filegallery.fragment.tagowner.TagOwnerFragment
import dev.ragnarok.filegallery.fragment.theme.ThemeFragment
import dev.ragnarok.filegallery.fromIOToMain
import dev.ragnarok.filegallery.listener.*
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.media.music.MusicPlaybackController.ServiceToken
import dev.ragnarok.filegallery.media.music.MusicPlaybackService
import dev.ragnarok.filegallery.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.filegallery.modalbottomsheetdialogfragment.Option
import dev.ragnarok.filegallery.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.filegallery.model.SectionItem
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.place.Place
import dev.ragnarok.filegallery.place.PlaceFactory.getFileManagerPlace
import dev.ragnarok.filegallery.place.PlaceFactory.getLocalMediaServerPlace
import dev.ragnarok.filegallery.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.filegallery.place.PlaceFactory.getPreferencesPlace
import dev.ragnarok.filegallery.place.PlaceFactory.getTagsPlace
import dev.ragnarok.filegallery.place.PlaceProvider
import dev.ragnarok.filegallery.settings.CurrentTheme
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.settings.theme.ThemesController.currentStyle
import dev.ragnarok.filegallery.settings.theme.ThemesController.nextRandom
import dev.ragnarok.filegallery.util.AppPerms
import dev.ragnarok.filegallery.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.filegallery.util.AppPerms.requestPermissionsResultAbs
import dev.ragnarok.filegallery.util.HelperSimple.NOTIFICATION_PERMISSION
import dev.ragnarok.filegallery.util.HelperSimple.needHelp
import dev.ragnarok.filegallery.util.Logger
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.ViewUtils.keyboardHide
import dev.ragnarok.filegallery.util.rxutils.RxUtils
import dev.ragnarok.filegallery.util.toast.CustomToast.Companion.createCustomToast
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.io.File

class MainActivity : AppCompatActivity(), OnSectionResumeCallback, AppStyleable, PlaceProvider,
    NavigationBarView.OnItemSelectedListener, UpdatableNavigation, ServiceConnection {
    private var mBottomNavigation: BottomNavigationView? = null
    private var isSelected = false

    @SectionItem
    private var mCurrentFrontSection: Int = SectionItem.NULL
    private var mToolbar: Toolbar? = null
    private var mViewFragment: FragmentContainerView? = null
    private var mLastBackPressedTime: Long = 0
    private val DOUBLE_BACK_PRESSED_TIMEOUT = 2000
    private var mDestroyed = false
    private var mAudioPlayServiceToken: ServiceToken? = null
    private val TAG = "MainActivity_LOG"
    private val mCompositeDisposable = CompositeDisposable()
    private val requestReadWritePermission = requestPermissionsResultAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        ), {
            handleIntent(null, true)
        }, {
            finish()
        })

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requestNPermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.POST_NOTIFICATIONS
        )
    ) {
        createCustomToast(this, mViewFragment)?.showToast(R.string.success)
    }

    private val mOnBackStackChangedListener =
        FragmentManager.OnBackStackChangedListener {
            resolveToolbarNavigationIcon()
            keyboardHide(this)
        }

    private val requestEnterPin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode != RESULT_OK) {
            finish()
        } else {
            handleIntent(intent?.action, true)
        }
    }

    private fun startEnterPinActivity() {
        val intent = Intent(this, getClass(this))
        requestEnterPin.launch(intent)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        delegate.applyDayNight()
        savedInstanceState ?: nextRandom()
        setTheme(currentStyle())
        Utils.prepareDensity(this)
        super.onCreate(savedInstanceState)
        mDestroyed = false

        savedInstanceState ?: run {
            if (Settings.get().security().isUsePinForEntrance && Settings.get().security()
                    .hasPinHash()
            ) {
                startEnterPinActivity()
            } else {
                handleIntent(intent?.action, true)
            }
        }
        bindToAudioPlayService()
        setContentView(noMainContentView)
        mBottomNavigation = findViewById(R.id.bottom_navigation_menu)
        mBottomNavigation?.setOnItemSelectedListener(this)
        mViewFragment = findViewById(R.id.fragment)

        supportFragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener)
        resolveToolbarNavigationIcon()
        savedInstanceState ?: run {
            if (Settings.get().main().getLocalServer().enabled_audio_local_sync) {
                mCompositeDisposable.add(MusicPlaybackController.tracksExist.findAllAudios(
                    this
                )
                    .fromIOToMain()
                    .subscribe(
                        RxUtils.dummy()
                    ) { t: Throwable? ->
                        if (Settings.get().main().isDeveloper_mode()) {
                            createCustomToast(
                                this,
                                mViewFragment,
                                mBottomNavigation
                            )?.showToastThrowable(t)
                        }
                    })
            }
            mCompositeDisposable.add(MusicPlaybackController.tracksExist.findAllTags()
                .fromIOToMain()
                .subscribe(
                    RxUtils.dummy()
                ) { t: Throwable? ->
                    if (Settings.get().main().isDeveloper_mode()) {
                        createCustomToast(this, mViewFragment, mBottomNavigation)
                            ?.showToastThrowable(t)
                    }
                })
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val front: Fragment? = frontFragment
                if (front is BackPressCallback) {
                    if (!(front as BackPressCallback).onBackPressed()) {
                        return
                    }
                }
                if (supportFragmentManager.backStackEntryCount == 1) {
                    if (mLastBackPressedTime < 0
                        || mLastBackPressedTime + DOUBLE_BACK_PRESSED_TIMEOUT > System.currentTimeMillis()
                    ) {
                        supportFinishAfterTransition()
                        return
                    }
                    mLastBackPressedTime = System.currentTimeMillis()
                    mViewFragment?.let {
                        createCustomToast(it.context, mViewFragment, mBottomNavigation)
                            ?.setDuration(Toast.LENGTH_SHORT)
                            ?.showToast(R.string.click_back_to_exit)
                    }
                } else {
                    supportFragmentManager.popBackStack()
                }
            }
        })
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.updateActivityContext(newBase))
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent?.action, false)
    }

    private val requestQRScan = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanner = result.data?.extras?.getString(Extra.URL)
            if (scanner.nonNullNoEmpty()) {
                MaterialAlertDialogBuilder(this)
                    .setIcon(R.drawable.qr_code)
                    .setMessage(scanner)
                    .setTitle(getString(R.string.scan_qr))
                    .setNeutralButton(R.string.button_copy) { _: DialogInterface?, _: Int ->
                        val clipboard = getSystemService(
                            CLIPBOARD_SERVICE
                        ) as ClipboardManager?
                        val clip = ClipData.newPlainText("response", scanner)
                        clipboard?.setPrimaryClip(clip)
                        createCustomToast(this, null)?.showToast(R.string.copied_to_clipboard)
                    }
                    .setCancelable(true)
                    .create().show()
            }
        }
    }

    private fun resolveToolbarNavigationIcon() {
        mToolbar ?: return
        val manager: FragmentManager = supportFragmentManager
        if (manager.backStackEntryCount > 1 || frontFragment is CanBackPressedCallback && (frontFragment as CanBackPressedCallback).canBackPressed()) {
            mToolbar?.setNavigationIcon(R.drawable.arrow_left)
            mToolbar?.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        } else {
            mToolbar?.setNavigationIcon(R.drawable.client_round)
            mToolbar?.setNavigationOnClickListener {
                val menus = ModalBottomSheetDialogFragment.Builder()
                menus.add(
                    OptionRequest(
                        0,
                        getString(R.string.night_mode_title),
                        R.drawable.ic_outline_nights_stay,
                        false
                    )
                )
                menus.add(
                    OptionRequest(
                        1,
                        getString(R.string.scan_qr),
                        R.drawable.qr_code,
                        false
                    )
                )
                menus.show(
                    supportFragmentManager,
                    "left_options",
                    object : ModalBottomSheetDialogFragment.Listener {
                        override fun onModalOptionSelected(option: Option) {
                            when {
                                option.id == 0 -> {
                                    if (Settings.get().main()
                                            .getNightMode() == AppCompatDelegate.MODE_NIGHT_YES || Settings.get()
                                            .main()
                                            .getNightMode() == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY || Settings.get()
                                            .main()
                                            .getNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                    ) {
                                        Settings.get().main()
                                            .switchNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                    } else {
                                        Settings.get().main()
                                            .switchNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                    }
                                }

                                option.id == 1 && FenrirNative.isNativeLoaded -> {
                                    val intent =
                                        Intent(this@MainActivity, CameraScanActivity::class.java)
                                    requestQRScan.launch(intent)
                                }
                            }
                        }
                    })
            }
        }
    }

    @get:LayoutRes
    private val noMainContentView: Int
        get() = R.layout.activity_main
    private val frontFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.fragment)

    override fun onSectionResume(@SectionItem section: Int) {
        mCurrentFrontSection = section
        mBottomNavigation?.menu ?: return
        for (i in (mBottomNavigation?.menu ?: return).iterator()) {
            i.isChecked = false
        }

        when (section) {
            SectionItem.FILE_MANAGER -> {
                mBottomNavigation?.menu?.findItem(R.id.menu_files)?.isChecked = true
            }

            SectionItem.LOCAL_SERVER -> {
                mBottomNavigation?.menu?.findItem(R.id.menu_local_server)?.isChecked = true
            }

            SectionItem.NULL -> {

            }

            SectionItem.SETTINGS -> {
                mBottomNavigation?.menu?.findItem(R.id.menu_settings)?.isChecked = true
            }

            SectionItem.TAGS -> {
                mBottomNavigation?.menu?.findItem(R.id.menu_tags)?.isChecked = true
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun setStatusbarColored(colored: Boolean, invertIcons: Boolean) {
        val statusbarNonColored = CurrentTheme.getStatusBarNonColored(this)
        val statusbarColored = CurrentTheme.getStatusBarColor(this)
        val w = window
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        w.statusBarColor = if (colored) statusbarColored else statusbarNonColored
        @ColorInt val navigationColor =
            if (colored) CurrentTheme.getNavigationBarColor(this) else Color.BLACK
        w.navigationBarColor = navigationColor
        if (Utils.hasMarshmallow()) {
            var flags = window.decorView.systemUiVisibility
            flags = if (invertIcons) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            window.decorView.systemUiVisibility = flags
        }
        if (Utils.hasOreo()) {
            var flags = window.decorView.systemUiVisibility
            if (invertIcons) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                w.decorView.systemUiVisibility = flags
                w.navigationBarColor = Color.WHITE
            } else {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                w.decorView.systemUiVisibility = flags
            }
        }
    }

    @get:IdRes
    private val mainContainerViewId: Int
        get() = R.id.fragment

    private fun handleIntent(action: String?, main: Boolean) {
        if (main) {
            if (Utils.hasTiramisu() && needHelp(
                    NOTIFICATION_PERMISSION,
                    1
                ) && !AppPerms.hasNotificationPermissionSimple(this)
            ) {
                requestNPermission.launch()
            }
            if (!AppPerms.hasReadWriteStoragePermission(this)) {
                requestReadWritePermission.launch()
                return
            }
            if (Intent.ACTION_GET_CONTENT.contentEquals(action, true)) {
                isSelected = true
                openNavigationPage(
                    SectionItem.FILE_MANAGER,
                    clearBackStack = false,
                    isSelect = true
                )
            } else if (Intent.ACTION_PICK.contentEquals(action, true)) {
                isSelected = true
                openNavigationPage(
                    SectionItem.FILE_MANAGER,
                    clearBackStack = false,
                    isSelect = true
                )
            } else {
                isSelected = false
                openNavigationPage(
                    SectionItem.FILE_MANAGER,
                    clearBackStack = false,
                    isSelect = false
                )
            }
        }
        if (ACTION_OPEN_AUDIO_PLAYER == action) {
            openPlace(getPlayerPlace())
        }
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

    override fun openPlace(place: Place) {
        val args: Bundle = place.prepareArguments()
        when (place.type) {
            Place.FILE_MANAGER -> {
                attachToFront(FileManagerFragment.newInstance(args))
            }

            Place.PREFERENCES -> {
                attachToFront(PreferencesFragment())
            }

            Place.LOCAL_MEDIA_SERVER -> {
                attachToFront(LocalServerTabsFragment())
            }

            Place.SETTINGS_THEME -> {
                attachToFront(ThemeFragment())
            }

            Place.AUDIO_PLAYER -> {
                val player = supportFragmentManager.findFragmentByTag("audio_player")
                if (player is AudioPlayerFragment) {
                    player.dismiss()
                }
                AudioPlayerFragment().show(supportFragmentManager, "audio_player")
            }

            Place.PHOTO_LOCAL, Place.PHOTO_LOCAL_SERVER -> {
                PhotoPagerActivity.newInstance(this, place.type, args)?.let {
                    place.launchActivityForResult(
                        this,
                        it
                    )
                }
            }

            Place.VIDEO_PLAYER -> {
                val intent = Intent(this, VideoPlayerActivity::class.java)
                intent.putExtras(args)
                startActivity(intent)
            }

            Place.TAGS -> {
                attachToFront(TagOwnerFragment.newInstance(args))
            }

            Place.TAG_DIRS -> {
                attachToFront(TagDirFragment.newInstance(args))
            }

            Place.SECURITY -> attachToFront(SecurityPreferencesFragment())
        }
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        mToolbar?.setNavigationOnClickListener(null)
        mToolbar?.setOnMenuItemClickListener(null)
        super.setSupportActionBar(toolbar)
        mToolbar = toolbar
        resolveToolbarNavigationIcon()
    }

    override fun onUpdateNavigation() {
        resolveToolbarNavigationIcon()
    }

    private fun bindToAudioPlayService() {
        if (!isActivityDestroyed() && mAudioPlayServiceToken == null) {
            mAudioPlayServiceToken = MusicPlaybackController.bindToServiceWithoutStart(this, this)
        }
    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        mDestroyed = true
        supportFragmentManager.removeOnBackStackChangedListener(mOnBackStackChangedListener)

        //if(!bNoDestroyServiceAudio)
        unbindFromAudioPlayService()
        super.onDestroy()
    }

    private fun unbindFromAudioPlayService() {
        if (mAudioPlayServiceToken != null) {
            MusicPlaybackController.unbindFromService(mAudioPlayServiceToken)
            mAudioPlayServiceToken = null
        }
    }

    private fun isActivityDestroyed(): Boolean {
        return mDestroyed
    }

    private fun clearBackStack() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun openNavigationPage(
        @SectionItem item: Int,
        clearBackStack: Boolean,
        isSelect: Boolean
    ) {
        if (item == mCurrentFrontSection) {
            return
        }
        if (clearBackStack) {
            clearBackStack()
        }
        mCurrentFrontSection = item
        when (item) {
            SectionItem.FILE_MANAGER -> {
                @Suppress("DEPRECATION")
                val path: File =
                    if (Environment.getExternalStorageDirectory().isDirectory && Environment.getExternalStorageDirectory()
                            .canRead()
                    ) {
                        Environment.getExternalStorageDirectory()
                    } else {
                        File("/")
                    }
                openPlace(getFileManagerPlace(path.absolutePath, false, isSelect))
            }

            SectionItem.LOCAL_SERVER -> {
                if (!Settings.get().main().getLocalServer().enabled) {
                    createCustomToast(this, mViewFragment, mBottomNavigation)
                        ?.setDuration(Toast.LENGTH_SHORT)
                        ?.showToastError(R.string.local_server_need_enable)
                    openPlace(getPreferencesPlace())
                } else {
                    openPlace(getLocalMediaServerPlace())
                }
            }

            SectionItem.NULL -> {
                throw UnsupportedOperationException()
            }

            SectionItem.SETTINGS -> {
                openPlace(getPreferencesPlace())
            }

            SectionItem.TAGS -> {
                openPlace(getTagsPlace(isSelect))
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_files -> {
                openNavigationPage(
                    SectionItem.FILE_MANAGER,
                    clearBackStack = true,
                    isSelect = isSelected
                )
                true
            }

            R.id.menu_settings -> {
                openNavigationPage(
                    SectionItem.SETTINGS,
                    clearBackStack = false,
                    isSelect = isSelected
                )
                true
            }

            R.id.menu_local_server -> {
                openNavigationPage(
                    SectionItem.LOCAL_SERVER,
                    clearBackStack = false,
                    isSelect = isSelected
                )
                true
            }

            R.id.menu_tags -> {
                openNavigationPage(SectionItem.TAGS, clearBackStack = false, isSelect = isSelected)
                true
            }

            else -> false
        }
    }

    companion object {
        const val ACTION_OPEN_AUDIO_PLAYER =
            "dev.ragnarok.filegallery.activity.MainActivity.openAudioPlayer"
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (name?.className.equals(MusicPlaybackService::class.java.name)) {
            Logger.d(TAG, "Connected to MusicPlaybackService")
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        if (isActivityDestroyed()) return

        if (name?.className.equals(MusicPlaybackService::class.java.name)) {
            Logger.d(TAG, "Disconnected from MusicPlaybackService")
            mAudioPlayServiceToken = null
            bindToAudioPlayService()
        }
    }
}
