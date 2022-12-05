package dev.ragnarok.fenrir.settings

import android.content.Context
import android.net.Uri
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
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable

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
    fun other(): IOtherSettings
    interface IOtherSettings {
        fun getFeedSourceIds(accountId: Int): String?
        fun setFeedSourceIds(accountId: Int, sourceIds: String?)
        fun storeFeedScrollState(accountId: Int, state: String?)
        fun restoreFeedScrollState(accountId: Int): String?
        fun restoreFeedNextFrom(accountId: Int): String?
        fun storeFeedNextFrom(accountId: Int, nextFrom: String?)
        val isAudioBroadcastActive: Boolean
        val maxBitmapResolution: Int
        val servicePlaylist: List<Int>
        val isValidate_tls: Boolean
        val isNative_parcel_photo: Boolean
        val isNative_parcel_story: Boolean
        val isDoLogs: Boolean
        val isDump_fcm: Boolean
        val isAutoplay_gif: Boolean
        val isStrip_news_repost: Boolean
        val isCommunities_in_page_search: Boolean
        val isAd_block_story_news: Boolean
        val isBlock_news_by_words: Set<String>?
        val isNew_loading_dialog: Boolean
        fun get_Api_Domain(): String
        fun get_Auth_Domain(): String
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
        fun toggleCommentsDirection(): Boolean
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
        fun is_side_navigation(): Boolean
        fun is_side_no_stroke(): Boolean
        fun is_side_transition(): Boolean
        fun is_notification_force_link(): Boolean
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
        fun videoExt(): Set<String>
        fun photoExt(): Set<String>
        fun audioExt(): Set<String>
        fun getMaxThumbResolution(): Int
        fun isEnable_dirs_files_count(): Boolean
        fun get_last_audio_sync(): Long
        fun set_last_audio_sync(time: Long)
        fun get_last_stikers_sync(accountId: Int): Long
        fun del_last_stikers_sync(accountId: Int)
        fun set_last_stikers_sync(accountId: Int, time: Long)
        val isOngoing_player_notification: Boolean
        fun reloadOwnerChangesMonitor()
        fun isOwnerInChangesMonitor(ownerId: Int): Boolean
        fun putOwnerInChangesMonitor(ownerId: Int)
        fun removeOwnerInChangesMonitor(ownerId: Int)
        val isAudio_catalog_v2: Boolean

        @get:Lang
        val language: Int
        val rendering_mode: Int
        val endListAnimation: Int
        fun appStoredVersionEqual(): Boolean
        var localServer: LocalServerSettings
        var playerCoverBackgroundSettings: PlayerCoverBackgroundSettings
        var slidrSettings: SlidrSettings
        fun getUserNameChanges(userId: Int): String?
        fun setUserNameChanges(userId: Int, name: String?)
        fun reloadUserNameChangesSettings(onlyRoot: Boolean)
        fun getUserNameChangesMap(): Map<String, String>
        val userNameChangesKeys: Set<String>
        val customChannelNotif: Int
        fun nextCustomChannelNotif()
        val currentParser: Int
        var catalogV2ListSort: List<Int>
    }

    interface IAccountsSettings {
        fun observeChanges(): Flowable<Int>
        fun observeRegistered(): Flowable<IAccountsSettings>
        val registered: List<Int>
        var current: Int
        val currentAccessToken: String?
        fun remove(accountId: Int)
        fun registerAccountId(accountId: Int, setCurrent: Boolean)
        fun storeAccessToken(accountId: Int, accessToken: String?)
        fun storeLogin(accountId: Int, loginCombo: String?)
        fun removeDevice(accountId: Int)
        fun storeDevice(accountId: Int, deviceName: String?)
        fun getDevice(accountId: Int): String?
        fun getLogin(accountId: Int): String?
        fun storeTokenType(accountId: Int, @AccountType type: Int)
        fun getAccessToken(accountId: Int): String?

        @AccountType
        fun getType(accountId: Int): Int
        fun removeAccessToken(accountId: Int)
        fun removeType(accountId: Int)
        fun removeLogin(accountId: Int)

        companion object {
            const val INVALID_ID = -1
        }
    }

    interface IMainSettings {
        val isSendByEnter: Boolean
        val isMy_message_no_color: Boolean
        val isNotification_bubbles_enabled: Boolean
        val isMessages_menu_down: Boolean
        val isExpand_voice_transcript: Boolean

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
        val isSnow_mode: Boolean
        val photoRoundMode: Int
        val fontSize: Int
        val isLoad_history_notif: Boolean
        val isDont_write: Boolean
        val isOver_ten_attach: Boolean
        fun cryptVersion(): Int
    }

    interface INotificationSettings {
        fun getNotifPref(aid: Int, peerid: Int): Int
        fun setDefault(aid: Int, peerId: Int)
        fun resetAccount(aid: Int)
        fun forceDisable(aid: Int, peerId: Int)
        fun setNotifPref(aid: Int, peerid: Int, flag: Int)
        val otherNotificationMask: Int
        val isCommentsNotificationsEnabled: Boolean
        val isFriendRequestAcceptationNotifEnabled: Boolean
        val isNewFollowerNotifEnabled: Boolean
        val isWallPublishNotifEnabled: Boolean
        val isGroupInvitedNotifEnabled: Boolean
        val isReplyNotifEnabled: Boolean
        val isNewPostOnOwnWallNotifEnabled: Boolean
        val isNewPostsNotificationEnabled: Boolean
        val isLikeNotificationEnable: Boolean
        val feedbackRingtoneUri: Uri
        val newPostRingtoneUri: Uri
        val defNotificationRingtone: String
        val notificationRingtone: String
        fun setNotificationRingtoneUri(path: String?)
        val vibrationLength: LongArray?
        val isQuickReplyImmediately: Boolean
        val isBirthdayNotifyEnabled: Boolean
        val isMentionNotifyEnabled: Boolean
        fun isSilentChat(aid: Int, peerId: Int): Boolean
        val chatsNotif: Map<String, Int>
        val chatsNotifKeys: Set<String>
        fun reloadNotifSettings(onlyRoot: Boolean)

        companion object {
            const val FLAG_SOUND = 1
            const val FLAG_VIBRO = 2
            const val FLAG_LED = 4
            const val FLAG_SHOW_NOTIF = 8
            const val FLAG_HIGH_PRIORITY = 16
        }
    }

    interface IRecentChats {
        operator fun get(acountid: Int): MutableList<RecentChat>
        fun store(accountid: Int, chats: List<RecentChat>)
    }

    interface IDrawerSettings {
        var categoriesOrder: List<DrawerCategory>
        fun observeChanges(): Observable<List<DrawerCategory>>
        fun reset()
    }

    interface ISideDrawerSettings {
        var categoriesOrder: List<DrawerCategory>
        fun observeChanges(): Observable<List<DrawerCategory>>
        fun reset()
    }

    interface IPushSettings {
        fun savePushRegistations(data: Collection<VkPushRegistration>)
        val registrations: List<VkPushRegistration>
    }

    interface ISecuritySettings {
        var isKeyEncryptionPolicyAccepted: Boolean
        fun isPinValid(values: IntArray): Boolean
        fun setPin(pin: IntArray?)
        val isUsePinForEntrance: Boolean
        val isUsePinForSecurity: Boolean
        val isEntranceByFingerprintAllowed: Boolean

        @KeyLocationPolicy
        fun getEncryptionLocationPolicy(accountId: Int, peerId: Int): Int
        fun disableMessageEncryption(accountId: Int, peerId: Int)
        fun isMessageEncryptionEnabled(accountId: Int, peerId: Int): Boolean
        fun enableMessageEncryption(accountId: Int, peerId: Int, @KeyLocationPolicy policy: Int)
        fun firePinAttemptNow()
        fun clearPinHistory()
        val pinEnterHistory: List<Long>
        fun hasPinHash(): Boolean
        fun pinHistoryDepthValue(): Int
        fun needHideMessagesBodyForNotif(): Boolean
        fun addHiddenDialog(peerId: Int)
        fun removeHiddenDialog(peerId: Int)
        fun hasHiddenDialogs(): Boolean
        fun isHiddenDialog(peerId: Int): Boolean
        var showHiddenDialogs: Boolean
        fun reloadHiddenDialogSettings()
        val isDelayedAllow: Boolean
        fun updateLastPinTime()
        fun IsShow_hidden_accounts(): Boolean
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
        fun getDefaultPage(accountId: Int): Place
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