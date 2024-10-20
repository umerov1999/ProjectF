package dev.ragnarok.filegallery.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Environment
import androidx.appcompat.app.AppCompatDelegate
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.FileUtils
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.Constants.forceDeveloperMode
import dev.ragnarok.filegallery.kJson
import dev.ragnarok.filegallery.model.Lang
import dev.ragnarok.filegallery.model.LocalServerSettings
import dev.ragnarok.filegallery.model.ParserType
import dev.ragnarok.filegallery.model.PlayerCoverBackgroundSettings
import dev.ragnarok.filegallery.model.SlidrSettings
import dev.ragnarok.filegallery.settings.ISettings.IMainSettings
import dev.ragnarok.filegallery.settings.theme.ThemeOverlay
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.myEmit
import dev.ragnarok.filegallery.view.pager.Transformers_Types
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

internal class MainSettings(context: Context) : IMainSettings {
    private val app: Context = context.applicationContext
    private val localServerPublisher = MutableSharedFlow<LocalServerSettings>(replay = 1)

    override val fontSize: Int
        get() = getPreferences(app).getInt("font_size_int", 0)

    override val isValidate_tls: Boolean
        get() = getPreferences(app).getBoolean("validate_tls", true)

    @get:ThemeOverlay
    override val themeOverlay: Int
        get() = try {
            getPreferences(app).getString("theme_overlay", "0")?.trim()?.toInt()
                ?: ThemeOverlay.OFF
        } catch (_: Exception) {
            ThemeOverlay.OFF
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
                .getString("night_switch", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString())!!
                .trim()
                .toInt()
        } catch (_: Exception) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

    override val isDeveloper_mode: Boolean
        get() = getPreferences(app).getBoolean("developer_mode", forceDeveloperMode)

    override val isEnable_dirs_files_count: Boolean
        get() = getPreferences(app).getBoolean("enable_dirs_files_count", true)

    override val isDeleteDisabled: Boolean
        get() = getPreferences(app).getBoolean("delete_disabled", false)

    override val musicDir: String
        get() {
            var ret = getPreferences(app).getString("music_dir", null)
            if (ret.isNullOrEmpty() || !File(ret).exists()) {
                ret =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
                getPreferences(app).edit().putString("music_dir", ret).apply()
            }
            return ret!!
        }

    override val photoDir: String
        get() {
            var ret = getPreferences(app).getString("photo_dir", null)
            if (ret.isNullOrEmpty() || !File(ret).exists()) {
                ret =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/Fenrir"
                getPreferences(app).edit().putString("photo_dir", ret).apply()
            }
            return ret
        }

    override val videoDir: String
        get() {
            var ret = getPreferences(app).getString("video_dir", null)
            if (ret.isNullOrEmpty() || !File(ret).exists()) {
                ret =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath + "/Fenrir"
                getPreferences(app).edit().putString("video_dir", ret).apply()
            }
            return ret
        }

    override val localServer: LocalServerSettings
        get() {
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
        localServerPublisher.myEmit(settings)
    }

    override fun updateLocalServer() {
        localServerPublisher.myEmit(localServer)
    }

