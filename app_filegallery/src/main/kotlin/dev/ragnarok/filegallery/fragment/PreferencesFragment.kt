package dev.ragnarok.filegallery.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.squareup.picasso3.BitmapSafeResize
import de.maxr1998.modernpreferences.AbsPreferencesFragment
import de.maxr1998.modernpreferences.PreferenceScreen
import de.maxr1998.modernpreferences.PreferencesAdapter
import de.maxr1998.modernpreferences.PreferencesExtra
import de.maxr1998.modernpreferences.helpers.*
import de.maxr1998.modernpreferences.preferences.CustomTextPreference
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.filegallery.*
import dev.ragnarok.filegallery.Constants.forceDeveloperMode
import dev.ragnarok.filegallery.Includes.provideApplicationContext
import dev.ragnarok.filegallery.activity.ActivityFeatures
import dev.ragnarok.filegallery.activity.ActivityUtils
import dev.ragnarok.filegallery.activity.EnterPinActivity
import dev.ragnarok.filegallery.activity.FileManagerSelectActivity
import dev.ragnarok.filegallery.api.adapters.AbsDtoAdapter.Companion.asJsonObjectSafe
import dev.ragnarok.filegallery.api.adapters.AbsDtoAdapter.Companion.asPrimitiveSafe
import dev.ragnarok.filegallery.api.adapters.AbsDtoAdapter.Companion.hasObject
import dev.ragnarok.filegallery.listener.BackPressCallback
import dev.ragnarok.filegallery.listener.CanBackPressedCallback
import dev.ragnarok.filegallery.listener.OnSectionResumeCallback
import dev.ragnarok.filegallery.listener.UpdatableNavigation
import dev.ragnarok.filegallery.model.LocalServerSettings
import dev.ragnarok.filegallery.model.PlayerCoverBackgroundSettings
import dev.ragnarok.filegallery.model.SectionItem
import dev.ragnarok.filegallery.model.SlidrSettings
import dev.ragnarok.filegallery.picasso.PicassoInstance.Companion.clear_cache
import dev.ragnarok.filegallery.place.PlaceFactory
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorPrimary
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorSecondary
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.settings.backup.SettingsBackup
import dev.ragnarok.filegallery.util.CoverSafeResize
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.Utils.getAppVersionName
import dev.ragnarok.filegallery.util.Utils.safelyClose
import dev.ragnarok.filegallery.util.coroutines.CancelableJob
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.toMain
import dev.ragnarok.filegallery.util.toast.CustomSnackbars
import dev.ragnarok.filegallery.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.filegallery.view.MySearchView
import dev.ragnarok.filegallery.view.natives.animation.ThorVGLottieView
import kotlinx.serialization.json.*
import kotlinx.serialization.prefs.Preferences
import okio.buffer
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.io.FilenameFilter

