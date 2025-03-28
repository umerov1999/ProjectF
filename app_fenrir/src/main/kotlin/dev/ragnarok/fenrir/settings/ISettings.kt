package dev.ragnarok.fenrir.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.api.model.LocalServerSettings
import dev.ragnarok.fenrir.api.model.PlayerCoverBackgroundSettings
import dev.ragnarok.fenrir.api.model.SlidrSettings
import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import dev.ragnarok.fenrir.model.DrawerCategory
import dev.ragnarok.fenrir.model.Lang
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.model.drawer.RecentChat
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.settings.theme.ThemeOverlay
import dev.ragnarok.fenrir.view.pager.Transformers_Types
import kotlinx.coroutines.flow.SharedFlow

interface ISettings {
    fun recentChats(): IRecentChats
    fun drawerSettings(): IDrawerSettings
    fun sideDrawerSettings(): ISideDrawerSettings
    fun pushSettings(): IPushSettings
    fun security(): ISecuritySettings
    fun ui(): IUISettings
    fun notifications(): INotificationSettings
    fun main(): IMainSettings
    fun accounts(): IAccountsSettings

    interface IAccountsSettings {
        val observeChanges: SharedFlow<Long>
        val observeRegistered: SharedFlow<IAccountsSettings>
        val registered: List<Long>
        var current: Long
        val currentAccessToken: String?

        @AccountType
        val currentType: Int
        val currentHidden: Boolean
        fun remove(accountId: Long): Long?
        fun registerAccountId(accountId: Long, setCurrent: Boolean)
        fun storeAccessToken(accountId: Long, accessToken: String?)
        fun storeLogin(accountId: Long, loginCombo: String?)
        fun removeDevice(accountId: Long)
        fun storeDevice(accountId: Long, deviceName: String?)
        fun getDevice(accountId: Long): String?
        fun getLogin(accountId: Long): String?
        fun storeTokenType(accountId: Long, @AccountType type: Int)
        fun getAccessToken(accountId: Long): String?

        @AccountType
        fun getType(accountId: Long): Int
        fun removeAccessToken(accountId: Long)
        fun removeType(accountId: Long)
        fun removeLogin(accountId: Long)

        fun loadAccounts(refresh: Boolean)

        var anonymToken: AnonymToken

        companion object {
            const val INVALID_ID = -1L
        }
    }

    interface IMainSettings {
        val isSendByEnter: Boolean
        val isMy_message_no_color: Boolean
        val isNotification_bubbles_enabled: Boolean
        val isMessages_menu_down: Boolean
        val isExpand_voice_transcript: Boolean
        val isChat_popup_menu: Boolean

        @get:ThemeOverlay
        val themeOverlay: Int
        val isAudio_round_icon: Boolean
        val isUse_long_click_download: Boolean
        val isRevert_play_audio: Boolean
        val isShow_bot_keyboard: Boolean
        val isPlayer_support_volume: Boolean
        val isOpenUrlInternal: Int
        var uploadImageSize: Int?
        val uploadImageSizePref: Int

        @get:PhotoSize
        val prefPreviewImageSize: Int
        fun notifyPrefPreviewSizeChanged()

        @PhotoSize
        fun getPrefDisplayImageSize(@PhotoSize byDefault: Int): Int

        @get:Transformers_Types
        val viewpager_page_transform: Int

        @get:Transformers_Types
        val player_cover_transform: Int
        val start_newsMode: Int
        fun setPrefDisplayImageSize(@PhotoSize size: Int)
        val isWebview_night_mode: Boolean
        val single_line_videos: Boolean
        val single_line_photos: Boolean
        val isSnow_mode: Boolean
        val photoRoundMode: Int
        val fontSize: Int
        val fontOnlyForChats: Boolean
        val isLoad_history_notif: Boolean
        val isDont_write: Boolean
        val isOver_ten_attach: Boolean
        val cryptVersion: Int