    override val playerCoverBackgroundSettings: PlayerCoverBackgroundSettings
        get() {
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

    override val slidrSettings: SlidrSettings
        get() {
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

    override val musicLifecycle: Int
        get() = try {
            var v = getPreferences(app).getString(
                "lifecycle_music_service", Constants.AUDIO_PLAYER_SERVICE_IDLE.toString()
            )!!
                .trim().toInt()
            if (v < 60000) {
                getPreferences(app).edit().putString("lifecycle_music_service", "60000").apply()
                v = 60000
            }
            v
        } catch (_: Exception) {
            Constants.AUDIO_PLAYER_SERVICE_IDLE
        }

    override val maxBitmapResolution: Int
        get() = try {
            getPreferences(app).getString("max_bitmap_resolution", "4000")!!.trim()
                .toInt()
        } catch (_: Exception) {
            4000
        }

    override val maxThumbResolution: Int
        get() = try {
            getPreferences(app).getString("max_thumb_resolution", "384")!!.trim()
                .toInt()
        } catch (_: Exception) {
            384
        }

    override val rendering_mode: Int
        get() = try {
            getPreferences(app).getString("rendering_bitmap_mode", "0")!!.trim().toInt()
        } catch (_: Exception) {
            0
        }

    override val fFmpegPlugin: Int
        get() = try {
            getPreferences(app).getString("ffmpeg_audio_codecs", "1")!!.trim()
                .toInt()
        } catch (_: Exception) {
            1
        }

    override val isPlayer_Has_Background: Boolean
        get() = getPreferences(app).getBoolean("player_has_background", true)

    override val isShow_mini_player: Boolean
        get() = getPreferences(app).getBoolean("show_mini_player", true)

    override val observeLocalServer: SharedFlow<LocalServerSettings>
        get() = localServerPublisher

    override val isUse_internal_downloader: Boolean
        get() = getPreferences(app).getBoolean("use_internal_downloader", true)

    override val isShow_photos_line: Boolean
        get() = getPreferences(app).getBoolean("show_photos_line", true)

    override val isInstant_photo_display: Boolean
        get() {
            if (!getPreferences(app).contains("instant_photo_display")) {
                getPreferences(app).edit().putBoolean(
                    "instant_photo_display",
                    FenrirNative.isNativeLoaded && FileUtils.threadsCount > 4
                ).apply()
            }
            return getPreferences(app).getBoolean("instant_photo_display", false)
        }

    override val isDownload_photo_tap: Boolean
        get() = getPreferences(app).getBoolean("download_photo_tap", true)

    override val isAudio_round_icon: Boolean
        get() = getPreferences(app).getBoolean("audio_round_icon", true)

    override val isPhoto_to_user_dir: Boolean
        get() = getPreferences(app).getBoolean("photo_to_user_dir", true)

    override val isVideo_swipes: Boolean
        get() = getPreferences(app).getBoolean("video_swipes", true)

    override val isVideo_controller_to_decor: Boolean
        get() = getPreferences(app).getBoolean("video_controller_to_decor", false)

    override val isUse_stop_audio: Boolean
        get() = getPreferences(app).getBoolean("use_stop_audio", false)

    override val isRevert_play_audio: Boolean
        get() = getPreferences(app).getBoolean("revert_play_audio", false)

    override val videoExt: Set<String>
        get() = getPreferences(app)
            .getStringSet("videos_ext", setOf("mp4", "avi", "mpeg"))!!

    override val photoExt: Set<String>
        get() = getPreferences(app)
            .getStringSet("photo_ext", setOf("jpg", "jpeg", "heic", "webp", "png", "tiff"))!!

    override val audioExt: Set<String>
        get() = getPreferences(app)
            .getStringSet("audio_ext", setOf("mp3", "ogg", "flac", "opus"))!!

    @get:ParserType
    override val currentParser: Int
        get() = try {
            getPreferences(app).getString("current_parser", "0")!!
                .trim().toInt()
        } catch (_: Exception) {
            ParserType.JSON
        }

    override val isCompress_incoming_traffic: Boolean
        get() = getPreferences(app).getBoolean("compress_incoming_traffic", true)

    @get:Transformers_Types
    override val viewpager_page_transform: Int
        get() = try {
            getPreferences(app).getString(
                "viewpager_page_transform",
                Transformers_Types.OFF.toString()
            )!!
                .trim().toInt()
        } catch (_: Exception) {
            Transformers_Types.OFF
        }

    @get:Transformers_Types
    override val player_cover_transform: Int
        get() = try {
            getPreferences(app).getString(
                "player_cover_transform",
                Transformers_Types.DEPTH_TRANSFORMER.toString()
            )!!
                .trim().toInt()
        } catch (_: Exception) {
            Transformers_Types.DEPTH_TRANSFORMER
        }

    override val isLimitImage_cache: Int
        get() = try {
            getPreferences(app).getString("limit_cache_images", "2")!!
                .trim().toInt()
        } catch (_: Exception) {
            2
        }

    override val isOpen_folder_new_window: Boolean
        get() = getPreferences(app).getBoolean("open_folder_new_window", false)

    override val picassoDispatcher: Int
        get() = try {
            if (!getPreferences(app).contains("picasso_dispatcher")) {
                getPreferences(app).edit().putString(
                    "picasso_dispatcher",
                    if (FenrirNative.isNativeLoaded && FileUtils.threadsCount > 4) "1" else "0"
                ).apply()
            }
            getPreferences(app).getString("picasso_dispatcher", "0")!!
                .trim().toInt()
        } catch (_: Exception) {
            0
        }

    @get:Lang
    override val language: Int
        get() = try {
            getPreferences(app).getString("language_ui", "0")!!
                .trim().toInt()
        } catch (_: Exception) {
            Lang.DEFAULT
        }
}
