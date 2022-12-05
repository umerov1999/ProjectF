package dev.ragnarok.filegallery.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Environment
import androidx.appcompat.app.AppCompatDelegate
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.Constants.forceDeveloperMode
import dev.ragnarok.filegallery.kJson
import dev.ragnarok.filegallery.model.*
import dev.ragnarok.filegallery.settings.ISettings.IMainSettings
import dev.ragnarok.filegallery.settings.theme.ThemeOverlay
import dev.ragnarok.filegallery.view.pager.Transformers_Types
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.io.File

internal class MainSettings(context: Context) : IMainSettings {
    private val app: Context = context.applicationContext
    private val localServerPublisher: PublishSubject<LocalServerSettings> = PublishSubject.create()

    override fun getFontSize(): Int {
        return try {
            getPreferences(app).getString("font_size", "0")!!.trim().toInt()
        } catch (e: Exception) {
            0
        }
    }

    override val isValidate_tls: Boolean
        get() = getPreferences(app).getBoolean("validate_tls", true)

    @ThemeOverlay
    override fun getThemeOverlay(): Int {
        return try {
            getPreferences(app).getString("theme_overlay", "0")!!.trim().toInt()
        } catch (e: Exception) {
            ThemeOverlay.OFF
        }
    }

    override fun getMainThemeKey(): String {
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

    @AppCompatDelegate.NightMode
    override fun getNightMode(): Int {
        return try {
            getPreferences(app)
                .getString("night_switch", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString())!!
                .trim()
                .toInt()
        } catch (e: Exception) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

    override fun isDeveloper_mode(): Boolean {
        return getPreferences(app).getBoolean("developer_mode", forceDeveloperMode)
    }

    override fun isEnable_dirs_files_count(): Boolean {
        return getPreferences(app).getBoolean("enable_dirs_files_count", true)
    }

    override fun isDeleteDisabled(): Boolean {
        return getPreferences(app).getBoolean("delete_disabled", false)
    }

    @Suppress("DEPRECATION")
    override fun getMusicDir(): String {
        var ret = getPreferences(app).getString("music_dir", null)
        if (ret.isNullOrEmpty() || !File(ret).exists()) {
            ret =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
            getPreferences(app).edit().putString("music_dir", ret).apply()
        }
        return ret!!
    }

    @Suppress("DEPRECATION")
    override fun getPhotoDir(): String {
        var ret = getPreferences(app).getString("photo_dir", null)
        if (ret.isNullOrEmpty() || !File(ret).exists()) {
            ret =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/Fenrir"
            getPreferences(app).edit().putString("photo_dir", ret).apply()
        }
        return ret
    }

    @Suppress("DEPRECATION")
    override fun getVideoDir(): String {
        var ret = getPreferences(app).getString("video_dir", null)
        if (ret.isNullOrEmpty() || !File(ret).exists()) {
            ret =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath + "/Fenrir"
            getPreferences(app).edit().putString("video_dir", ret).apply()
        }
        return ret
    }

    override fun getLocalServer(): LocalServerSettings {
        val ret = getPreferences(app).getString("local_media_server", null)
        return if (ret == null) {
            LocalServerSettings()
        } else {
            kJson.decodeFromString(LocalServerSettings.serializer(), ret)
        }
    }

    override fun setLocalServer(settings: LocalServerSettings) {
        getPreferences(app).edit()
            .putString(
                "local_media_server",
                kJson.encodeToString(LocalServerSettings.serializer(), settings)
            ).apply()
        localServerPublisher.onNext(settings)
    }

    override fun updateLocalServer() {
        localServerPublisher.onNext(getLocalServer())
    }

    override fun getPlayerCoverBackgroundSettings(): PlayerCoverBackgroundSettings {
        val ret =
            getPreferences(app).getString("player_background_settings_json", null)
        return if (ret == null) {
            PlayerCoverBackgroundSettings().set_default()
        } else {
            kJson.decodeFromString(PlayerCoverBackgroundSettings.serializer(), ret)
        }
    }

    override fun setPlayerCoverBackgroundSettings(settings: PlayerCoverBackgroundSettings) {
        getPreferences(app).edit()
            .putString(
                "player_background_settings_json",
                kJson.encodeToString(PlayerCoverBackgroundSettings.serializer(), settings)
            ).apply()
    }

    override fun getSlidrSettings(): SlidrSettings {
        val ret = getPreferences(app).getString("slidr_settings_json", null)
        return if (ret == null) {
            SlidrSettings().set_default()
        } else {
            kJson.decodeFromString(SlidrSettings.serializer(), ret)
        }
    }

    override fun setSlidrSettings(settings: SlidrSettings) {
        getPreferences(app).edit()
            .putString(
                "slidr_settings_json",
                kJson.encodeToString(SlidrSettings.serializer(), settings)
            ).apply()
    }

    override fun getMusicLifecycle(): Int {
        return try {
            var v = getPreferences(app).getString(
                "lifecycle_music_service",
                Constants.AUDIO_PLAYER_SERVICE_IDLE.toString()
            )!!
                .trim().toInt()
            if (v < 60000) {
                getPreferences(app).edit()
                    .putString("lifecycle_music_service", "60000").apply()
                v = 60000
            }
            v
        } catch (e: Exception) {
            Constants.AUDIO_PLAYER_SERVICE_IDLE
        }
    }

    override fun getMaxBitmapResolution(): Int {
        return try {
            getPreferences(app).getString("max_bitmap_resolution", "4000")!!.trim()
                .toInt()
        } catch (e: Exception) {
            4000
        }
    }

    override fun getMaxThumbResolution(): Int {
        return try {
            getPreferences(app).getString("max_thumb_resolution", "384")!!.trim()
                .toInt()
        } catch (e: Exception) {
            384
        }
    }

    override fun getRendering_mode(): Int {
        return try {
            getPreferences(app).getString("rendering_mode", "0")!!.trim().toInt()
        } catch (e: Exception) {
            0
        }
    }

    override fun getFFmpegPlugin(): Int {
        return try {
            getPreferences(app).getString("ffmpeg_audio_codecs", "1")!!.trim()
                .toInt()
        } catch (e: Exception) {
            1
        }
    }

    override fun isPlayer_Has_Background(): Boolean {
        return getPreferences(app).getBoolean("player_has_background", true)
    }

    override fun isShow_mini_player(): Boolean {
        return getPreferences(app).getBoolean("show_mini_player", true)
    }

    override fun observeLocalServer(): Observable<LocalServerSettings> {
        return localServerPublisher
    }

    override fun isUse_internal_downloader(): Boolean {
        return getPreferences(app).getBoolean("use_internal_downloader", true)
    }

    override fun isShow_photos_line(): Boolean {
        return getPreferences(app).getBoolean("show_photos_line", true)
    }

    override fun isDownload_photo_tap(): Boolean {
        return getPreferences(app).getBoolean("download_photo_tap", true)
    }

    override fun isAudio_round_icon(): Boolean {
        return getPreferences(app).getBoolean("audio_round_icon", true)
    }

    override fun isPhoto_to_user_dir(): Boolean {
        return getPreferences(app).getBoolean("photo_to_user_dir", true)
    }

    override fun isVideo_swipes(): Boolean {
        return getPreferences(app).getBoolean("video_swipes", true)
    }

    override fun isVideo_controller_to_decor(): Boolean {
        return getPreferences(app).getBoolean("video_controller_to_decor", false)
    }

    override fun isUse_stop_audio(): Boolean {
        return getPreferences(app).getBoolean("use_stop_audio", false)
    }

    override fun isRevert_play_audio(): Boolean {
        return getPreferences(app).getBoolean("revert_play_audio", false)
    }

    override fun videoExt(): Set<String> {
        return getPreferences(app)
            .getStringSet("videos_ext", setOf("mp4", "avi", "mpeg"))!!
    }

    override fun photoExt(): Set<String> {
        return getPreferences(app)
            .getStringSet("photo_ext", setOf("gif", "jpg", "jpeg", "jpg", "webp", "png", "tiff"))!!
    }

    override fun audioExt(): Set<String> {
        return getPreferences(app)
            .getStringSet("audio_ext", setOf("mp3", "ogg", "flac", "opus"))!!
    }

    override val isOngoing_player_notification: Boolean
        get() = getPreferences(app).getBoolean("ongoing_player_notification", false)

    @get:ParserType
    override val currentParser: Int
        get() = try {
            getPreferences(app).getString("current_parser", "0")!!
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            ParserType.JSON
        }

    override val isCompress_incoming_traffic: Boolean
        get() = getPreferences(app).getBoolean("compress_incoming_traffic", true)

    @Transformers_Types
    override fun getViewpager_page_transform(): Int {
        return try {
            getPreferences(app).getString(
                "viewpager_page_transform",
                java.lang.String.valueOf(Transformers_Types.OFF)
            )!!.trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            Transformers_Types.OFF
        }
    }

    @Transformers_Types
    override fun getPlayer_cover_transform(): Int {
        return try {
            getPreferences(app).getString(
                "player_cover_transform",
                java.lang.String.valueOf(Transformers_Types.DEPTH_TRANSFORMER)
            )!!.trim { it <= ' ' }
                .toInt()
        } catch (e: Exception) {
            Transformers_Types.DEPTH_TRANSFORMER
        }
    }

    override val isLimitImage_cache: Int
        get() = try {
            getPreferences(app).getString("limit_cache_images", "2")!!
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            2
        }

    override val isOpen_folder_new_window: Boolean
        get() = getPreferences(app).getBoolean("open_folder_new_window", false)

    @get:Lang
    override val language: Int
        get() = try {
            getPreferences(app).getString("language_ui", "0")!!
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            Lang.DEFAULT
        }
}