        fun getFeedSourceIds(accountId: Long): String?
        fun setFeedSourceIds(accountId: Long, sourceIds: String?)
        fun storeFeedScrollState(accountId: Long, state: String?)
        fun restoreFeedScrollState(accountId: Long): String?
        fun restoreFeedNextFrom(accountId: Long): String?
        fun storeFeedNextFrom(accountId: Long, nextFrom: String?)
        val isAudioBroadcastActive: Boolean
        val maxBitmapResolution: Int
        val servicePlaylist: List<Int>
        val isValidate_tls: Boolean
        val isNative_parcel_photo: Boolean
        val isNative_parcel_story: Boolean
        val isNative_parcel_docs: Boolean
        val isDoLogs: Boolean
        val isDump_fcm: Boolean
        val isAutoplay_gif: Boolean
        val isStrip_news_repost: Boolean
        val isCommunities_in_page_search: Boolean
        val isAd_block_story_news: Boolean
        val isBlock_news_by_words: Set<String>?
        val isNew_loading_dialog: Boolean
        val apiDomain: String
        val authDomain: String
        val isUse_api_5_90_for_audio: Boolean
        val isDisable_history: Boolean
        val isShow_wall_cover: Boolean
        val isDeveloper_mode: Boolean
        val isForce_cache: Boolean
        val isKeepLongpoll: Boolean
        fun setDisableErrorFCM(en: Boolean)
        val isDisabledErrorFCM: Boolean
        val isSettings_no_push: Boolean
        val isCommentsDesc: Boolean
        val toggleCommentsDirection: Boolean
        val isInfo_reading: Boolean
        val isAuto_read: Boolean
        val isMarkListenedVoice: Boolean
        val isNot_update_dialogs: Boolean
        val isBe_online: Boolean
        val donate_anim_set: Int
        val colorChat: Int
        val secondColorChat: Int
        val isCustom_chat_color: Boolean
        val colorMyMessage: Int
        val secondColorMyMessage: Int
        val isCustom_MyMessage: Boolean
        val isUse_stop_audio: Boolean
        val isPlayer_Has_Background: Boolean
        val isShow_mini_player: Boolean
        val isEnable_show_recent_dialogs: Boolean
        val is_side_navigation: Boolean
        val is_side_no_stroke: Boolean
        val is_side_transition: Boolean
        val is_notification_force_link: Boolean
        val isEnable_show_audio_top: Boolean
        val isUse_internal_downloader: Boolean
        val isEnable_last_read: Boolean
        val isNot_read_show: Boolean
        val isHeaders_in_dialog: Boolean
        val musicDir: String
        val photoDir: String
        val videoDir: String
        val docDir: String
        val stickerDir: String
        val isPhoto_to_user_dir: Boolean
        val isDownload_voice_ogg: Boolean
        val isDelete_cache_images: Boolean
        val isCompress_incoming_traffic: Boolean
        val isCompress_outgoing_traffic: Boolean
        val isLimitImage_cache: Int
        val isDo_not_clear_back_stack: Boolean
        val isMention_fave: Boolean
        val isDisabled_encryption: Boolean
        val isDownload_photo_tap: Boolean
        val isDisable_sensored_voice: Boolean
        var isInvertPhotoRev: Boolean
        val isAudio_save_mode_button: Boolean
        val isShow_mutual_count: Boolean
        val isDo_zoom_photo: Boolean
        val isChange_upload_size: Boolean
        val isInstant_photo_display: Boolean
        val isShow_photos_line: Boolean
        val isShow_photos_date: Boolean
        var isDisable_likes: Boolean
        var isDisable_notifications: Boolean
        val isDo_auto_play_video: Boolean
        val isVideo_controller_to_decor: Boolean
        val isVideo_swipes: Boolean
        val isHint_stickers: Boolean
        val isEnable_native: Boolean
        val isRecording_to_opus: Boolean
        val paganSymbol: Int
        val isRunes_show: Boolean
        val musicLifecycle: Int
        val fFmpegPlugin: Int
        var isRememberLocalAudioAlbum: Boolean
        var currentLocalAudioAlbum: Int
        val videoExt: Set<String>
        val photoExt: Set<String>
        val audioExt: Set<String>
        val maxThumbResolution: Int
        val isEnable_dirs_files_count: Boolean
        val last_audio_sync: Long
        fun set_last_audio_sync(time: Long)
        fun get_last_sticker_sets_sync(accountId: Long): Long
        fun set_last_sticker_sets_sync(accountId: Long, time: Long)
        fun get_last_sticker_sets_custom_sync(accountId: Long): Long
        fun set_last_sticker_sets_custom_sync(accountId: Long, time: Long)
        fun get_last_sticker_keywords_sync(accountId: Long): Long
        fun set_last_sticker_keywords_sync(accountId: Long, time: Long)
        fun del_last_sticker_sets_sync(accountId: Long)
        fun del_last_sticker_sets_custom_sync(accountId: Long)
        fun del_last_sticker_keywords_sync(accountId: Long)
        fun del_last_reaction_assets_sync(accountId: Long)
        fun set_last_reaction_assets_sync(accountId: Long, time: Long)
        fun get_last_reaction_assets_sync(accountId: Long): Long
        fun reloadOwnerChangesMonitor()
        fun isOwnerInChangesMonitor(ownerId: Long): Boolean
        fun putOwnerInChangesMonitor(ownerId: Long)
        fun removeOwnerInChangesMonitor(ownerId: Long)
        fun resetAllChangesMonitor()
        val isAudio_catalog_v2: Boolean

