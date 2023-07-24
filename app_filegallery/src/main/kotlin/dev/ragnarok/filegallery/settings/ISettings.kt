package dev.ragnarok.filegallery.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dev.ragnarok.filegallery.model.Lang
import dev.ragnarok.filegallery.model.LocalServerSettings
import dev.ragnarok.filegallery.model.PlayerCoverBackgroundSettings
import dev.ragnarok.filegallery.model.SlidrSettings
import dev.ragnarok.filegallery.settings.theme.ThemeOverlay
import io.reactivex.rxjava3.core.Observable

interface ISettings {
    fun main(): IMainSettings
    fun security(): ISecuritySettings
    interface IMainSettings {
        val fontSize: Int

        @ThemeOverlay
        val themeOverlay: Int
        val mainThemeKey: String
        fun setMainTheme(key: String)
        fun switchNightMode(@AppCompatDelegate.NightMode key: Int)
        fun isDarkModeEnabled(context: Context): Boolean

        @get:Lang
        val language: Int

        @AppCompatDelegate.NightMode
        val nightMode: Int
        val isDeveloper_mode: Boolean
        val isOpen_folder_new_window: Boolean
        val isEnable_dirs_files_count: Boolean

        val musicDir: String
        val photoDir: String
        val videoDir: String

        val localServer: LocalServerSettings
        fun setLocalServer(settings: LocalServerSettings)
        fun updateLocalServer()
        val playerCoverBackgroundSettings: PlayerCoverBackgroundSettings
        fun setPlayerCoverBackgroundSettings(settings: PlayerCoverBackgroundSettings)
        val slidrSettings: SlidrSettings
        fun setSlidrSettings(settings: SlidrSettings)
        val musicLifecycle: Int

        val maxBitmapResolution: Int
        val maxThumbResolution: Int
        val rendering_mode: Int
        val fFmpegPlugin: Int

        val isUse_internal_downloader: Boolean

        val isPlayer_Has_Background: Boolean
        val isShow_mini_player: Boolean

        val observeLocalServer: Observable<LocalServerSettings>
        val isShow_photos_line: Boolean
        val isInstant_photo_display: Boolean
        val isDownload_photo_tap: Boolean
        val isAudio_round_icon: Boolean
        val isPhoto_to_user_dir: Boolean
        val isVideo_swipes: Boolean
        val isVideo_controller_to_decor: Boolean
        val isUse_stop_audio: Boolean
        val isRevert_play_audio: Boolean

        val videoExt: Set<String>
        val photoExt: Set<String>
        val audioExt: Set<String>

        val viewpager_page_transform: Int
        val player_cover_transform: Int
        val isDeleteDisabled: Boolean
        val isValidate_tls: Boolean
        val isCompress_incoming_traffic: Boolean
        val currentParser: Int
        val isLimitImage_cache: Int
    }

    interface ISecuritySettings {
        fun isPinValid(values: IntArray): Boolean
        fun setPin(pin: IntArray?)
        var isEntranceByFingerprintAllowed: Boolean

        fun firePinAttemptNow()
        fun clearPinHistory()
        val pinEnterHistory: List<Long>
        val hasPinHash: Boolean
        val pinHistoryDepthValue: Int
        fun updateLastPinTime()
        var isUsePinForEntrance: Boolean
    }
}
