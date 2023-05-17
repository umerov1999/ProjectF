package dev.ragnarok.fenrir.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.squareup.picasso3.BitmapSafeResize.isOverflowCanvas
import com.squareup.picasso3.BitmapSafeResize.setHardwareRendering
import com.squareup.picasso3.BitmapSafeResize.setMaxResolution
import de.maxr1998.modernpreferences.AbsPreferencesFragment
import de.maxr1998.modernpreferences.PreferenceScreen
import de.maxr1998.modernpreferences.PreferencesAdapter
import de.maxr1998.modernpreferences.PreferencesExtra
import de.maxr1998.modernpreferences.helpers.*
import de.maxr1998.modernpreferences.preferences.CustomTextPreference
import de.maxr1998.modernpreferences.preferences.choice.SelectionItem
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.Constants.API_VERSION
import dev.ragnarok.fenrir.Extra.ACCOUNT_ID
import dev.ragnarok.fenrir.Extra.PHOTOS
import dev.ragnarok.fenrir.activity.*
import dev.ragnarok.fenrir.activity.alias.*
import dev.ragnarok.fenrir.api.model.LocalServerSettings
import dev.ragnarok.fenrir.api.model.PlayerCoverBackgroundSettings
import dev.ragnarok.fenrir.api.model.SlidrSettings
import dev.ragnarok.fenrir.db.DBHelper
import dev.ragnarok.fenrir.db.TempDataHelper
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.listener.CanBackPressedCallback
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.UpdatableNavigation
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels
import dev.ragnarok.fenrir.media.record.AudioRecordWrapper
import dev.ragnarok.fenrir.model.DrawerCategory
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.model.SwitchableCategory
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.clear_cache
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.EllipseTransformation
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.service.FaveSyncWorker
import dev.ragnarok.fenrir.service.KeepLongpollService
import dev.ragnarok.fenrir.settings.AvatarStyle
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.backup.SettingsBackup
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.refresh.RefreshToken
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import dev.ragnarok.fenrir.util.serializeble.prefs.Preferences
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class PreferencesFragment : AbsPreferencesFragment(), PreferencesAdapter.OnScreenChangeListener,
    BackPressCallback, CanBackPressedCallback {
    private var preferencesView: RecyclerView? = null
    private var layoutManager: LinearLayoutManager? = null
    private var searchView: MySearchView? = null
    private val disposables = CompositeDisposable()
    private var sleepDataDisposable = Disposable.disposed()
    private val SEARCH_DELAY = 2000
    override val keyInstanceState: String = "root_preferences"

    private val requestLightBackground = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            changeDrawerBackground(false, result.data)
        }
    }
    private val requestDarkBackground = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            changeDrawerBackground(true, result.data)
        }
    }
    private val requestPin = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            PlaceFactory.securitySettingsPlace.tryOpenWith(requireActivity())
        }
    }

    private val requestPinForAdditionalInfo = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            showAdditionalInfo(false)
        }
    }

    private val requestContactsPermission = requestPermissionsAbs(
        arrayOf(Manifest.permission.READ_CONTACTS)
    ) {
        PlaceFactory.getFriendsByPhonesPlace(accountId).tryOpenWith(requireActivity())
    }
    private val requestReadPermission = requestPermissionsAbs(
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    ) {
        createCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.preference_fenrir_list_fragment, container, false)
        searchView = root.findViewById(R.id.searchview)
        searchView?.setRightButtonVisibility(false)
        searchView?.setLeftIcon(R.drawable.magnify)
        searchView?.setQuery("", true)
        layoutManager = LinearLayoutManager(requireActivity())
        val isNull = createPreferenceAdapter()
        preferencesView = (root.findViewById<RecyclerView>(R.id.recycler_view)).apply {
            layoutManager = this@PreferencesFragment.layoutManager
            adapter = preferencesAdapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(
                requireActivity(),
                R.anim.preference_layout_fall_down
            )
        }
        if (isNull) {
            preferencesAdapter?.onScreenChangeListener = this
            loadInstanceState({ createRootScreen() }, root)
        }

        searchView?.let {
            it.setOnBackButtonClickListener(object : MySearchView.OnBackButtonClickListener {
                override fun onBackButtonClick() {
                    if (it.text.nonNullNoEmpty() && it.text.trimmedNonNullNoEmpty()) {
                        preferencesAdapter?.findPreferences(
                            requireActivity(),
                            (it.text ?: return).toString(),
                            root
                        )
                    }
                }
            })
            it.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    sleepDataDisposable.dispose()
                    if (query.nonNullNoEmpty() && query.trimmedNonNullNoEmpty()) {
                        preferencesAdapter?.findPreferences(requireActivity(), query, root)
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    sleepDataDisposable.dispose()
                    sleepDataDisposable = Single.just(Any())
                        .delay(SEARCH_DELAY.toLong(), TimeUnit.MILLISECONDS)
                        .fromIOToMain()
                        .subscribe({
                            if (newText.nonNullNoEmpty() && newText.trimmedNonNullNoEmpty()) {
                                preferencesAdapter?.findPreferences(
                                    requireActivity(),
                                    newText,
                                    root
                                )
                            }
                        }, { RxUtils.dummy() })
                    return false
                }
            })
        }
        return root
    }

    override fun onBackPressed(): Boolean {
        return !goBack()
    }

    override fun canBackPressed(): Boolean {
        return canGoBack()
    }

    override fun beforeScreenChange(screen: PreferenceScreen): Boolean {
        preferencesView?.let { preferencesAdapter?.stopObserveScrollPosition(it) }
        return true
    }

    override fun onScreenChanged(screen: PreferenceScreen, subScreen: Boolean, animation: Boolean) {
        searchView?.visibility = if (screen.getSearchQuery() == null) View.VISIBLE else View.GONE
        if (animation) {
            preferencesView?.scheduleLayoutAnimation()
        }
        preferencesView?.let { preferencesAdapter?.restoreAndObserveScrollPosition(it) }
        val actionBar = ActivityUtils.supportToolbarFor(this)
        if (actionBar != null) {
            if (screen.key == "root" || screen.title.isEmpty() && screen.titleRes == DEFAULT_RES_ID) {
                actionBar.setTitle(R.string.settings)
            } else if (screen.titleRes != DEFAULT_RES_ID) {
                actionBar.setTitle(screen.titleRes)
            } else {
                actionBar.title = screen.title
            }
            actionBar.subtitle = null
        }
        if (requireActivity() is UpdatableNavigation) {
            (requireActivity() as UpdatableNavigation).onUpdateNavigation()
        }
    }

    private val fixTimeDir = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            try {
                doFixDirTime(
                    result.data?.getStringExtra(Extra.PATH) ?: return@registerForActivityResult,
                    true
                )
                createCustomToast(requireActivity())
                    .setDuration(Toast.LENGTH_LONG)
                    .showToastSuccessBottom(R.string.success)
            } catch (e: Exception) {
                createCustomToast(requireActivity())
                    .setDuration(Toast.LENGTH_LONG)
                    .showToastError(e.localizedMessage)
            }
        }
    }

    private val musicDir = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            PreferenceScreen.getPreferences(requireActivity())
                .edit().putString(
                    "music_dir",
                    result.data?.getStringExtra(Extra.PATH)
                ).apply()
            preferencesAdapter?.applyToPreference("music_dir") { ss -> (ss as CustomTextPreference).reload() }
        }
    }


    private val photoDir = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            PreferenceScreen.getPreferences(requireActivity())
                .edit().putString(
                    "photo_dir",
                    result.data?.getStringExtra(Extra.PATH)
                ).apply()
            preferencesAdapter?.applyToPreference("photo_dir") { ss -> (ss as CustomTextPreference).reload() }
        }
    }

    private val videoDir = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            PreferenceScreen.getPreferences(requireActivity())
                .edit().putString(
                    "video_dir",
                    result.data?.getStringExtra(Extra.PATH)
                ).apply()
            preferencesAdapter?.applyToPreference("video_dir") { ss -> (ss as CustomTextPreference).reload() }
        }
    }

    private val docDir = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            PreferenceScreen.getPreferences(requireActivity())
                .edit().putString(
                    "docs_dir",
                    result.data?.getStringExtra(Extra.PATH)
                ).apply()
            preferencesAdapter?.applyToPreference("docs_dir") { ss -> (ss as CustomTextPreference).reload() }
        }
    }


    private val stickerDir = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            PreferenceScreen.getPreferences(requireActivity())
                .edit().putString(
                    "sticker_dir",
                    result.data?.getStringExtra(Extra.PATH)
                ).apply()
            preferencesAdapter?.applyToPreference("sticker_dir") { ss -> (ss as CustomTextPreference).reload() }

        }
    }


    private fun selectLocalImage(isDark: Boolean) {
        if (!AppPerms.hasReadStoragePermission(requireActivity())) {
            requestReadPermission.launch()
            return
        }
        val intent = Intent(activity, PhotosActivity::class.java)
        intent.putExtra(PhotosActivity.EXTRA_MAX_SELECTION_COUNT, 1)
        if (isDark) {
            requestDarkBackground.launch(intent)
        } else {
            requestLightBackground.launch(intent)
        }
    }

    private fun enableChatPhotoBackground(index: Int) {
        val bEnable: Boolean = when (index) {
            0, 1, 2, 3 -> false
            else -> true
        }
        preferencesAdapter?.applyToPreference("chat_light_background") {
            it.enabled = bEnable
        }
        preferencesAdapter?.applyToPreference("chat_dark_background") {
            it.enabled = bEnable
        }
        preferencesAdapter?.applyToPreference("reset_chat_background") {
            it.enabled = bEnable
        }
    }

    private fun lunchScopedControl() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        } else {
            throw UnsupportedOperationException("VERSION.SDK_INT < R")
        }
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        requireActivity().startActivity(intent)
    }

    private fun doFixDirTime(dir: String, isRoot: Boolean) {
        val root = File(dir)
        val list: ArrayList<Long> = ArrayList()
        if (root.exists() && root.isDirectory) {
            val children = root.list()
            if (children != null) {
                for (child in children) {
                    val rem = File(root, child)
                    if (rem.isFile && !rem.isHidden && !isRoot) {
                        list.add(rem.lastModified())
                    } else if (rem.isDirectory && !rem.isHidden && rem.name != "." && rem.name != "..") {
                        doFixDirTime(rem.absolutePath, false)
                    }
                }
            }
        } else {
            return
        }
        if (isRoot) {
            return
        }

        val res = list.maxOrNull()
        res?.let {
            root.setLastModified(it)
        }
    }

    @SuppressLint("CheckResult")
    @Suppress("DEPRECATION")
    private fun createRootScreen() = screen(requireActivity()) {
        subScreen("general_preferences") {
            titleRes = R.string.general_settings
            iconRes = R.drawable.preferences_settings
            pref("proxy") {
                titleRes = R.string.http_proxy
                onClick {
                    startActivity(Intent(requireActivity(), ProxyManagerActivity::class.java))
                    true
                }
            }
            switch("use_api_5_90_for_audio") {
                summaryRes = R.string.use_api_5_90_for_audio_summary
                titleRes = R.string.use_api_5_90_for_audio
                defaultValue = true
            }
            subScreen("audio_catalog_v2") {
                titleRes = R.string.audio_catalog_v2
                summaryRes = R.string.experimental
                visible = Utils.isOfficialVKCurrent

                switch("audio_catalog_v2_enable") {
                    defaultValue = true
                    titleRes = R.string.turn_on
                }

                pref("audio_catalog_v2_sort_list") {
                    titleRes = R.string.catalog_v2_edit_list
                    dependency = "audio_catalog_v2_enable"
                    onClick {
                        PlaceFactory.catalogV2ListEditPlace.tryOpenWith(requireActivity())
                        true
                    }
                }
            }
            singleChoice(
                "language_ui",
                selItems(R.array.array_language_names, R.array.array_language_items),
                parentFragmentManager
            ) {
                iconRes = R.drawable.lang_settings
                initialSelection = "0"
                titleRes = R.string.language_ui
                onSelectionChange {
                    requireActivity().recreate()
                }
            }
            pref("blacklist") {
                iconRes = R.drawable.ban_settings
                titleRes = R.string.user_blacklist_title
                onClick {
                    PlaceFactory.getUserBlackListPlace(
                        accountId
                    ).tryOpenWith(requireActivity())
                    true
                }
            }
            pref("friends_by_phone") {
                titleRes = R.string.friends_by_phone
                iconRes = R.drawable.sync_settings
                onClick {
                    if (!AppPerms.hasContactsPermission(requireActivity())) {
                        requestContactsPermission.launch()
                    } else {
                        PlaceFactory.getFriendsByPhonesPlace(accountId)
                            .tryOpenWith(requireActivity())
                    }
                    true
                }
            }
            pref("faves_sync") {
                titleRes = R.string.faves_sync
                iconRes = R.drawable.sync_settings
                onClick {
                    val faveWork = OneTimeWorkRequest.Builder(FaveSyncWorker::class.java)
                    WorkManager.getInstance(requireActivity()).enqueue(faveWork.build())
                    true
                }
            }
            pref(KEY_SECURITY) {
                titleRes = R.string.security
                iconRes = R.drawable.security_settings
                onClick {
                    onSecurityClick()
                    true
                }
            }
            pref(KEY_NOTIFICATION) {
                titleRes = R.string.notif_setting_title
                iconRes = R.drawable.feed_settings
                onClick {
                    if (Utils.hasOreo()) {
                        val intent = Intent()
                        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                        intent.putExtra(
                            "android.provider.extra.APP_PACKAGE",
                            requireContext().packageName
                        )
                        requireContext().startActivity(intent)
                    } else {
                        PlaceFactory.notificationSettingsPlace.tryOpenWith(requireActivity())
                    }
                    true
                }
            }
            pref("notifications_sync") {
                titleRes = R.string.notifications_sync
                iconRes = R.drawable.feed_settings
                onClick {
                    disposables.add(
                        InteractorFactory.createAccountInteractor()
                            .getPushSettings(accountId)
                            .fromIOToMain()
                            .subscribe({
                                Settings.get().notifications().resetAccount(accountId)
                                for (i in it) {
                                    if (i.disabled_until < 0) {
                                        Settings.get().notifications()
                                            .forceDisable(accountId, i.peer_id)
                                    }
                                }
                                createCustomToast(requireActivity()).showToast(R.string.success)
                            }, { createCustomToast(requireActivity()).showToastThrowable(it) })
                    )
                    true
                }
            }
            pref("reset_notifications_groups") {
                titleRes = R.string.reset_notifications_groups
                iconRes = R.drawable.feed_settings
                visible = Utils.hasOreo()
                onClick {
                    if (Utils.hasOreo()) {
                        AppNotificationChannels.invalidateSoundChannels(requireActivity())
                        createCustomToast(requireActivity())
                            .setDuration(Toast.LENGTH_LONG)
                            .showToastSuccessBottom(R.string.success)
                    }
                    true
                }
            }
            pref("refresh_audio_token") {
                titleRes = R.string.refresh_audio_token
                iconRes = R.drawable.dir_song
                onClick {
                    Settings.get().accounts().getAccessToken(accountId)?.let { ito ->
                        disposables.add(
                            RefreshToken.upgradeTokenRxPref(
                                accountId, ito
                            )
                                .fromIOToMain()
                                .subscribe({
                                    createCustomToast(requireActivity()).showToast(if (it) R.string.success else (R.string.error))
                                }, RxUtils.ignore())
                        )
                    }
                    true
                }
            }
        }
        subScreen("appearance_settings") {
            titleRes = R.string.appearance_settings
            iconRes = R.drawable.select_colored

            pref(KEY_APP_THEME) {
                iconRes = R.drawable.select_colored
                titleRes = R.string.choose_theme_title
                onClick {
                    PlaceFactory.settingsThemePlace.tryOpenWith(requireActivity())
                    true
                }
            }

            pref("select_custom_icon") {
                titleRes = R.string.select_custom_icon
                iconRes = R.drawable.app_icon_pref
                val hasOreo = Utils.hasOreo()
                visible = hasOreo
                if (hasOreo) {
                    onClick {
                        showSelectIcon()
                        true
                    }
                }
            }

            singleChoice(
                KEY_NIGHT_SWITCH,
                selItems(R.array.night_mode_names, R.array.night_mode_values),
                parentFragmentManager
            ) {
                initialSelection = "1"
                titleRes = R.string.night_mode_title
                iconRes = R.drawable.night_mode_pref
                onSelectionChange {
                    AppCompatDelegate.setDefaultNightMode(it.toInt())
                }
            }

            singleChoice(
                "theme_overlay",
                selItems(R.array.theme_overlay_names, R.array.theme_overlay_values),
                parentFragmentManager
            ) {
                initialSelection = "0"
                titleRes = R.string.theme_overlay
                onSelectionChange {
                    requireActivity().recreate()
                }
            }

            switch("snow_mode") {
                defaultValue = false
                titleRes = R.string.snow_mode
                onCheckedChange {
                    requireActivity().recreate()
                }
            }

            switch("webview_night_mode") {
                defaultValue = true
                summaryRes = R.string.webview_summary
                titleRes = R.string.webview_night_mode
            }

            seekBar("font_size_int") {
                min = -3
                max = 9
                default = 0
                step = 1
                showTickMarks = true
                titleRes = R.string.font_size
                onSeek {
                    sleepDataDisposable.dispose()
                    sleepDataDisposable = Single.just(Any())
                        .delay(1, TimeUnit.SECONDS)
                        .fromIOToMain()
                        .subscribe({
                            requireActivity().recreate()
                        }, { RxUtils.dummy() })
                }
            }

            switch("font_only_for_chats") {
                defaultValue = false
                titleRes = R.string.font_only_for_chats
                onCheckedChange {
                    requireActivity().recreate()
                }
            }

            singleChoice(
                "photo_rounded_view",
                selItems(R.array.array_photo_rounded_names, R.array.array_photo_rounded_items),
                parentFragmentManager
            ) {
                initialSelection = "0"
                titleRes = R.string.photo_rounded_view
            }

            pref(KEY_AVATAR_STYLE) {
                titleRes = R.string.avatar_style_title
                onClick {
                    showAvatarStyleDialog()
                    true
                }
            }

            switch("show_wall_cover") {
                defaultValue = true
                titleRes = R.string.show_wall_cover
            }

        }

        subScreen("pagan_section") {
            iconRes = R.drawable.valknut_settings
            titleRes = R.string.wall_themed
            collapseIcon = true
            singleChoice(
                "pagan_symbol",
                selItems(
                    dev.ragnarok.fenrir_common.R.array.array_pagan_symbol_names,
                    dev.ragnarok.fenrir_common.R.array.array_pagan_symbol_items
                ),
                parentFragmentManager
            ) {
                initialSelection = "1"
                titleRes = R.string.pagan_symbol
                onSelectionChange {
                    if (Settings.get().main().isSnow_mode)
                        requireActivity().recreate()
                }
            }

            switch("runes_show") {
                defaultValue = true
                titleRes = R.string.runes_show
            }

        }

        subScreen("behaviour_settings") {
            titleRes = R.string.behaviour_settings
            iconRes = R.drawable.behavior_settings
            collapseIcon = true
            singleChoice(KEY_DEFAULT_CATEGORY, initStartPagePreference(), parentFragmentManager) {
                summaryRes = R.string.default_category_summary
                titleRes = R.string.default_category_title
            }

            singleChoice(
                "start_news",
                selItems(
                    R.array.array_news_start_updates_names,
                    R.array.array_news_start_updates_items
                ),
                parentFragmentManager
            ) {
                initialSelection = "2"
                titleRes = R.string.start_news
            }

            switch("strip_news_repost") {
                defaultValue = false
                summaryRes = R.string.need_refresh
                titleRes = R.string.strip_news_repost
            }

            switch("communities_in_page_search") {
                defaultValue = false
                titleRes = R.string.communities_in_page_search
            }

            switch("ad_block_story_news") {
                defaultValue = true
                summaryRes = R.string.ad_block_story_news_summary
                titleRes = R.string.ad_block_story_news
            }

            multiLineText("block_news_by_words_set", parentFragmentManager) {
                titleRes = R.string.block_news_by_words
                messageRes = R.string.block_news_by_words_message
            }

            switch("new_loading_dialog") {
                defaultValue = true
                titleRes = R.string.new_loading_dialog
                visible = Utils.hasMarshmallow()
            }

            switch("autoplay_gif") {
                defaultValue = true
                dependency = "enable_native"
                titleRes = R.string.autoplay_gif
            }

            singleChoice(
                "viewpager_page_transform",
                selItems(
                    R.array.array_pager_transform_names,
                    R.array.array_pager_transform_anim_items
                ),
                parentFragmentManager
            ) {
                initialSelection = "0"
                titleRes = R.string.viewpager_page_transform
            }

            singleChoice(
                "player_cover_transform",
                selItems(
                    R.array.array_pager_transform_names,
                    R.array.array_pager_transform_anim_items
                ),
                parentFragmentManager
            ) {
                initialSelection = "1"
                titleRes = R.string.player_cover_transform
            }

            singleChoice(
                "is_open_url_internal",
                selItems(
                    R.array.array_is_open_url_internal_names,
                    R.array.array_is_open_url_internal_items
                ),
                parentFragmentManager
            ) {
                initialSelection = "1"
                titleRes = R.string.open_links_in_app
            }

            switch("notification_force_link") {
                defaultValue = false
                titleRes = R.string.notification_force_link
            }

            switch("load_history_notif") {
                defaultValue = false
                summaryRes = R.string.load_history_notif_summary
                titleRes = R.string.load_history_notif
            }

            switch("use_internal_downloader") {
                defaultValue = true
                summaryRes = R.string.use_internal_downloader_summary
                titleRes = R.string.use_internal_downloader
            }

            switch("do_auto_play_video") {
                defaultValue = false
                titleRes = R.string.do_auto_play_video
            }

            switch("video_controller_to_decor") {
                defaultValue = false
                titleRes = R.string.video_controller_to_decor
            }

            switch("video_swipes") {
                defaultValue = true
                titleRes = R.string.video_swipes
            }

            singleChoice(
                "end_list_anim",
                selItems(R.array.array_end_list_anim_names, R.array.array_end_list_anim_items),
                parentFragmentManager
            ) {
                initialSelection = "0"
                titleRes = R.string.end_list_anim
            }

            switch("disable_history") {
                defaultValue = false
                titleRes = R.string.disable_history
            }

        }

        subScreen("photo_settings") {
            titleRes = R.string.photo_settings
            iconRes = R.drawable.photo_settings
            collapseIcon = true
            singleChoice(
                "image_size",
                selItems(
                    R.array.array_image_sizes_settings_names,
                    R.array.array_image_sizes_settings_values
                ),
                parentFragmentManager
            ) {
                initialSelection = "0"
                summaryRes = R.string.select_image_size_summary
                titleRes = R.string.select_image_size_title
            }

            singleChoice(
                "photo_preview_size",
                selItems(R.array.preview_preview_size, R.array.preview_preview_size_values),
                parentFragmentManager
            ) {
                initialSelection = "4"
                titleRes = R.string.photo_preview_size_title
                onSelectionChange {
                    Settings.get().main().notifyPrefPreviewSizeChanged()
                }
            }

            switch("download_photo_tap") {
                defaultValue = true
                titleRes = R.string.download_photo_tap
            }

            switch("do_zoom_photo") {
                defaultValue = true
                titleRes = R.string.do_zoom_photo
                onCheckedChange {
                    requireActivity().recreate()
                }
            }

            switch("show_photos_line") {
                defaultValue = true
                titleRes = R.string.show_photos_line
            }

            switch("show_photos_date") {
                defaultValue = false
                titleRes = R.string.show_photos_date
            }

            switch("disable_likes") {
                defaultValue = false
                titleRes = R.string.disable_likes
            }

            switch("change_upload_size") {
                defaultValue = false
                titleRes = R.string.change_upload_size
            }

        }

        subScreen("input_settings") {
            titleRes = R.string.input
            iconRes = R.drawable.input_settings
            collapseIcon = true
            switch("emojis_type") {
                defaultValue = false
                titleRes = R.string.emojis_type_title
            }

            switch("dont_write") {
                defaultValue = false
                titleRes = R.string.dont_write
            }

            switch("hint_stickers") {
                defaultValue = true
                titleRes = R.string.hint_stickers
            }

            switch("send_by_enter") {
                defaultValue = false
                titleRes = R.string.send_on_enter_title
            }

            switch("stickers_by_theme") {
                defaultValue = true
                summaryRes = R.string.stickers_by_theme_summary
                titleRes = R.string.stickers_by_theme
            }

            switch("stickers_by_new") {
                defaultValue = false
                titleRes = R.string.stickers_by_new
            }

            switch("emojis_full_screen") {
                defaultValue = false
                titleRes = R.string.emojis_full_screen
            }

            switch("over_ten_attach") {
                defaultValue = false
                titleRes = R.string.over_ten_attach
            }

        }

        subScreen("custom_chat") {
            titleRes = R.string.custom_chat
            iconRes = R.drawable.chat_settings
            switch("notification_bubbles") {
                defaultValue = true
                titleRes = R.string.notification_bubbles
                visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            }

            singleChoice(
                "swipes_for_chats",
                selItems(R.array.array_swipes_chats_names, R.array.array_swipes_chats_items),
                parentFragmentManager
            ) {
                initialSelection = "1"
                titleRes = R.string.swipes_for_chats_mode
            }

            pref("slidr_settings") {
                titleRes = R.string.slidr_settings
                onClick {
                    SlidrEditDialog().show(parentFragmentManager, "SlidrPrefs")
                    true
                }
            }

            switch("messages_menu_down") {
                defaultValue = false
                titleRes = R.string.messages_menu_down
                onCheckedChange {
                    requireActivity().recreate()
                }
            }

            switch("expand_voice_transcript") {
                defaultValue = false
                titleRes = R.string.expand_voice_transcript
            }

            singleChoice(
                "chat_background",
                selItems(R.array.array_chat_background_names, R.array.array_chat_background_items),
                parentFragmentManager
            ) {
                initialSelection = "0"
                titleRes = R.string.chat_background
                getString()?.let { enableChatPhotoBackground(it.toInt()) }
                onSelectionChange {
                    val index = it.toInt()
                    enableChatPhotoBackground(index)
                }
            }

            pref("chat_light_background") {
                titleRes = R.string.chat_light_background
                onClick {
                    selectLocalImage(false)
                    true
                }
                val bitmap = getDrawerBackgroundFile(requireActivity(), true)
                if (bitmap.exists()) {
                    icon(Drawable.createFromPath(bitmap.absolutePath))
                } else icon(R.drawable.dir_photo)
            }

            pref("chat_dark_background") {
                titleRes = R.string.chat_dark_background
                onClick {
                    selectLocalImage(true)
                    true
                }
                val bitmap = getDrawerBackgroundFile(requireActivity(), false)
                if (bitmap.exists()) {
                    icon(Drawable.createFromPath(bitmap.absolutePath))
                } else icon(R.drawable.dir_photo)
            }

            pref("reset_chat_background") {
                titleRes = R.string.delete_chats_photo
                onClick {
                    val chatLight = getDrawerBackgroundFile(requireActivity(), true)
                    val chatDark = getDrawerBackgroundFile(requireActivity(), false)
                    try {
                        tryDeleteFile(chatLight)
                        tryDeleteFile(chatDark)
                    } catch (e: Exception) {
                        createCustomToast(activity).setDuration(Toast.LENGTH_LONG)
                            .showToastError(e.message)
                    }
                    preferencesAdapter?.applyToPreference("chat_light_background") {
                        val bitmap = getDrawerBackgroundFile(requireActivity(), true)
                        if (bitmap.exists()) {
                            it.icon(Drawable.createFromPath(bitmap.absolutePath))
                        } else it.icon(R.drawable.dir_photo)
                        it.requestRebind()
                    }

                    preferencesAdapter?.applyToPreference("chat_dark_background") {
                        val bitmap = getDrawerBackgroundFile(requireActivity(), false)
                        if (bitmap.exists()) {
                            it.icon(Drawable.createFromPath(bitmap.absolutePath))
                        } else it.icon(R.drawable.dir_photo)
                        it.requestRebind()
                    }
                    true
                }
            }

            switch("custom_chat_color_usage") {
                defaultValue = false
                summaryRes = R.string.custom_chat_color_summary
                titleRes = R.string.use_custom_chat_color
            }

            colorPick("custom_chat_color", parentFragmentManager) {
                dependency = "custom_chat_color_usage"
                titleRes = R.string.custom_chat_color
                alphaSlider = true
                density = 12
                defaultValue = Color.WHITE
                lightSlider = true
            }

            colorPick("custom_chat_color_second", parentFragmentManager) {
                dependency = "custom_chat_color_usage"
                titleRes = R.string.custom_chat_color_second
                alphaSlider = true
                density = 12
                defaultValue = Color.WHITE
                lightSlider = true
            }

            switch("my_message_no_color") {
                defaultValue = false
                titleRes = R.string.my_message_no_color
                disableDependents = true
            }

            switch("custom_message_color_usage") {
                defaultValue = false
                dependency = "my_message_no_color"
                titleRes = R.string.usage_custom_message_color
            }

            colorPick("custom_message_color", parentFragmentManager) {
                dependency = "custom_message_color_usage"
                titleRes = R.string.custom_message_color
                alphaSlider = true
                density = 12
                defaultValue = Color.parseColor("#CBD438FF")
                lightSlider = true
            }

            colorPick("custom_second_message_color", parentFragmentManager) {
                dependency = "custom_message_color_usage"
                titleRes = R.string.custom_second_message_color
                alphaSlider = true
                density = 12
                defaultValue = Color.parseColor("#BF6539DF")
                lightSlider = true
            }

            switch("disable_sensored_voice") {
                defaultValue = false
                summaryRes = R.string.disable_sensored_voice_summary
                titleRes = R.string.disable_sensored_voice
            }

            switch("display_writing") {
                defaultValue = true
                titleRes = R.string.display_writing
            }

        }

        subScreen("additional_settings") {
            iconRes = R.drawable.additional_settings
            titleRes = R.string.additional_settings
            collapseIcon = true
            switch("auto_read") {
                defaultValue = false
                titleRes = R.string.auto_read
            }

            switch("mark_listened_voice") {
                defaultValue = true
                titleRes = R.string.mark_listened_voice
            }

            switch("not_update_dialogs") {
                defaultValue = false
                titleRes = R.string.not_update_dialogs
            }

            switch("enable_last_read") {
                defaultValue = false
                summaryRes = R.string.enable_last_read_summary
                titleRes = R.string.enable_last_read
            }

            switch("not_read_show") {
                defaultValue = true
                summaryRes = R.string.not_read_show_summary
                titleRes = R.string.not_read_show
            }

            switch("headers_in_dialog") {
                defaultValue = true
                titleRes = R.string.headers_in_dialog
            }

            switch("info_reading") {
                defaultValue = true
                titleRes = R.string.info_reading
            }

            switch("show_mutual_count") {
                defaultValue = false
                summaryRes = R.string.show_mutual_count_summary
                titleRes = R.string.show_mutual_count
            }

            switch("be_online") {
                defaultValue = false
                titleRes = R.string.be_online
            }

        }

        subScreen("music_settings") {
            titleRes = R.string.music_settings
            iconRes = R.drawable.player_settings
            collapseIcon = true
            switch("audio_round_icon") {
                defaultValue = true
                titleRes = R.string.audio_round_icon
                onCheckedChange {
                    requireActivity().recreate()
                }
            }

            switch("ongoing_player_notification") {
                titleRes = R.string.ongoing_player_notification
            }

            switch("force_cache") {
                titleRes = R.string.force_cache
            }

            switch("use_long_click_download") {
                defaultValue = false
                titleRes = R.string.use_long_click_download
            }

            switch("revert_play_audio") {
                defaultValue = false
                titleRes = R.string.revert_play_audio
            }

            switch("player_has_background") {
                defaultValue = true
                titleRes = R.string.player_has_background
            }

            pref("player_background") {
                titleRes = R.string.player_background
                dependency = "player_has_background"
                onClick {
                    PlayerBackgroundDialog().show(parentFragmentManager, "PlayerBackgroundPref")
                    true
                }
            }

            switch("use_stop_audio") {
                defaultValue = false
                titleRes = R.string.use_stop_audio
            }

            switch("broadcast") {
                defaultValue = false
                titleRes = R.string.audio_in_status
            }

            switch("audio_save_mode_button") {
                defaultValue = true
                titleRes = R.string.audio_save_mode_button
            }

            switch("show_mini_player") {
                defaultValue = true
                titleRes = R.string.show_mini_player
                onCheckedChange {
                    requireActivity().recreate()
                }
            }

            switch("show_audio_top") {
                defaultValue = false
                titleRes = R.string.show_audio_top
            }

            singleChoice(
                "lifecycle_music_service",
                selItems(R.array.array_lifecycle_names, R.array.array_lifecycle_items),
                parentFragmentManager
            ) {
                initialSelection = "300000"
                titleRes = R.string.lifecycle_music_service
            }

            singleChoice(
                "ffmpeg_audio_codecs",
                selItems(R.array.array_ffmpeg_names, R.array.array_ffmpeg_items),
                parentFragmentManager
            ) {
                initialSelection = "1"
                dependency = "enable_native"
                titleRes = R.string.ffmpeg_audio_codecs
            }

        }

        subScreen("wear_section") {
            titleRes = R.string.for_wear
            iconRes = R.drawable.wear_settings
            collapseIcon = true
            switch("show_bot_keyboard") {
                defaultValue = true
                titleRes = R.string.show_bot_keyboard
            }

            switch("is_player_support_volume") {
                defaultValue = false
                titleRes = R.string.is_player_support_volume
            }

            switch("show_profile_in_additional_page") {
                defaultValue = true
                titleRes = R.string.show_profile_in_additional_page
                onCheckedChange {
                    requireActivity().recreate()
                }
            }

        }

        subScreen("sidebar_settings_section") {
            titleRes = R.string.sidebar_settings
            iconRes = R.drawable.sidebar_settings
            collapseIcon = true
            pref(KEY_DRAWER_ITEMS) {
                summaryRes = R.string.drawer_categories_summary
                titleRes = R.string.drawer_categories_title
                onClick {
                    PlaceFactory.drawerEditPlace.tryOpenWith(requireActivity())
                    true
                }
            }

            switch("show_recent_dialogs") {
                defaultValue = true
                titleRes = R.string.show_recent_dialogs
                onCheckedChange {
                    requireActivity().recreate()
                }
            }

            switch("is_side_navigation") {
                defaultValue = false
                summaryRes = R.string.need_restart
                titleRes = R.string.is_side_navigation
            }

            switch("is_side_no_stroke") {
                defaultValue = false
                dependency = "is_side_navigation"
                titleRes = R.string.is_side_no_stroke
                onCheckedChange {
                    requireActivity().recreate()
                }
            }

            switch("is_side_transition") {
                defaultValue = true
                dependency = "is_side_navigation"
                titleRes = R.string.side_transition
                onCheckedChange {
                    requireActivity().recreate()
                }
            }

            pref(KEY_SIDE_DRAWER_ITEMS) {
                dependency = "is_side_navigation"
                summaryRes = R.string.drawer_categories_summary
                titleRes = R.string.side_drawer_categories_title
                onClick {
                    PlaceFactory.sideDrawerEditPlace.tryOpenWith(requireActivity())
                    true
                }
            }

            switch("do_not_clear_back_stack") {
                defaultValue = false
                titleRes = R.string.do_not_clear_back_stack
            }

        }

        subScreen("download_directory") {
            titleRes = R.string.download_directory
            iconRes = R.drawable.save_settings
            pref("scoped_storage") {
                titleRes = R.string.scoped_storage
                iconRes = R.drawable.security_settings
                val hasScoped = Utils.hasScopedStorage()
                visible = hasScoped
                if (hasScoped) {
                    onClick {
                        lunchScopedControl()
                        true
                    }
                }
            }

            customText("music_dir", parentFragmentManager) {
                titleRes = R.string.music_dir
                iconRes = R.drawable.dir_song
                onClick {
                    if (!AppPerms.hasReadStoragePermission(requireActivity())) {
                        requestReadPermission.launch()
                        return@onClick true
                    }
                    musicDir.launch(
                        FileManagerSelectActivity.makeFileManager(
                            requireActivity(),
                            Environment.getExternalStorageDirectory().absolutePath,
                            "dirs"
                        )
                    )
                    true
                }
            }

            customText("photo_dir", parentFragmentManager) {
                titleRes = R.string.photo_dir
                iconRes = R.drawable.dir_photo
                onClick {
                    if (!AppPerms.hasReadStoragePermission(requireActivity())) {
                        requestReadPermission.launch()
                        return@onClick true
                    }
                    photoDir.launch(
                        FileManagerSelectActivity.makeFileManager(
                            requireActivity(),
                            Environment.getExternalStorageDirectory().absolutePath,
                            "dirs"
                        )
                    )
                    true
                }
            }

            customText("video_dir", parentFragmentManager) {
                titleRes = R.string.video_dir
                iconRes = R.drawable.dir_video
                onClick {
                    if (!AppPerms.hasReadStoragePermission(requireActivity())) {
                        requestReadPermission.launch()
                        return@onClick true
                    }
                    videoDir.launch(
                        FileManagerSelectActivity.makeFileManager(
                            requireActivity(),
                            Environment.getExternalStorageDirectory().absolutePath,
                            "dirs"
                        )
                    )
                    true
                }
            }

            customText("docs_dir", parentFragmentManager) {
                titleRes = R.string.docs_dir
                iconRes = R.drawable.dir_doc
                onClick {
                    if (!AppPerms.hasReadStoragePermission(requireActivity())) {
                        requestReadPermission.launch()
                        return@onClick true
                    }
                    docDir.launch(
                        FileManagerSelectActivity.makeFileManager(
                            requireActivity(),
                            Environment.getExternalStorageDirectory().absolutePath,
                            "dirs"
                        )
                    )
                    true
                }
            }

            customText("sticker_dir", parentFragmentManager) {
                titleRes = R.string.sticker_dir
                iconRes = R.drawable.dir_sticker
                onClick {
                    if (!AppPerms.hasReadStoragePermission(requireActivity())) {
                        requestReadPermission.launch()
                        return@onClick true
                    }
                    stickerDir.launch(
                        FileManagerSelectActivity.makeFileManager(
                            requireActivity(),
                            Environment.getExternalStorageDirectory().absolutePath,
                            "dirs"
                        )
                    )
                    true

                }
            }

            switch("photo_to_user_dir") {
                defaultValue = true
                titleRes = R.string.photo_to_user_dir
                iconRes = R.drawable.dir_groups
            }

            switch("download_voice_ogg") {
                defaultValue = true
                titleRes = R.string.download_voice_ogg
            }

        }

        subScreen("settings_show_logs_title") {
            titleRes = R.string.settings_show_logs_title
            collapseIcon = true
            iconRes = R.drawable.developer_mode
            switch("do_logs") {
                defaultValue = false
                dependency = "developer_mode"
                titleRes = R.string.do_logs
            }

            pref("show_logs") {
                dependency = "do_logs"
                titleRes = R.string.settings_show_logs_title
                onClick {
                    PlaceFactory.logsPlace.tryOpenWith(requireActivity())
                    true
                }
            }

            switch("dump_fcm") {
                defaultValue = false
                dependency = "do_logs"
                titleRes = R.string.dump_fcm
            }

        }

        subScreen("dev_settings") {
            iconRes = R.drawable.developer_mode
            titleRes = R.string.dev_settings
            switch("developer_mode") {
                defaultValue = Constants.forceDeveloperMode
                titleRes = R.string.developer_mode
                iconRes = R.drawable.developer_mode
            }

            pref("request_executor") {
                dependency = "developer_mode"
                titleRes = R.string.request_executor_title
                onClick {
                    PlaceFactory.getRequestExecutorPlace(
                        accountId
                    ).tryOpenWith(requireActivity())
                    true
                }
            }

            editText("vk_api_domain", parentFragmentManager) {
                defaultValue = "api.vk.com"
                titleRes = R.string.settings_domain
                iconRes = R.drawable.web_settings
                isTrim = true
                onTextChanged {
                    if (it.isNullOrEmpty()) {
                        commitString("api.vk.com")
                        reload()
                    }
                    Includes.proxySettings.broadcastUpdate(null)
                }
            }

            editText("vk_auth_domain", parentFragmentManager) {
                defaultValue = "oauth.vk.com"
                titleRes = R.string.settings_vk_auth_domain
                iconRes = R.drawable.web_settings
                isTrim = true
                onTextChanged {
                    if (it.isNullOrEmpty()) {
                        commitString("oauth.vk.com")
                        reload()
                    }
                    Includes.proxySettings.broadcastUpdate(null)
                }
            }

            singleChoice(
                "current_parser",
                selItems(R.array.array_parser_names, R.array.array_parser_items),
                parentFragmentManager
            ) {
                initialSelection = "0"
                titleRes = R.string.parser_type
                onSelectionChange {
                    Utils.currentParser = it.toInt()
                    Includes.proxySettings.broadcastUpdate(null)
                }
            }

            switch("validate_tls") {
                defaultValue = true
                summaryRes = R.string.validate_tls_summary
                titleRes = R.string.validate_tls
                onCheckedChange {
                    Includes.proxySettings.broadcastUpdate(null)
                }
            }

            switch("native_parcel_photo") {
                defaultValue = true
                dependency = "enable_native"
                summaryRes = R.string.native_parcel_summary
                titleRes = R.string.native_parcel_photo
            }

            switch("native_parcel_story") {
                defaultValue = true
                dependency = "enable_native"
                summaryRes = R.string.native_parcel_summary
                titleRes = R.string.native_parcel_story
            }

            editText("max_bitmap_resolution", parentFragmentManager) {
                defaultValue = "4000"
                textInputType = InputType.TYPE_CLASS_NUMBER
                titleRes = R.string.max_bitmap_resolution
                isTrim = true
                onTextBeforeChanged { its ->
                    var sz = -1
                    try {
                        sz = its.toString().trim { it <= ' ' }.toInt()
                    } catch (ignored: NumberFormatException) {
                    }
                    if (isOverflowCanvas(sz) || sz in 0..99) {
                        return@onTextBeforeChanged false
                    } else {
                        setMaxResolution(sz)
                    }
                    requireActivity().recreate()
                    true
                }
                onTextChanged {
                    cleanCache(requireActivity(), false)
                    requireActivity().recreate()
                }
            }

            editText("max_thumb_resolution", parentFragmentManager) {
                defaultValue = "384"
                textInputType = InputType.TYPE_CLASS_NUMBER
                titleRes = R.string.max_thumb_resolution
                isTrim = true
                onTextBeforeChanged { its ->
                    var sz = -1
                    try {
                        sz = its.toString().trim { it <= ' ' }.toInt()
                    } catch (ignored: NumberFormatException) {
                    }
                    if (isOverflowCanvas(sz) || sz in 0..99) {
                        return@onTextBeforeChanged false
                    } else {
                        setMaxResolution(sz)
                    }
                    true
                }
                onTextChanged {
                    cleanCache(requireActivity(), false)
                    requireActivity().recreate()
                }
            }

            singleChoice(
                "rendering_bitmap_mode",
                selItems(R.array.array_rendering_mode_names, R.array.array_rendering_mode_items),
                parentFragmentManager
            ) {
                initialSelection = "2"
                titleRes = R.string.rendering_mode
                visible = Utils.hasPie()
                onSelectionChange { it ->
                    var sz = 2
                    try {
                        sz = it.trim { it <= ' ' }.toInt()
                    } catch (ignored: NumberFormatException) {
                    }
                    setHardwareRendering(sz)
                    requireActivity().recreate()
                }
            }

            multiLineText("videos_ext", parentFragmentManager) {
                titleRes = R.string.video_ext
                defaultValue = setOf("gif", "mp4", "avi", "mpeg")
                isSpace = true
                onMultiLineTextChange {
                    requireActivity().recreate()
                }
            }

            multiLineText("photo_ext", parentFragmentManager) {
                titleRes = R.string.photo_ext
                isSpace = true
                defaultValue = setOf("jpg", "jpeg", "jpg", "webp", "png", "tiff")
                onMultiLineTextChange {
                    requireActivity().recreate()
                }
            }

            multiLineText("audio_ext", parentFragmentManager) {
                titleRes = R.string.audio_ext
                isSpace = true
                defaultValue = setOf("mp3", "ogg", "flac", "opus")
                onMultiLineTextChange {
                    requireActivity().recreate()
                }
            }

            switch("enable_dirs_files_count") {
                defaultValue = true
                titleRes = R.string.enable_dirs_files_count
            }

            separatorSpace("service_playlists", parentFragmentManager) {
                defaultValue = "-21 -22 -25 -26 -27 -28"
                titleRes = R.string.service_playlists
            }

            switch("enable_native") {
                defaultValue = true
                summaryRes = R.string.need_restart
                titleRes = R.string.enable_native
            }

            switch("settings_no_push") {
                titleRes = R.string.settings_no_push
            }

            switch("recording_to_opus") {
                defaultValue = false
                dependency = "developer_mode"
                summaryRes = R.string.recording_to_opus_summary
                titleRes = R.string.recording_to_opus
            }

            switch("keep_longpoll") {
                iconRes = R.drawable.low_battery
                summaryRes = R.string.settings_keep_longpoll_summary
                titleRes = R.string.settings_keep_longpoll_title
                onCheckedChange {
                    if (it) {
                        KeepLongpollService.start(requireActivity())
                    } else {
                        KeepLongpollService.stop(requireActivity())
                    }
                }
            }

            subScreen("cache_control") {
                titleRes = R.string.cache_cleaner

                pref("account_cache_cleaner") {
                    titleRes = R.string.account_cache_cleaner
                    onClick {
                        MaterialAlertDialogBuilder(requireActivity()).setIcon(R.drawable.ic_outline_delete)
                            .setTitle(R.string.select)
                            .setMessage(R.string.ask_account_cache_cleaner)
                            .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                                DBHelper.removeDatabaseFor(requireActivity(), accountId)
                                cleanCache(requireActivity(), true)
                                Includes.stores.stickers().clearAccount(accountId).fromIOToMain()
                                    .subscribe(RxUtils.dummy(), RxUtils.ignore())
                            }
                            .setNegativeButton(R.string.button_no, null)
                            .setCancelable(true).show()
                        true
                    }
                }

                accentButtonPref("cache_cleaner") {
                    titleRes = R.string.cache_cleaner
                    onClick {
                        TempDataHelper.helper.clear()
                        cleanCache(requireActivity(), true)
                        requireActivity().recreate()
                        true
                    }
                }

                switch("delete_cache_images") {
                    titleRes = R.string.delete_cache_images
                }

                singleChoice(
                    "limit_cache_images",
                    selItems(
                        R.array.array_limit_cache_images_names,
                        R.array.array_limit_cache_images_items
                    ),
                    parentFragmentManager
                ) {
                    initialSelection = "2"
                    titleRes = R.string.limit_cache_images
                    onSelectionChange {
                        TempDataHelper.helper.clear()
                        cleanCache(requireActivity(), true)
                        requireActivity().recreate()
                    }
                }
            }

            switch("compress_incoming_traffic") {
                defaultValue = true
                titleRes = R.string.compress_incoming_traffic
                onCheckedChange {
                    Utils.isCompressIncomingTraffic = it
                    Includes.proxySettings.broadcastUpdate(null)
                }
            }

            switch("compress_outgoing_traffic") {
                defaultValue = false
                titleRes = R.string.compress_outgoing_traffic
                onCheckedChange {
                    Utils.isCompressOutgoingTraffic = it
                    Includes.proxySettings.broadcastUpdate(null)
                }
            }

            pref("delete_dynamic_shortcuts") {
                dependency = "developer_mode"
                titleRes = R.string.delete_dynamic_shortcuts
                visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
                onClick {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        val manager: ShortcutManager? = requireActivity().getSystemService(
                            ShortcutManager::class.java
                        )
                        manager?.removeAllDynamicShortcuts()
                        createCustomToast(context).showToast(R.string.success)
                    }
                    true
                }
            }

            switch("mention_fave") {
                defaultValue = false
                dependency = "developer_mode"
                summaryRes = R.string.do_not_use
                titleRes = R.string.mention_fave
            }

            accentButtonPref("fix_dir_time") {
                dependency = "developer_mode"
                summaryRes = R.string.do_not_use
                titleRes = R.string.fix_dir_time
                onClick {
                    if (!AppPerms.hasReadStoragePermission(requireActivity())) {
                        requestReadPermission.launch()
                        return@onClick true
                    }
                    fixTimeDir.launch(
                        FileManagerSelectActivity.makeFileManager(
                            requireActivity(),
                            Environment.getExternalStorageDirectory().absolutePath,
                            "dirs"
                        )
                    )
                    true
                }
            }

        }

        subScreen("about_section") {
            iconRes = R.drawable.app_info_settings
            titleRes = R.string.about_settings
            collapseIcon = true
            singleChoice(
                "donate_anim_set",
                selItems(R.array.array_donate_anim_names, R.array.array_donate_anim_items),
                parentFragmentManager
            ) {
                initialSelection = "2"
                titleRes = R.string.show_donate_anim
                onSelectionChange {
                    requireActivity().recreate()
                }
            }

            pref("additional_debug") {
                dependency = "developer_mode"
                titleRes = R.string.additional_info
                onClick {
                    showAdditionalInfo(true)
                    true
                }
            }

            pref("reset_settings") {
                titleRes = R.string.reset_settings
                onClick {
                    CustomSnackbars.createCustomSnackbars(view)
                        ?.setDurationSnack(Snackbar.LENGTH_LONG)
                        ?.themedSnack(R.string.reset_settings)
                        ?.setAction(
                            R.string.button_yes
                        ) {
                            val pref =
                                PreferenceScreen.getPreferences(Includes.provideApplicationContext())
                            val preferences = Preferences(pref)

                            for (i in Settings.get().notifications().chatsNotifKeys) {
                                pref.edit().remove(i).apply()
                            }

                            for (i in Settings.get().other().userNameChangesKeys) {
                                pref.edit().remove(i).apply()
                            }
                            SettingsBackup.AppPreferencesList().let {
                                preferences.encode(
                                    SettingsBackup.AppPreferencesList.serializer(),
                                    "",
                                    it
                                )
                            }
                            requireActivity().finish()
                        }?.show()
                    true
                }
            }

            pref("shortcuts") {
                dependency = "developer_mode"
                titleRes = R.string.shortcuts
                onClick {
                    PlaceFactory.getShortcutsPlace().tryOpenWith(requireActivity())
                    true
                }
            }

            accentButtonPref("local_media_server") {
                titleRes = R.string.local_media_server
                onClick {
                    LocalMediaServerDialog().show(parentFragmentManager, "LocalMediaServer")
                    true
                }
            }

            pref("version") {
                titleRes = R.string.app_name
                badge = "VK API $API_VERSION"
                summary = Utils.getAppVersionName(requireActivity())
                onClick {
                    val view = View.inflate(requireActivity(), R.layout.dialog_about_us, null)
                    val anim: RLottieImageView = view.findViewById(R.id.lottie_animation)
                    val txt: TextView =
                        view.findViewById(dev.ragnarok.fenrir_common.R.id.sub_header)
                    txt.setText(Common.getAboutUsHeader(Settings.get().other().paganSymbol))
                    val cbc = Common.getAboutUsAnimation(
                        Settings.get().other().paganSymbol,
                        requireActivity()
                    )
                    if (FenrirNative.isNativeLoaded) {
                        anim.visibility = View.VISIBLE
                        anim.fromRes(
                            cbc.lottieRes,
                            Utils.dp(cbc.lottie_widthHeight),
                            Utils.dp(cbc.lottie_widthHeight),
                            cbc.lottie_replacement
                        )
                        anim.playAnimation()
                    } else {
                        anim.visibility = View.GONE
                    }
                    MaterialAlertDialogBuilder(requireActivity())
                        .setView(view)
                        .show()
                    true
                }
            }

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).setSupportActionBar(view.findViewById(R.id.toolbar))
    }

    private fun onSecurityClick() {
        if (Settings.get().security().isUsePinForSecurity) {
            requestPin.launch(Intent(requireActivity(), EnterPinActivity::class.java))
        } else {
            PlaceFactory.securitySettingsPlace.tryOpenWith(requireActivity())
        }
    }

    @Throws(IOException::class)
    private fun tryDeleteFile(file: File) {
        if (file.exists() && !file.delete()) {
            throw IOException("Can't delete file $file")
        }
    }

    private fun changeDrawerBackground(isDark: Boolean, data: Intent?) {
        val photos: ArrayList<LocalPhoto>? = data?.getParcelableArrayListExtraCompat(PHOTOS)
        if (photos.isNullOrEmpty()) {
            return
        }
        val photo = photos[0]
        val light = !isDark
        val file = getDrawerBackgroundFile(requireActivity(), light)
        var original: Bitmap?
        try {
            original = BitmapFactory.decodeFile(photo.getFullImageUri()?.path)
            original = checkBitmap(original)
            original?.let {
                FileOutputStream(file).use { fos ->
                    it.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    fos.flush()
                    fos.close()
                    it.recycle()
                    val d = Drawable.createFromPath(file.absolutePath)
                    if (light) {
                        preferencesAdapter?.applyToPreference("chat_light_background") {
                            it.icon(d)
                            it.requestRebind()
                        }
                    } else {
                        preferencesAdapter?.applyToPreference("chat_dark_background") {
                            it.icon(d)
                            it.requestRebind()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            createCustomToast(requireActivity()).setDuration(Toast.LENGTH_LONG)
                .showToastError(e.message)
        }
    }

    private fun pushToken(): String? {
        val accountId = Settings.get().accounts().current
        if (accountId == ISettings.IAccountsSettings.INVALID_ID) {
            return null
        }
        val available = Settings.get().pushSettings().registrations
        val can = available.size == 1 && available[0].userId == accountId
        return if (can) available[0].gmcToken else null
    }

    private val internalDataIntent = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            try {
                val file = File(
                    result.data?.getStringExtra(Extra.PATH) ?: return@registerForActivityResult
                )
                if (file.exists()) {
                    MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(R.string.confirmation)
                        .setMessage(R.string.data)
                        .setPositiveButton(R.string.delete) { _: DialogInterface?, _: Int ->
                            file.delete()
                        }
                        .setNegativeButton(R.string.button_save) { _: DialogInterface?, _: Int ->
                            file.copyTo(
                                File(Environment.getExternalStorageDirectory(), file.name),
                                true
                            )
                        }
                        .setNeutralButton(R.string.button_cancel, null)
                        .show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                createCustomToast(requireActivity()).showToastError(e.localizedMessage)
            }
        }
    }

    private fun showAdditionalInfo(secured: Boolean) {
        if (secured) {
            if (!Settings.get().security().isUsePinForSecurity) {
                createCustomToast(requireActivity()).showToastError(R.string.not_supported_hide)
                PlaceFactory.securitySettingsPlace.tryOpenWith(requireActivity())
            } else {
                requestPinForAdditionalInfo.launch(
                    Intent(
                        requireActivity(),
                        EnterPinActivity::class.java
                    )
                )
            }
            return
        }
        val view = View.inflate(requireActivity(), R.layout.dialog_additional_us, null)
        view.findViewById<TextInputEditText>(R.id.item_user_agent)
            .setText(UserAgentTool.USER_AGENT_CURRENT_ACCOUNT)
        view.findViewById<TextInputEditText>(R.id.item_device_id)
            .setText(Utils.getDeviceId(requireActivity()))
        view.findViewById<TextInputEditText>(R.id.item_gcm_token).setText(pushToken())
        view.findViewById<TextInputEditText>(R.id.item_access_token)
            .setText(Settings.get().accounts().currentAccessToken)
        val ot = MaterialAlertDialogBuilder(requireActivity())
            .setPositiveButton(R.string.button_ok, null)
            .setTitle(R.string.additional_info)
            .setView(view)
        if (Utils.hasNougat()) {
            ot.setNegativeButton(R.string.data) { _: DialogInterface?, _: Int ->
                internalDataIntent.launch(
                    FileManagerSelectActivity.makeFileManager(
                        requireActivity(),
                        requireActivity().dataDir.absolutePath,
                        null
                    )
                )
            }
        }
        ot.show()
    }

    private fun showSelectIcon() {
        IconSelectDialog().show(parentFragmentManager, "IconSelectDialog")
    }

    class IconSelectDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val view = View.inflate(requireActivity(), R.layout.icon_select_alert, null)
            view.findViewById<View>(R.id.default_icon).setOnClickListener {
                ToggleAlias.reset(
                    requireActivity()
                )
                dismiss()
            }
            view.findViewById<View>(R.id.blue_icon).setOnClickListener {
                ToggleAlias.toggleTo(
                    requireActivity(),
                    BlueFenrirAlias::class.java
                )
                dismiss()
            }
            view.findViewById<View>(R.id.green_icon).setOnClickListener {
                ToggleAlias.toggleTo(
                    requireActivity(),
                    GreenFenrirAlias::class.java
                )
                dismiss()
            }
            view.findViewById<View>(R.id.violet_icon).setOnClickListener {
                ToggleAlias.toggleTo(
                    requireActivity(),
                    VioletFenrirAlias::class.java
                )
                dismiss()
            }
            view.findViewById<View>(R.id.red_icon).setOnClickListener {
                ToggleAlias.toggleTo(
                    requireActivity(),
                    RedFenrirAlias::class.java
                )
                dismiss()
            }
            view.findViewById<View>(R.id.yellow_icon).setOnClickListener {
                ToggleAlias.toggleTo(
                    requireActivity(),
                    YellowFenrirAlias::class.java
                )
                dismiss()
            }
            view.findViewById<View>(R.id.black_icon).setOnClickListener {
                ToggleAlias.toggleTo(
                    requireActivity(),
                    BlackFenrirAlias::class.java
                )
                dismiss()
            }
            view.findViewById<View>(R.id.vk_official).setOnClickListener {
                ToggleAlias.toggleTo(
                    requireActivity(),
                    VKFenrirAlias::class.java
                )
                dismiss()
            }
            view.findViewById<View>(R.id.white_icon).setOnClickListener {
                ToggleAlias.toggleTo(
                    requireActivity(),
                    WhiteFenrirAlias::class.java
                )
                dismiss()
            }
            view.findViewById<View>(R.id.lineage_icon).setOnClickListener {
                ToggleAlias.toggleTo(
                    requireActivity(),
                    LineageFenrirAlias::class.java
                )
                dismiss()
            }
            return MaterialAlertDialogBuilder(requireActivity())
                .setView(view)
                .create()
        }

    }

    private fun showAvatarStyleDialog() {
        AvatarStyleDialog().show(parentFragmentManager, " AvatarStyleDialog")
    }

    class AvatarStyleDialog : DialogFragment() {
        private fun resolveAvatarStyleViews(style: Int, circle: ImageView, oval: ImageView) {
            when (style) {
                AvatarStyle.CIRCLE -> {
                    circle.visibility = View.VISIBLE
                    oval.visibility = View.INVISIBLE
                }

                AvatarStyle.OVAL -> {
                    circle.visibility = View.INVISIBLE
                    oval.visibility = View.VISIBLE
                }
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val current = Settings.get()
                .ui()
                .avatarStyle
            val view = View.inflate(requireActivity(), R.layout.dialog_avatar_style, null)
            val ivCircle =
                view.findViewById<ImageView>(R.id.circle_avatar)
            val ivOval =
                view.findViewById<ImageView>(R.id.oval_avatar)
            val ivCircleSelected =
                view.findViewById<ImageView>(R.id.circle_avatar_selected)
            val ivOvalSelected =
                view.findViewById<ImageView>(R.id.oval_avatar_selected)
            ivCircle.setOnClickListener {
                resolveAvatarStyleViews(
                    AvatarStyle.CIRCLE,
                    ivCircleSelected,
                    ivOvalSelected
                )
            }
            ivOval.setOnClickListener {
                resolveAvatarStyleViews(
                    AvatarStyle.OVAL,
                    ivCircleSelected,
                    ivOvalSelected
                )
            }
            resolveAvatarStyleViews(current, ivCircleSelected, ivOvalSelected)
            with()
                .load(R.drawable.ava_settings)
                .transform(RoundTransformation())
                .into(ivCircle)
            with()
                .load(R.drawable.ava_settings)
                .transform(EllipseTransformation())
                .into(ivOval)
            return MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.avatar_style_title)
                .setView(view)
                .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                    val circle = ivCircleSelected.visibility == View.VISIBLE
                    Settings.get()
                        .ui()
                        .storeAvatarStyle(if (circle) AvatarStyle.CIRCLE else AvatarStyle.OVAL)
                    clear_cache()
                    parentFragmentManager.setFragmentResult(
                        PreferencesExtra.RECREATE_ACTIVITY_REQUEST,
                        Bundle()
                    )
                    dismiss()
                }
                .setNegativeButton(R.string.button_cancel, null)
                .create()
        }

    }

    private val accountId: Long
        get() = requireArguments().getLong(ACCOUNT_ID)

    private fun isEnabledNavigationCategoryById(
        list: List<DrawerCategory>,
        @SwitchableCategory key: String
    ): Boolean {
        for (i in list) {
            if (i.getId() == key) {
                return i.isActive()
            }
        }
        return true
    }

    private fun initStartPagePreference(): ArrayList<SelectionItem> {
        val drawerSettings = if (Settings.get().other().is_side_navigation()) Settings.get()
            .sideDrawerSettings().categoriesOrder else Settings.get()
            .drawerSettings().categoriesOrder
        val enabledCategoriesName = ArrayList<String>()
        val enabledCategoriesValues = ArrayList<String>()
        enabledCategoriesName.add(getString(R.string.last_closed_page))
        enabledCategoriesValues.add("last_closed")
        if (isEnabledNavigationCategoryById(drawerSettings, SwitchableCategory.FRIENDS)) {
            enabledCategoriesName.add(getString(R.string.friends))
            enabledCategoriesValues.add("1")
        }
        enabledCategoriesName.add(getString(R.string.dialogs))
        enabledCategoriesValues.add("2")
        enabledCategoriesName.add(getString(R.string.feed))
        enabledCategoriesValues.add("3")
        enabledCategoriesName.add(getString(R.string.drawer_feedback))
        enabledCategoriesValues.add("4")
        if (isEnabledNavigationCategoryById(drawerSettings, SwitchableCategory.GROUPS)) {
            enabledCategoriesName.add(getString(R.string.groups))
            enabledCategoriesValues.add("5")
        }
        if (isEnabledNavigationCategoryById(drawerSettings, SwitchableCategory.PHOTOS)) {
            enabledCategoriesName.add(getString(R.string.photos))
            enabledCategoriesValues.add("6")
        }
        if (isEnabledNavigationCategoryById(drawerSettings, SwitchableCategory.VIDEOS)) {
            enabledCategoriesName.add(getString(R.string.videos))
            enabledCategoriesValues.add("7")
        }
        if (isEnabledNavigationCategoryById(drawerSettings, SwitchableCategory.MUSIC)) {
            enabledCategoriesName.add(getString(R.string.music))
            enabledCategoriesValues.add("8")
        }
        if (isEnabledNavigationCategoryById(drawerSettings, SwitchableCategory.DOCS)) {
            enabledCategoriesName.add(getString(R.string.attachment_documents))
            enabledCategoriesValues.add("9")
        }
        if (isEnabledNavigationCategoryById(drawerSettings, SwitchableCategory.FAVES)) {
            enabledCategoriesName.add(getString(R.string.bookmarks))
            enabledCategoriesValues.add("10")
        }
        enabledCategoriesName.add(getString(R.string.search))
        enabledCategoriesValues.add("11")
        if (isEnabledNavigationCategoryById(drawerSettings, SwitchableCategory.NEWSFEED_COMMENTS)) {
            enabledCategoriesName.add(getString(R.string.drawer_newsfeed_comments))
            enabledCategoriesValues.add("12")
        }
        enabledCategoriesName.add(getString(R.string.profile))
        enabledCategoriesValues.add("13")
        return selItems(
            enabledCategoriesName.toTypedArray(),
            enabledCategoriesValues.toTypedArray()
        )
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.PREFERENCES)
        val actionBar = ActivityUtils.supportToolbarFor(this)
        if (actionBar != null) {
            if (preferencesAdapter?.currentScreen?.key == "root" || preferencesAdapter?.currentScreen?.title.isNullOrEmpty() && (preferencesAdapter?.currentScreen?.titleRes == DEFAULT_RES_ID || preferencesAdapter?.currentScreen?.titleRes == 0)) {
                actionBar.setTitle(R.string.settings)
            } else if (preferencesAdapter?.currentScreen?.titleRes != DEFAULT_RES_ID && preferencesAdapter?.currentScreen?.titleRes != 0) {
                preferencesAdapter?.currentScreen?.titleRes?.let { actionBar.setTitle(it) }
            } else {
                actionBar.title = preferencesAdapter?.currentScreen?.title
            }
            actionBar.subtitle = null
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_SETTINGS)
        }
        if (requireActivity() is UpdatableNavigation) {
            (requireActivity() as UpdatableNavigation).onUpdateNavigation()
        }
        searchView?.visibility =
            if (preferencesAdapter?.currentScreen?.getSearchQuery() == null) View.VISIBLE else View.GONE
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    class PlayerBackgroundDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val view =
                View.inflate(requireActivity(), R.layout.entry_player_background, null)
            val enabledRotation: MaterialSwitch = view.findViewById(R.id.enabled_anim)
            val invertRotation: MaterialSwitch =
                view.findViewById(R.id.edit_invert_rotation)
            val fadeSaturation: MaterialSwitch =
                view.findViewById(R.id.edit_fade_saturation)
            val rotationSpeed = view.findViewById<SeekBar>(R.id.edit_rotation_speed)
            val zoom = view.findViewById<SeekBar>(R.id.edit_zoom)
            val blur = view.findViewById<SeekBar>(R.id.edit_blur)
            val textRotationSpeed: MaterialTextView =
                view.findViewById(R.id.text_rotation_speed)
            val textZoom: MaterialTextView = view.findViewById(R.id.text_zoom)
            val textBlur: MaterialTextView = view.findViewById(R.id.text_blur)
            zoom.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textZoom.text = getString(R.string.rotate_scale, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            rotationSpeed.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textRotationSpeed.text = getString(R.string.rotate_speed, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
            blur.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textBlur.text = getString(R.string.player_blur, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            val settings = Settings.get()
                .other().playerCoverBackgroundSettings
            enabledRotation.isChecked = settings.enabled_rotation
            invertRotation.isChecked = settings.invert_rotation
            fadeSaturation.isChecked = settings.fade_saturation
            blur.progress = settings.blur
            rotationSpeed.progress = (settings.rotation_speed * 10).toInt()
            zoom.progress = ((settings.zoom - 1) * 10).toInt()
            textZoom.text =
                getString(R.string.rotate_scale, ((settings.zoom - 1) * 10).toInt())
            textRotationSpeed.text =
                getString(R.string.rotate_speed, (settings.rotation_speed * 10).toInt())
            textBlur.text = getString(R.string.player_blur, settings.blur)
            return MaterialAlertDialogBuilder(requireActivity())
                .setView(view)
                .setCancelable(true)
                .setNegativeButton(R.string.button_cancel, null)
                .setNeutralButton(R.string.set_default) { _: DialogInterface?, _: Int ->
                    Settings.get()
                        .other().playerCoverBackgroundSettings =
                        PlayerCoverBackgroundSettings().set_default()
                    parentFragmentManager.setFragmentResult(
                        PreferencesExtra.RECREATE_ACTIVITY_REQUEST,
                        Bundle()
                    )
                    dismiss()
                }
                .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                    val st = PlayerCoverBackgroundSettings()
                    st.blur = blur.progress
                    st.invert_rotation = invertRotation.isChecked
                    st.fade_saturation = fadeSaturation.isChecked
                    st.enabled_rotation = enabledRotation.isChecked
                    st.rotation_speed = rotationSpeed.progress.toFloat() / 10
                    st.zoom = zoom.progress.toFloat() / 10 + 1f
                    Settings.get()
                        .other().playerCoverBackgroundSettings = st
                    parentFragmentManager.setFragmentResult(
                        PreferencesExtra.RECREATE_ACTIVITY_REQUEST,
                        Bundle()
                    )
                    dismiss()
                }
                .create()
        }
    }

    class LocalMediaServerDialog : DialogFragment() {
        @SuppressLint("CheckResult")
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val view = View.inflate(
                requireActivity(),
                dev.ragnarok.fenrir_common.R.layout.entry_local_server,
                null
            )
            val url: TextInputEditText = view.findViewById(dev.ragnarok.fenrir_common.R.id.edit_url)
            val password: TextInputEditText =
                view.findViewById(dev.ragnarok.fenrir_common.R.id.edit_password)
            val enabled: MaterialSwitch =
                view.findViewById(dev.ragnarok.fenrir_common.R.id.enabled_server)
            val settings = Settings.get().other().localServer
            url.setText(settings.url)
            password.setText(settings.password)
            enabled.isChecked = settings.enabled

            view.findViewById<MaterialButton>(dev.ragnarok.fenrir_common.R.id.reboot_pc_win)
                .setOnClickListener {
                    Includes.networkInterfaces.localServerApi().rebootPC("win")
                        .fromIOToMain()
                        .subscribe({
                            createCustomToast(requireActivity()).showToastSuccessBottom(R.string.success)
                        }, { createCustomToast(requireActivity()).showToastThrowable(it) })
                }

            view.findViewById<MaterialButton>(dev.ragnarok.fenrir_common.R.id.reboot_pc_linux)
                .setOnClickListener {
                    Includes.networkInterfaces.localServerApi().rebootPC("linux")
                        .fromIOToMain()
                        .subscribe({
                            createCustomToast(requireActivity()).showToastSuccessBottom(R.string.success)
                        }, { createCustomToast(requireActivity()).showToastThrowable(it) })
                }

            return MaterialAlertDialogBuilder(requireActivity())
                .setView(view)
                .setCancelable(true)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                    val enabledVal = enabled.isChecked
                    val urlVal = url.editableText.toString()
                    val passVal = password.editableText.toString()
                    if (enabledVal && (urlVal.isEmpty() || passVal.isEmpty())) {
                        return@setPositiveButton
                    }
                    val srv = LocalServerSettings()
                    srv.enabled = enabledVal
                    srv.password = passVal
                    srv.url = urlVal
                    Settings.get().other().localServer = srv
                    Includes.proxySettings.broadcastUpdate(null)
                }
                .create()
        }

    }

    class SlidrEditDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val view = View.inflate(requireActivity(), R.layout.entry_slidr_settings, null)

            val verticalSensitive = view.findViewById<SeekBar>(R.id.edit_vertical_sensitive)
            val horizontalSensitive =
                view.findViewById<SeekBar>(R.id.edit_horizontal_sensitive)
            val textHorizontalSensitive: MaterialTextView =
                view.findViewById(R.id.text_horizontal_sensitive)
            val textVerticalSensitive: MaterialTextView =
                view.findViewById(R.id.text_vertical_sensitive)

            val verticalVelocityThreshold =
                view.findViewById<SeekBar>(R.id.edit_vertical_velocity_threshold)
            val horizontalVelocityThreshold =
                view.findViewById<SeekBar>(R.id.edit_horizontal_velocity_threshold)
            val textHorizontalVelocityThreshold: MaterialTextView =
                view.findViewById(R.id.text_horizontal_velocity_threshold)
            val textVerticalVelocityThreshold: MaterialTextView =
                view.findViewById(R.id.text_vertical_velocity_threshold)

            val verticalDistanceThreshold =
                view.findViewById<SeekBar>(R.id.edit_vertical_distance_threshold)
            val horizontalDistanceThreshold =
                view.findViewById<SeekBar>(R.id.edit_horizontal_distance_threshold)
            val textHorizontalDistanceThreshold: MaterialTextView =
                view.findViewById(R.id.text_horizontal_distance_threshold)
            val textVerticalDistanceThreshold: MaterialTextView =
                view.findViewById(R.id.text_vertical_distance_threshold)

            verticalSensitive.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textVerticalSensitive.text =
                        getString(R.string.slidr_sensitive, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            horizontalSensitive.setOnSeekBarChangeListener(object :
                OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textHorizontalSensitive.text =
                        getString(R.string.slidr_sensitive, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
            verticalVelocityThreshold.setOnSeekBarChangeListener(object :
                OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textVerticalVelocityThreshold.text =
                        getString(R.string.slidr_velocity_threshold, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            horizontalVelocityThreshold.setOnSeekBarChangeListener(object :
                OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textHorizontalVelocityThreshold.text =
                        getString(R.string.slidr_velocity_threshold, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
            verticalDistanceThreshold.setOnSeekBarChangeListener(object :
                OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textVerticalDistanceThreshold.text =
                        getString(R.string.slidr_distance_threshold, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            horizontalDistanceThreshold.setOnSeekBarChangeListener(object :
                OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textHorizontalDistanceThreshold.text =
                        getString(R.string.slidr_distance_threshold, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
            val settings = Settings.get()
                .other().slidrSettings
            verticalSensitive.progress = (settings.vertical_sensitive * 100).toInt()
            horizontalSensitive.progress = (settings.horizontal_sensitive * 100).toInt()

            textHorizontalSensitive.text = getString(
                R.string.slidr_sensitive,
                (settings.horizontal_sensitive * 100).toInt()
            )
            textVerticalSensitive.text =
                getString(
                    R.string.slidr_sensitive,
                    (settings.vertical_sensitive * 100).toInt()
                )

            verticalVelocityThreshold.progress =
                (settings.vertical_velocity_threshold * 10).toInt()
            horizontalVelocityThreshold.progress =
                (settings.horizontal_velocity_threshold * 10).toInt()

            textHorizontalVelocityThreshold.text = getString(
                R.string.slidr_velocity_threshold,
                (settings.horizontal_velocity_threshold * 10).toInt()
            )
            textVerticalVelocityThreshold.text = getString(
                R.string.slidr_velocity_threshold,
                (settings.vertical_velocity_threshold * 10).toInt()
            )

            verticalDistanceThreshold.progress =
                (settings.vertical_distance_threshold * 100).toInt()
            horizontalDistanceThreshold.progress =
                (settings.horizontal_distance_threshold * 100).toInt()

            textHorizontalDistanceThreshold.text = getString(
                R.string.slidr_distance_threshold,
                (settings.horizontal_distance_threshold * 100).toInt()
            )
            textVerticalDistanceThreshold.text = getString(
                R.string.slidr_distance_threshold,
                (settings.vertical_distance_threshold * 100).toInt()
            )

            return MaterialAlertDialogBuilder(requireActivity())
                .setView(view)
                .setCancelable(true)
                .setNegativeButton(R.string.button_cancel, null)
                .setNeutralButton(R.string.set_default) { _: DialogInterface?, _: Int ->
                    Settings.get()
                        .other().slidrSettings =
                        SlidrSettings().set_default()
                    parentFragmentManager.setFragmentResult(
                        PreferencesExtra.RECREATE_ACTIVITY_REQUEST,
                        Bundle()
                    )
                    dismiss()
                }
                .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                    val st = SlidrSettings()
                    st.horizontal_sensitive = horizontalSensitive.progress.toFloat() / 100
                    st.vertical_sensitive = verticalSensitive.progress.toFloat() / 100

                    st.horizontal_velocity_threshold =
                        horizontalVelocityThreshold.progress.toFloat() / 10
                    st.vertical_velocity_threshold =
                        verticalVelocityThreshold.progress.toFloat() / 10

                    st.horizontal_distance_threshold =
                        horizontalDistanceThreshold.progress.toFloat() / 100
                    st.vertical_distance_threshold =
                        verticalDistanceThreshold.progress.toFloat() / 100
                    Settings.get()
                        .other().slidrSettings = st
                    parentFragmentManager.setFragmentResult(
                        PreferencesExtra.RECREATE_ACTIVITY_REQUEST,
                        Bundle()
                    )
                    dismiss()
                }.create()
        }
    }

    override fun onDestroy() {
        sleepDataDisposable.dispose()
        disposables.dispose()
        preferencesView?.let { preferencesAdapter?.stopObserveScrollPosition(it) }
        preferencesAdapter?.onScreenChangeListener = null
        preferencesView?.adapter = null
        super.onDestroy()
    }

    companion object {
        const val KEY_DEFAULT_CATEGORY = "default_category"
        const val KEY_AVATAR_STYLE = "avatar_style"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_NIGHT_SWITCH = "night_switch"
        private const val KEY_NOTIFICATION = "notifications"
        private const val KEY_SECURITY = "security"
        private const val KEY_DRAWER_ITEMS = "drawer_categories"
        private const val KEY_SIDE_DRAWER_ITEMS = "side_drawer_categories"


        fun buildArgs(accountId: Long): Bundle {
            val args = Bundle()
            args.putLong(ACCOUNT_ID, accountId)
            return args
        }


        fun newInstance(args: Bundle?): PreferencesFragment {
            val fragment = PreferencesFragment()
            fragment.arguments = args
            return fragment
        }

        fun getDrawerBackgroundFile(context: Context, light: Boolean): File {
            return File(context.filesDir, if (light) "chat_light.jpg" else "chat_dark.jpg")
        }


        fun cleanCache(context: Context, notify: Boolean) {
            try {
                clear_cache()
                var cache = File(context.cacheDir, "notif-cache")
                if (cache.exists() && cache.isDirectory) {
                    val children = cache.list()
                    if (children != null) {
                        for (child in children) {
                            val rem = File(cache, child)
                            if (rem.isFile) {
                                rem.delete()
                            }
                        }
                    }
                }
                cache = File(context.cacheDir, "covers-cache")
                if (cache.exists() && cache.isDirectory) {
                    val children = cache.list()
                    if (children != null) {
                        for (child in children) {
                            val rem = File(cache, child)
                            if (rem.isFile) {
                                rem.delete()
                            }
                        }
                    }
                }
                cache = File(context.cacheDir, "lottie_cache")
                if (cache.exists() && cache.isDirectory) {
                    val children = cache.list()
                    if (children != null) {
                        for (child in children) {
                            val rem = File(cache, child)
                            if (rem.isFile) {
                                rem.delete()
                            }
                        }
                    }
                }
                cache = File(context.cacheDir, "video_network_cache")
                if (cache.exists() && cache.isDirectory) {
                    val children = cache.list()
                    if (children != null) {
                        for (child in children) {
                            val rem = File(cache, child)
                            if (rem.isFile) {
                                rem.delete()
                            }
                        }
                    }
                }
                cache = File(context.cacheDir, "video_resource_cache")
                if (cache.exists() && cache.isDirectory) {
                    val children = cache.list()
                    if (children != null) {
                        for (child in children) {
                            val rem = File(cache, child)
                            if (rem.isFile) {
                                rem.delete()
                            }
                        }
                    }
                }
                AudioRecordWrapper.getRecordingDirectory(context)?.let {
                    if (it.exists() && it.isDirectory) {
                        val children = it.list()
                        if (children != null) {
                            for (child in children) {
                                val rem = File(it, child)
                                if (rem.isFile) {
                                    rem.delete()
                                }
                            }
                        }
                    }
                }
                if (notify) createCustomToast(context).showToast(R.string.success)
            } catch (e: Exception) {
                e.printStackTrace()
                if (notify) createCustomToast(context).showToastError(e.localizedMessage)
            }
        }

        internal fun checkBitmap(bitmap: Bitmap?): Bitmap? {
            bitmap ?: return null
            if (bitmap.width <= 0 || bitmap.height <= 0 || bitmap.width <= 4000 && bitmap.height <= 4000) {
                return bitmap
            }
            var mWidth = bitmap.width
            var mHeight = bitmap.height
            val mCo = mHeight.coerceAtMost(mWidth).toFloat() / mHeight.coerceAtLeast(mWidth)
            if (mWidth > mHeight) {
                mWidth = 4000
                mHeight = (4000 * mCo).toInt()
            } else {
                mHeight = 4000
                mWidth = (4000 * mCo).toInt()
            }
            if (mWidth <= 0 || mHeight <= 0) {
                return bitmap
            }
            val tmp = Bitmap.createScaledBitmap(bitmap, mWidth, mHeight, true)
            bitmap.recycle()
            return tmp
        }
    }
}
