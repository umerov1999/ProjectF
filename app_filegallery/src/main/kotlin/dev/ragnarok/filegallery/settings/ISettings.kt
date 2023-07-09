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
        fun getFontSize(): Int

        @ThemeOverlay
        fun getThemeOverlay(): Int
        fun getMainThemeKey(): String
        fun setMainTheme(key: String)
        fun switchNightMode(@AppCompatDelegate.NightMode key: Int)
        fun isDarkModeEnabled(context: Context): Boolean

        @get:Lang
        val language: Int

        @AppCompatDelegate.NightMode
        fun getNightMode(): Int
        fun isDeveloper_mode(): Boolean
        val isOpen_folder_new_window: Boolean
        fun isEnable_dirs_files_count(): Boolean

        fun getMusicDir(): String
        fun getPhotoDir(): String
        fun getVideoDir(): String

        fun getLocalServer(): LocalServerSettings
        fun setLocalServer(settings: LocalServerSettings)
        fun updateLocalServer()
        fun getPlayerCoverBackgroundSettings(): PlayerCoverBackgroundSettings
        fun setPlayerCoverBackgroundSettings(settings: PlayerCoverBackgroundSettings)
        fun getSlidrSettings(): SlidrSettings
        fun setSlidrSettings(settings: SlidrSettings)
        fun getMusicLifecycle(): Int

        fun getMaxBitmapResolution(): Int
        fun getMaxThumbResolution(): Int
        fun getRendering_mode(): Int
        fun getFFmpegPlugin(): Int

        fun isUse_internal_downloader(): Boolean

        fun isPlayer_Has_Background(): Boolean
        fun isShow_mini_player(): Boolean

        fun observeLocalServer(): Observable<LocalServerSettings>
        fun isShow_photos_line(): Boolean
        fun isDownload_photo_tap(): Boolean
        fun isAudio_round_icon(): Boolean
        fun isPhoto_to_user_dir(): Boolean
        fun isVideo_swipes(): Boolean
        fun isVideo_controller_to_decor(): Boolean
        fun isUse_stop_audio(): Boolean
        fun isRevert_play_audio(): Boolean

        fun videoExt(): Set<String>
        fun photoExt(): Set<String>
        fun audioExt(): Set<String>

        fun getViewpager_page_transform(): Int
        fun getPlayer_cover_transform(): Int
        fun isDeleteDisabled(): Boolean
        val isValidate_tls: Boolean
        val isOngoing_player_notification: Boolean
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
        fun hasPinHash(): Boolean
        fun pinHistoryDepthValue(): Int
        fun updateLastPinTime()
        var isUsePinForEntrance: Boolean
    }
}