class PreferencesFragment : AbsPreferencesFragment(), PreferencesAdapter.OnScreenChangeListener,
    BackPressCallback, CanBackPressedCallback {
    private var preferencesView: RecyclerView? = null
    private var layoutManager: LinearLayoutManager? = null
    private var searchView: MySearchView? = null
    private var sleepDataDisposable = CancelableJob()
    private val SEARCH_DELAY = 2000
    override val keyInstanceState: String = "root_preferences"

    private val musicDir = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            PreferenceScreen.getPreferences(requireActivity())
                .edit {
                    putString(
                        "music_dir",
                        result.data?.getStringExtra(Extra.PATH)
                    )
                }
            preferencesAdapter?.applyToPreference("music_dir") { ss -> (ss as CustomTextPreference).reload() }
        }
    }
    private val photoDir = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            PreferenceScreen.getPreferences(requireActivity())
                .edit {
                    putString(
                        "photo_dir",
                        result.data?.getStringExtra(Extra.PATH)
                    )
                }
            preferencesAdapter?.applyToPreference("photo_dir") { ss -> (ss as CustomTextPreference).reload() }
        }
    }

    private val videoDir = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            PreferenceScreen.getPreferences(requireActivity())
                .edit {
                    putString(
                        "video_dir",
                        result.data?.getStringExtra(Extra.PATH)
                    )
                }
            preferencesAdapter?.applyToPreference("video_dir") { ss -> (ss as CustomTextPreference).reload() }
        }
    }

    private val exportSettings = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val file = File(
                    result.data?.getStringExtra(Extra.PATH),
                    "file_gallery_settings_backup.json"
                )
                val root = JsonObjectBuilder()
                val app = JsonObjectBuilder()
                app.put("version", getAppVersionName(requireActivity()))
                app.put("settings_format", Constants.EXPORT_SETTINGS_FORMAT)
                root.put("app", app.build())
                val settings = SettingsBackup().doBackup()
                root.put("settings", settings)
                val bytes = kJsonPretty.printJsonElement(root.build()).toByteArray(
                    Charsets.UTF_8
                )
                val out = FileOutputStream(file)
                val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                out.write(bom)
                out.write(bytes)
                out.flush()
                provideApplicationContext().sendBroadcast(
                    @Suppress("deprecation")
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(file)
                    )
                )
                createCustomToast(requireActivity(), view)?.showToast(
                    R.string.success,
                    file.absolutePath
                )

            } catch (e: Exception) {
                createCustomToast(requireActivity(), view)?.showToastThrowable(e)
            }
        }
    }

    private val importSettings = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val file =
                    File(
                        result.data?.getStringExtra(Extra.PATH) ?: return@registerForActivityResult
                    )
                if (file.exists()) {
                    val objApp = kJson.parseToJsonElement(file.source().buffer()).jsonObject
                    if (objApp["app"]?.asJsonObjectSafe?.get("settings_format")?.asPrimitiveSafe?.intOrNull != Constants.EXPORT_SETTINGS_FORMAT) {
                        createCustomToast(requireActivity(), view)?.setDuration(Toast.LENGTH_LONG)
                            ?.showToastError(R.string.wrong_settings_format)
                        return@registerForActivityResult
                    }
                    if (hasObject(objApp, "settings")) {
                        SettingsBackup().doRestore(objApp["settings"]?.jsonObject)
                        createCustomToast(requireActivity(), view)?.setDuration(Toast.LENGTH_LONG)
                            ?.showToastSuccessBottom(
                                R.string.need_restart
                            )
                    }
                }
                createCustomToast(requireActivity(), view)?.showToast(R.string.success)
            } catch (e: Exception) {
                createCustomToast(requireActivity(), view)?.showToastThrowable(e)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root =
            inflater.inflate(R.layout.preference_file_gallery_list_fragment, container, false)
        searchView = root.findViewById(R.id.searchview)
        searchView?.setRightButtonVisibility(false)
        searchView?.setLeftIcon(R.drawable.magnify)
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
            it.setOnBackButtonClickListener(
                object : MySearchView.OnBackButtonClickListener {
                    override fun onBackButtonClick() {
                        if (it.text.nonNullNoEmpty() && it.text?.trimmedNonNullNoEmpty() == true) {
                            preferencesAdapter?.findPreferences(
                                requireActivity(),
                                (it.text ?: return).toString(),
                                root
                            )
                        }
                    }
                }
            )
            it.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    sleepDataDisposable.cancel()
                    if (query.nonNullNoEmpty() && query.trimmedNonNullNoEmpty()) {
                        preferencesAdapter?.findPreferences(requireActivity(), query, root)
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    sleepDataDisposable.cancel()
                    sleepDataDisposable.set(
                        delayTaskFlow(SEARCH_DELAY.toLong())
                            .toMain {
                                if (newText.nonNullNoEmpty() && newText.trimmedNonNullNoEmpty()) {
                                    preferencesAdapter?.findPreferences(
                                        requireActivity(),
                                        newText,
                                        root
                                    )
                                }
                            })
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
            if (screen.key == "root" || screen.title.isEmpty() && screen.titleRes == DISABLED_RESOURCE_ID) {
                actionBar.setTitle(R.string.settings)
            } else if (screen.titleRes != DISABLED_RESOURCE_ID) {
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

    private fun createRootScreen() = screen(requireActivity()) {
        subScreen("general_preferences") {
            titleRes = R.string.general_settings
            iconRes = R.drawable.preferences_settings
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
            pref(KEY_APP_THEME) {
                iconRes = R.drawable.select_colored
                titleRes = R.string.choose_theme_title
                onClick {
                    PlaceFactory.getSettingsThemePlace().tryOpenWith(requireActivity())
                    true
                }
            }

            singleChoice(
                KEY_NIGHT_SWITCH,
                selItems(R.array.night_mode_names, R.array.night_mode_values),
                parentFragmentManager
            ) {
                initialSelection = "-1"
                titleRes = R.string.night_mode_title
                iconRes = R.drawable.night_mode_pref
                onSelectionChange {
                    AppCompatDelegate.setDefaultNightMode(it.toInt())
                }
            }

            switch("validate_tls") {
                defaultValue = true
                summaryRes = R.string.validate_tls_summary
                titleRes = R.string.validate_tls
            }

            switch("delete_disabled") {
                defaultValue = false
                titleRes = R.string.delete_disabled
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

            seekBar("font_size_int") {
                min = -3
                max = 9
                default = 0
                step = 1
                showTickMarks = true
                titleRes = R.string.font_size
                onSeek {
                    sleepDataDisposable.cancel()
                    sleepDataDisposable.set(
                        delayTaskFlow(1000)
                            .toMain {
                                requireActivity().recreate()
                            })
                }
            }

            switch("use_internal_downloader") {
                defaultValue = true
                summaryRes = R.string.use_internal_downloader_summary
                titleRes = R.string.use_internal_downloader
            }

            switch("video_controller_to_decor") {
                defaultValue = false
                titleRes = R.string.video_controller_to_decor
            }

            switch("video_swipes") {
                defaultValue = true
                titleRes = R.string.video_swipes
            }

            switch("download_photo_tap") {
                defaultValue = true
                titleRes = R.string.download_photo_tap
            }

            switch("show_photos_line") {
                defaultValue = true
                titleRes = R.string.show_photos_line
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

            switch("instant_photo_display") {
                defaultValue = Settings.get().main().isInstant_photo_display
                titleRes = R.string.instant_photo_display
            }

            singleChoice(
                "picasso_dispatcher",
                selItems(R.array.picasso_dispatcher_names, R.array.picasso_dispatcher_values),
                parentFragmentManager
            ) {
                initialSelection = Settings.get().main().picassoDispatcher.toString()
                titleRes = R.string.picasso_dispatcher
                onSelectionChange {
                    clear_cache()
                }
            }
        }
        pref("security") {
            titleRes = R.string.security
            iconRes = R.drawable.security_settings
            onClick {
                onSecurityClick()
                true
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

            pref("slidr_settings") {
                titleRes = R.string.slidr_settings
                onClick {
                    SlidrEditDialog().show(parentFragmentManager, "SlidrPrefs")
                    true
                }
            }

            switch("use_stop_audio") {
                defaultValue = false
                titleRes = R.string.use_stop_audio
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
                titleRes = R.string.ffmpeg_audio_codecs
            }
        }

        subScreen("download_directory") {
            iconRes = R.drawable.save_settings
            titleRes = R.string.download_directory

            customText("music_dir", parentFragmentManager) {
                titleRes = R.string.music_dir
                iconRes = R.drawable.dir_song
                onClick {
                    musicDir.launch(
                        FileManagerSelectActivity.makeFileManager(
                            requireActivity(),
                            Environment.getExternalStorageDirectory().absolutePath,
                            "dirs", null
                        )
                    )
                    true
                }
            }

            customText("photo_dir", parentFragmentManager) {
                titleRes = R.string.photo_dir
                iconRes = R.drawable.dir_photo
                onClick {
                    photoDir.launch(
                        FileManagerSelectActivity.makeFileManager(
                            requireActivity(),
                            Environment.getExternalStorageDirectory().absolutePath,
                            "dirs", null
                        )
                    )
                    true
                }
            }

            customText("video_dir", parentFragmentManager) {
                titleRes = R.string.video_dir
                iconRes = R.drawable.dir_video
                onClick {
                    videoDir.launch(
                        FileManagerSelectActivity.makeFileManager(
                            requireActivity(),
                            Environment.getExternalStorageDirectory().absolutePath,
                            "dirs", null
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

        }

        subScreen("dev_settings") {
            iconRes = R.drawable.developer_mode
            titleRes = R.string.dev_settings
            switch("developer_mode") {
                defaultValue = forceDeveloperMode
                titleRes = R.string.developer_mode
                iconRes = R.drawable.developer_mode
            }

            switch("open_folder_new_window") {
                defaultValue = false
                titleRes = R.string.open_folder_new_window
                dependency = "developer_mode"
            }

            multiLineText("videos_ext", parentFragmentManager) {
                titleRes = R.string.video_ext
                defaultValue = setOf("mp4", "avi", "mov", "mpeg")
                isSpace = true
                dependency = "developer_mode"
                onMultiLineTextChange {
                    requireActivity().recreate()
                }
            }

            multiLineText("photo_ext", parentFragmentManager) {
                titleRes = R.string.photo_ext
                isSpace = true
                dependency = "developer_mode"
                defaultValue = setOf("jpg", "jpeg", "heic", "webp", "png", "tiff")
                onMultiLineTextChange {
                    requireActivity().recreate()
                }
            }

            multiLineText("audio_ext", parentFragmentManager) {
                titleRes = R.string.audio_ext
                isSpace = true
                dependency = "developer_mode"
                defaultValue = setOf("mp3", "ogg", "flac", "opus")
                onMultiLineTextChange {
                    requireActivity().recreate()
                }
            }

            editText("max_bitmap_resolution", parentFragmentManager) {
                defaultValue = "4000"
                textInputType = InputType.TYPE_CLASS_NUMBER
                titleRes = R.string.max_bitmap_resolution
                dependency = "developer_mode"
                isTrim = true
                onTextBeforeChanged { its ->
                    var sz = -1
                    try {
                        sz = its.toString().trim().toInt()
                    } catch (_: NumberFormatException) {
                    }
                    if (BitmapSafeResize.isOverflowCanvas(sz) || sz in 0..99) {
                        return@onTextBeforeChanged false
                    } else {
                        BitmapSafeResize.setMaxResolution(sz)
                    }
                    requireActivity().recreate()
                    true
                }
            }

            editText("max_thumb_resolution", parentFragmentManager) {
                defaultValue = "384"
                textInputType = InputType.TYPE_CLASS_NUMBER
                titleRes = R.string.max_thumb_resolution
                dependency = "developer_mode"
                isTrim = true
                onTextBeforeChanged { its ->
                    var sz = -1
                    try {
                        sz = its.toString().trim().toInt()
                    } catch (_: NumberFormatException) {
                    }
                    if (CoverSafeResize.isOverflowCanvas(sz) || sz in 0..99) {
                        return@onTextBeforeChanged false
                    } else {
                        CoverSafeResize.setMaxResolution(sz)
                    }
                    true
                }
                onTextChanged {
                    cleanCache(requireActivity(), false)
                    requireActivity().recreate()
                }
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
                    cleanCache(requireActivity(), true)
                    requireActivity().recreate()
                }
            }

            singleChoice(
                "rendering_bitmap_mode",
                selItems(
                    R.array.array_rendering_mode_names,
                    R.array.array_rendering_mode_items
                ),
                parentFragmentManager
            ) {
                initialSelection = "0"
                titleRes = R.string.rendering_mode
                dependency = "developer_mode"
                visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                onSelectionChange { it ->
                    var sz = 0
                    try {
                        sz = it.trim().toInt()
                    } catch (_: NumberFormatException) {
                    }
                    BitmapSafeResize.setHardwareRendering(sz)
                    requireActivity().recreate()
                }
            }

            switch("enable_dirs_files_count") {
                defaultValue = true
                titleRes = R.string.enable_dirs_files_count
            }

            switch("compress_incoming_traffic") {
                defaultValue = true
                titleRes = R.string.compress_incoming_traffic
                onCheckedChange {
                    Utils.isCompressIncomingTraffic = it
                    Settings.get().main().updateLocalServer()
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
                    Settings.get().main().updateLocalServer()
                }
            }

            pref("export_adb") {
                titleRes = R.string.export_adb
                onClick {
                    val path: File =
                        if (Environment.getExternalStorageDirectory().isDirectory && Environment.getExternalStorageDirectory()
                                .canRead()
                        ) {
                            Environment.getExternalStorageDirectory()
                        } else {
                            File("/")
                        }
                    val ou = StringBuilder()
                    ou.append("mkdir mnt/media\r\n")
                    ou.append("mkdir mnt/media/0\r\n")
                    var countCopy = 0
                    path.list(FilenameFilter { dir, filename ->
                        val sel = File(dir, filename)
                        if (sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                "Android"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                "Fonts"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                "Documents"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                "Subtitles"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                "ColorOS"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                "Audiobooks"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                "Notifications"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                "Alarms"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                "Ringtones"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                "Podcasts"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                ".sstmp"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                ".picker_transcoded"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                ".SLOGAN"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                ".\$Trash$"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                "OplusOS"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                "ThemeStore"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                ".time"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                ".dev"
                            ).absolutePath || sel.absolutePath == File(
                                Environment.getExternalStorageDirectory(),
                                ".ext4"
                            ).absolutePath
                        ) {
                            return@FilenameFilter false
                        }
                        countCopy++
                        ou.append("adb pull -a \"/storage/emulated/0/${filename}\" \"mnt/media/0/${filename}\" > \"${filename}.txt\"\r\n")
                        true
                    })
                    ou.append("\r\n-------Total $countCopy members-------\r\n")
                    var out: FileOutputStream? = null
                    try {
                        val file = File(Environment.getExternalStorageDirectory(), "to_adb.sh")
                        file.delete()
                        val bytes = ou.toString().toByteArray(Charsets.UTF_8)
                        out = FileOutputStream(file)
                        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                        out.write(bom)
                        out.write(bytes)
                        out.flush()
                        requireActivity().sendBroadcast(
                            @Suppress("deprecation")
                            Intent(
                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.fromFile(file)
                            )
                        )
                        createCustomToast(requireActivity(), view)?.showToast(
                            R.string.success,
                            file.absolutePath
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        safelyClose(out)
                    }
                    true
                }
            }

            accentButtonPref("cache_cleaner") {
                titleRes = R.string.cache_cleaner
                onClick {
                    Includes.stores.searchQueriesStore().clearQueriesAll()
                    Includes.stores.searchQueriesStore().clearFilesAll()
                    cleanCache(requireActivity(), true)
                    requireActivity().recreate()
                    true
                }
            }
        }

        subScreen("import_export_settings") {
            iconRes = R.drawable.preferences_settings
            titleRes = R.string.import_export_settings
            pref("export_settings") {
                titleRes = R.string.export_settings
                onClick {
                    exportSettings.launch(
                        FileManagerSelectActivity.makeFileManager(
                            requireActivity(),
                            Environment.getExternalStorageDirectory().absolutePath,
                            "dirs", null
                        )
                    )
                    true
                }
            }
            pref("import_settings") {
                titleRes = R.string.import_settings
                onClick {
                    importSettings.launch(
                        FileManagerSelectActivity.makeFileManager(
                            requireActivity(),
                            Environment.getExternalStorageDirectory().absolutePath,
                            "json", null
                        )
                    )
                    true
                }
            }
        }
        accentButtonPref("local_media_server") {
            iconRes = R.drawable.web_settings
            titleRes = R.string.local_media_server
            onClick {
                LocalMediaServerDialog().show(parentFragmentManager, "LocalMediaServer")
                true
            }
        }
        pref("reset_settings") {
            titleRes = R.string.reset_settings
            iconRes = R.drawable.refresh_settings
            onClick {
                CustomSnackbars.createCustomSnackbars(view)
                    ?.setDurationSnack(Snackbar.LENGTH_LONG)
                    ?.themedSnack(R.string.reset_settings)
                    ?.setAction(
                        R.string.button_yes
                    ) {
                        val preferences =
                            Preferences(PreferenceScreen.getPreferences(provideApplicationContext()))
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
        pref("version") {
            iconRes = R.drawable.app_info_settings
            titleRes = R.string.app_name
            summary = BuildConfig.VERSION_NAME
            onClick {
                val view = View.inflate(requireActivity(), R.layout.dialog_about_us, null)
                val anim: ThorVGLottieView = view.findViewById(R.id.lottie_animation)
                if (FenrirNative.isNativeLoaded) {
                    anim.fromRes(
                        R.raw.fenrir,
                        intArrayOf(
                            0x333333,
                            getColorPrimary(requireActivity()),
                            0x777777,
                            getColorSecondary(requireActivity())
                        )
                    )
                    anim.startAnimation()
                }
                MaterialAlertDialogBuilder(requireActivity())
                    .setView(view)
                    .show()
                true
            }
        }
    }

    private val requestPin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            PlaceFactory.securitySettingsPlace.tryOpenWith(requireActivity())
        }
    }

    private fun onSecurityClick() {
        if (Settings.get().security().hasPinHash) {
            requestPin.launch(Intent(requireActivity(), EnterPinActivity::class.java))
        } else {
            PlaceFactory.securitySettingsPlace.tryOpenWith(requireActivity())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).setSupportActionBar(view.findViewById(R.id.toolbar))
    }

    override fun onResume() {
        super.onResume()
        val actionBar = ActivityUtils.supportToolbarFor(this)
        if (actionBar != null) {
            if (preferencesAdapter?.currentScreen?.key == "root" || preferencesAdapter?.currentScreen?.title.isNullOrEmpty() && (preferencesAdapter?.currentScreen?.titleRes == DISABLED_RESOURCE_ID || preferencesAdapter?.currentScreen?.titleRes == 0)
            ) {
                actionBar.setTitle(R.string.settings)
            } else if (preferencesAdapter?.currentScreen?.titleRes != DISABLED_RESOURCE_ID && preferencesAdapter?.currentScreen?.titleRes != 0) {
                preferencesAdapter?.currentScreen?.titleRes?.let { actionBar.setTitle(it) }
            } else {
                actionBar.title = preferencesAdapter?.currentScreen?.title
            }
            actionBar.subtitle = null
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(SectionItem.SETTINGS)
        }
        if (requireActivity() is UpdatableNavigation) {
            (requireActivity() as UpdatableNavigation).onUpdateNavigation()
        }
        searchView?.visibility =
            if (preferencesAdapter?.currentScreen?.getSearchQuery() == null) View.VISIBLE else View.GONE
        ActivityFeatures.Builder()
            .begin()
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
            val rotationSpeed = view.findViewById<Slider>(R.id.edit_rotation_speed)
            val zoom = view.findViewById<Slider>(R.id.edit_zoom)
            val blur = view.findViewById<Slider>(R.id.edit_blur)
            val textRotationSpeed: MaterialTextView =
                view.findViewById(R.id.text_rotation_speed)
            val textZoom: MaterialTextView = view.findViewById(R.id.text_zoom)
            val textBlur: MaterialTextView = view.findViewById(R.id.text_blur)
            zoom.addOnChangeListener { slider, value, fromUser ->
                textZoom.text = getString(R.string.rotate_scale, value.toInt())
            }
            rotationSpeed.addOnChangeListener { slider, value, fromUser ->
                textRotationSpeed.text = getString(R.string.rotate_speed, value.toInt())
            }
            blur.addOnChangeListener { slider, value, fromUser ->
                textBlur.text = getString(R.string.player_blur, value.toInt())
            }
            val settings = Settings.get()
                .main().playerCoverBackgroundSettings
            enabledRotation.isChecked = settings.enabled_rotation
            invertRotation.isChecked = settings.invert_rotation
            fadeSaturation.isChecked = settings.fade_saturation
            blur.value = settings.blur.toFloat()
            rotationSpeed.value = (settings.rotation_speed * 10).toInt().toFloat()
            zoom.value = ((settings.zoom - 1) * 10).toInt().toFloat()
            textZoom.text =
                getString(R.string.rotate_scale, ((settings.zoom - 1) * 10).toInt())
            textRotationSpeed.text =
                getString(R.string.rotate_speed, (settings.rotation_speed * 10).toInt())
            textBlur.text = getString(R.string.player_blur, settings.blur)
            return MaterialAlertDialogBuilder(requireActivity())
                .setView(view)
                .setCancelable(true)
                .setNegativeButton(R.string.button_cancel, null)
                .setNeutralButton(R.string.set_default) { _, _ ->
                    Settings.get()
                        .main()
                        .setPlayerCoverBackgroundSettings(PlayerCoverBackgroundSettings().set_default())
                    parentFragmentManager.setFragmentResult(
                        PreferencesExtra.RECREATE_ACTIVITY_REQUEST,
                        Bundle()
                    )
                    dismiss()
                }
                .setPositiveButton(R.string.button_ok) { _, _ ->
                    val st = PlayerCoverBackgroundSettings()
                    st.blur = blur.value.toInt()
                    st.invert_rotation = invertRotation.isChecked
                    st.fade_saturation = fadeSaturation.isChecked
                    st.enabled_rotation = enabledRotation.isChecked
                    st.rotation_speed = rotationSpeed.value / 10
                    st.zoom = zoom.value / 10 + 1f
                    Settings.get()
                        .main().setPlayerCoverBackgroundSettings(st)
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
            val view = View.inflate(requireActivity(), R.layout.entry_local_server, null)
            val url: TextInputEditText = view.findViewById(R.id.edit_url)
            val password: TextInputEditText = view.findViewById(R.id.edit_password)
            val enabled: MaterialSwitch = view.findViewById(R.id.enabled_server)
            val enabled_audio_local_sync: MaterialSwitch =
                view.findViewById(R.id.enabled_audio_local_sync)
            val settings = Settings.get().main().localServer
            url.setText(settings.url)
            password.setText(settings.password)
            enabled.isChecked = settings.enabled
            enabled_audio_local_sync.isChecked = settings.enabled_audio_local_sync

            view.findViewById<MaterialButton>(R.id.reboot_pc_win).setOnClickListener {
                Includes.networkInterfaces.localServerApi().rebootPC("win")
                    .fromIOToMain({
                        createCustomToast(
                            requireActivity(),
                            view
                        )?.showToastSuccessBottom(R.string.success)
                    }, { createCustomToast(requireActivity(), view)?.showToastThrowable(it) })
            }

            view.findViewById<MaterialButton>(R.id.reboot_pc_linux).setOnClickListener {
                Includes.networkInterfaces.localServerApi().rebootPC("linux")
                    .fromIOToMain({
                        createCustomToast(
                            requireActivity(),
                            view
                        )?.showToastSuccessBottom(R.string.success)
                    }, { createCustomToast(requireActivity(), view)?.showToastThrowable(it) })
            }

            return MaterialAlertDialogBuilder(requireActivity())
                .setView(view)
                .setCancelable(true)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_ok) { _, _ ->
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
                    srv.enabled_audio_local_sync = enabled_audio_local_sync.isChecked
                    Settings.get().main().setLocalServer(srv)
                }
                .create()
        }

    }

    class SlidrEditDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val view = View.inflate(requireActivity(), R.layout.entry_slidr_settings, null)

            val verticalSensitive =
                view.findViewById<Slider>(R.id.edit_vertical_sensitive)
            val horizontalSensitive =
                view.findViewById<Slider>(R.id.edit_horizontal_sensitive)
            val textHorizontalSensitive: MaterialTextView =
                view.findViewById(R.id.text_horizontal_sensitive)
            val textVerticalSensitive: MaterialTextView =
                view.findViewById(R.id.text_vertical_sensitive)

            val verticalVelocityThreshold =
                view.findViewById<Slider>(R.id.edit_vertical_velocity_threshold)
            val horizontalVelocityThreshold =
                view.findViewById<Slider>(R.id.edit_horizontal_velocity_threshold)
            val textHorizontalVelocityThreshold: MaterialTextView =
                view.findViewById(R.id.text_horizontal_velocity_threshold)
            val textVerticalVelocityThreshold: MaterialTextView =
                view.findViewById(R.id.text_vertical_velocity_threshold)

            val verticalDistanceThreshold =
                view.findViewById<Slider>(R.id.edit_vertical_distance_threshold)
            val horizontalDistanceThreshold =
                view.findViewById<Slider>(R.id.edit_horizontal_distance_threshold)
            val textHorizontalDistanceThreshold: MaterialTextView =
                view.findViewById(R.id.text_horizontal_distance_threshold)
            val textVerticalDistanceThreshold: MaterialTextView =
                view.findViewById(R.id.text_vertical_distance_threshold)

            verticalSensitive.addOnChangeListener { slider, value, fromUser ->
                if (fromUser && value < 20) {
                    verticalSensitive.value = 20.0f
                    textVerticalSensitive.text = getString(R.string.slidr_sensitive, 20)
                } else {
                    textVerticalSensitive.text = getString(R.string.slidr_sensitive, value.toInt())
                }
            }

            horizontalSensitive.addOnChangeListener { slider, value, fromUser ->
                if (fromUser && value < 20) {
                    horizontalSensitive.value = 20.0f
                    textHorizontalSensitive.text = getString(R.string.slidr_sensitive, 20)
                } else {
                    textHorizontalSensitive.text =
                        getString(R.string.slidr_sensitive, value.toInt())
                }
            }

            verticalVelocityThreshold.addOnChangeListener { slider, value, fromUser ->
                if (fromUser && value < 4) {
                    verticalVelocityThreshold.value = 4.0f
                    textVerticalVelocityThreshold.text =
                        getString(R.string.slidr_velocity_threshold, 4)
                } else {
                    textVerticalVelocityThreshold.text =
                        getString(R.string.slidr_velocity_threshold, value.toInt())
                }
            }

            horizontalVelocityThreshold.addOnChangeListener { slider, value, fromUser ->
                if (fromUser && value < 4) {
                    horizontalVelocityThreshold.value = 4.0f
                    textHorizontalVelocityThreshold.text =
                        getString(R.string.slidr_velocity_threshold, 4)
                } else {
                    textHorizontalVelocityThreshold.text =
                        getString(R.string.slidr_velocity_threshold, value.toInt())
                }
            }

            verticalDistanceThreshold.addOnChangeListener { slider, value, fromUser ->
                if (fromUser && value < 4) {
                    verticalDistanceThreshold.value = 4.0f
                    textVerticalDistanceThreshold.text =
                        getString(R.string.slidr_distance_threshold, 4)
                } else {
                    textVerticalDistanceThreshold.text =
                        getString(R.string.slidr_distance_threshold, value.toInt())
                }
            }

            horizontalDistanceThreshold.addOnChangeListener { slider, value, fromUser ->
                if (fromUser && value < 4) {
                    horizontalDistanceThreshold.value = 4.0f
                    textHorizontalDistanceThreshold.text =
                        getString(R.string.slidr_distance_threshold, 4)
                } else {
                    textHorizontalDistanceThreshold.text =
                        getString(R.string.slidr_distance_threshold, value.toInt())
                }
            }

            val settings = Settings.get()
                .main().slidrSettings
            verticalSensitive.value = (settings.vertical_sensitive * 100).toInt().toFloat()
            horizontalSensitive.value = (settings.horizontal_sensitive * 100).toInt().toFloat()

            textHorizontalSensitive.text = getString(
                R.string.slidr_sensitive,
                (settings.horizontal_sensitive * 100).toInt()
            )
            textVerticalSensitive.text =
                getString(
                    R.string.slidr_sensitive,
                    (settings.vertical_sensitive * 100).toInt()
                )

            verticalVelocityThreshold.value =
                (settings.vertical_velocity_threshold * 10).toInt().toFloat()
            horizontalVelocityThreshold.value =
                (settings.horizontal_velocity_threshold * 10).toInt().toFloat()

            textHorizontalVelocityThreshold.text = getString(
                R.string.slidr_velocity_threshold,
                (settings.horizontal_velocity_threshold * 10).toInt()
            )
            textVerticalVelocityThreshold.text = getString(
                R.string.slidr_velocity_threshold,
                (settings.vertical_velocity_threshold * 10).toInt()
            )

            verticalDistanceThreshold.value =
                (settings.vertical_distance_threshold * 100).toInt().toFloat()
            horizontalDistanceThreshold.value =
                (settings.horizontal_distance_threshold * 100).toInt().toFloat()

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
                .setNeutralButton(R.string.set_default) { _, _ ->
                    Settings.get()
                        .main().setSlidrSettings(SlidrSettings().set_default())
                    parentFragmentManager.setFragmentResult(
                        PreferencesExtra.RECREATE_ACTIVITY_REQUEST,
                        Bundle()
                    )
                    dismiss()
                }
                .setPositiveButton(R.string.button_ok) { _, _ ->
                    val st = SlidrSettings()
                    st.horizontal_sensitive = horizontalSensitive.value.toInt().toFloat() / 100
                    st.vertical_sensitive = verticalSensitive.value.toInt().toFloat() / 100

                    st.horizontal_velocity_threshold =
                        horizontalVelocityThreshold.value.toInt().toFloat() / 10
                    st.vertical_velocity_threshold =
                        verticalVelocityThreshold.value.toInt().toFloat() / 10

                    st.horizontal_distance_threshold =
                        horizontalDistanceThreshold.value.toInt().toFloat() / 100
                    st.vertical_distance_threshold =
                        verticalDistanceThreshold.value.toInt().toFloat() / 100
                    Settings.get()
                        .main().setSlidrSettings(st)
                    parentFragmentManager.setFragmentResult(
                        PreferencesExtra.RECREATE_ACTIVITY_REQUEST,
                        Bundle()
                    )
                    dismiss()
                }.create()
        }
    }

    override fun onDestroy() {
        sleepDataDisposable.cancel()
        preferencesView?.let { preferencesAdapter?.stopObserveScrollPosition(it) }
        preferencesAdapter?.onScreenChangeListener = null
        preferencesView?.adapter = null
        super.onDestroy()
    }

    companion object {
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_NIGHT_SWITCH = "night_switch"

        fun cleanCache(context: Context, notify: Boolean) {
            try {
                clear_cache()
                var cache = File(context.cacheDir, "covers-cache")
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
                if (notify) createCustomToast(context, null)?.showToast(R.string.success)
            } catch (e: Exception) {
                e.printStackTrace()
                if (notify) createCustomToast(context, null)?.showToastThrowable(e)
            }
        }
    }
}