        val picassoDispatcher: Int

        @get:Lang
        val language: Int
        val rendering_mode: Int
        val endListAnimation: Int
        val appStoredVersionEqual: Boolean
        var localServer: LocalServerSettings
        var playerCoverBackgroundSettings: PlayerCoverBackgroundSettings
        var slidrSettings: SlidrSettings
        fun getUserNameChanges(userId: Long): String?
        fun setUserNameChanges(userId: Long, name: String?)
        fun reloadUserNameChangesSettings(onlyRoot: Boolean)
        val userNameChangesMap: Map<String, String>
        fun resetAllUserNameChanges()
        val customChannelNotif: Int
        fun nextCustomChannelNotif()
        val currentParser: Int
        var catalogV2ListSort: List<Int>
    }

    interface INotificationSettings {
        fun resetAccount(aid: Long)
        fun resetAll()

        fun setSilentPeer(aid: Long, peerId: Long, silent: Boolean)
        fun isSilentPeer(aid: Long, peerId: Long): Boolean

        val silentPeersMap: Map<String, Boolean>

        fun reloadSilentSettings(onlyRoot: Boolean)
    }

    interface IRecentChats {
        operator fun get(accountId: Long): MutableList<RecentChat>
        fun store(accountId: Long, chats: List<RecentChat>)
    }

    interface IDrawerSettings {
        var categoriesOrder: List<DrawerCategory>
        val observeChanges: SharedFlow<List<DrawerCategory>>
        fun reset()
    }

    interface ISideDrawerSettings {
        var categoriesOrder: List<DrawerCategory>
        val observeChanges: SharedFlow<List<DrawerCategory>>
        fun reset()
    }

    interface IPushSettings {
        fun savePushRegistrations(data: Collection<VKPushRegistration>)
        val registrations: List<VKPushRegistration>
    }

    interface ISecuritySettings {
        var isKeyEncryptionPolicyAccepted: Boolean
        fun isPinValid(values: IntArray): Boolean
        fun setPin(pin: IntArray?)
        var isUsePinForSecurity: Boolean
        var isUsePinForEntrance: Boolean
        var isDelayedAllow: Boolean
        var isEntranceByFingerprintAllowed: Boolean

        @KeyLocationPolicy
        fun getEncryptionLocationPolicy(accountId: Long, peerId: Long): Int
        fun disableMessageEncryption(accountId: Long, peerId: Long)
        fun isMessageEncryptionEnabled(accountId: Long, peerId: Long): Boolean
        fun enableMessageEncryption(accountId: Long, peerId: Long, @KeyLocationPolicy policy: Int)
        fun firePinAttemptNow()
        fun clearPinHistory()
        val pinEnterHistory: List<Long>
        val hasPinHash: Boolean
        val pinHistoryDepthValue: Int
        val needHideMessagesBodyForNotif: Boolean
        fun addHiddenDialog(peerId: Long)
        fun removeHiddenDialog(peerId: Long)
        val hasHiddenDialogs: Boolean
        fun isHiddenDialog(peerId: Long): Boolean
        var showHiddenDialogs: Boolean
        fun reloadHiddenDialogSettings()
        fun updateLastPinTime()
        val IsShow_hidden_accounts: Boolean
    }

    interface IUISettings {
        fun setMainTheme(key: String)
        fun switchNightMode(@AppCompatDelegate.NightMode key: Int)
        val mainThemeKey: String

        @get:AvatarStyle
        val avatarStyle: Int
        fun storeAvatarStyle(@AvatarStyle style: Int)
        fun isDarkModeEnabled(context: Context): Boolean
        val nightMode: Int
        fun getDefaultPage(accountId: Long): Place
        fun notifyPlaceResumed(type: Int)
        val isSystemEmoji: Boolean
        val isEmojis_full_screen: Boolean
        val isStickers_by_theme: Boolean
        val isStickers_by_new: Boolean
        val isShow_profile_in_additional_page: Boolean

        @get:SwipesChatMode
        val swipes_chat_mode: Int
        val isDisplay_writing: Boolean
    }
}