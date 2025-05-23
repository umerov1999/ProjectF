package dev.ragnarok.fenrir.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Environment
import androidx.core.content.edit
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.BuildConfig
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.model.LocalServerSettings
import dev.ragnarok.fenrir.api.model.PlayerCoverBackgroundSettings
import dev.ragnarok.fenrir.api.model.SlidrSettings
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.model.Lang
import dev.ragnarok.fenrir.model.ParserType
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.FileUtils
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.ISettings.IMainSettings
import dev.ragnarok.fenrir.settings.theme.ThemeOverlay
import dev.ragnarok.fenrir.toColor
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.pager.Transformers_Types
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File
import java.util.Collections

internal class MainSettings(context: Context) : IMainSettings {
    private val app: Context = context.applicationContext
    private var preferPhotoPreviewSize: Optional<Int>
    private val userNameChangesKeys: MutableSet<String> = Collections.synchronizedSet(HashSet(1))
    private val userNameChangesTypes: MutableMap<String, String> =
        Collections.synchronizedMap(HashMap(1))
    private val ownerChangesMonitor: MutableSet<Long> = Collections.synchronizedSet(HashSet(1))

    override val isSendByEnter: Boolean
        get() = getPreferences(app).getBoolean("send_by_enter", false)

    @get:ThemeOverlay
    override val themeOverlay: Int
        get() = try {
            getPreferences(app).getString("theme_overlay", "0")?.trim()?.toInt()
                ?: ThemeOverlay.OFF
        } catch (_: Exception) {
            ThemeOverlay.OFF
        }
    override val isAudio_round_icon: Boolean
        get() = getPreferences(app).getBoolean("audio_round_icon", true)
    override val isUse_long_click_download: Boolean
        get() = getPreferences(app).getBoolean("use_long_click_download", false)
    override val isRevert_play_audio: Boolean
        get() = getPreferences(app).getBoolean("revert_play_audio", false)
    override val isPlayer_support_volume: Boolean
        get() = getPreferences(app).getBoolean("is_player_support_volume", false)
    override val isShow_bot_keyboard: Boolean
        get() = getPreferences(app).getBoolean("show_bot_keyboard", true)
    override val isMy_message_no_color: Boolean
        get() = getPreferences(app).getBoolean("my_message_no_color", false)
    override val isNotification_bubbles_enabled: Boolean
        get() = getPreferences(app).getBoolean("notification_bubbles", true)
    override val isMessages_menu_down: Boolean
        get() = getPreferences(app).getBoolean("messages_menu_down", false)
    override val isExpand_voice_transcript: Boolean
        get() = getPreferences(app).getBoolean("expand_voice_transcript", false)
    override val isChat_popup_menu: Boolean
        get() = getPreferences(app).getBoolean("chat_popup_menu", true)
    override var uploadImageSize: Int?
        get() {
            return when (getPreferences(app).getString(KEY_IMAGE_SIZE, "0")) {
                "1" -> Upload.IMAGE_SIZE_800
                "2" -> Upload.IMAGE_SIZE_1200
                "3" -> Upload.IMAGE_SIZE_FULL
                "4" -> Upload.IMAGE_SIZE_CROPPING
                else -> null
            }
        }
        set(uploadImgSize) {
            getPreferences(app).edit {
                putString(KEY_IMAGE_SIZE, uploadImgSize?.toString() ?: "0")
            }
        }

    override val uploadImageSizePref: Int
        get() = try {
            getPreferences(app).getString(KEY_IMAGE_SIZE, "0")?.trim()?.toInt() ?: 2
        } catch (_: Exception) {
            0
        }
    override val start_newsMode: Int
        get() = try {
            getPreferences(app).getString("start_news", "2")?.trim()?.toInt() ?: 2
        } catch (_: Exception) {
            2
        }

    @get:PhotoSize
    @get:SuppressLint("WrongConstant")
    override val prefPreviewImageSize: Int
        get() {
            if (preferPhotoPreviewSize.isEmpty) {
                preferPhotoPreviewSize = wrap(restorePhotoPreviewSize())
            }
            return preferPhotoPreviewSize.get()!!
        }

    override val cryptVersion: Int
        get() = try {
            getPreferences(app).getString("crypt_version", "1")?.trim()?.toInt() ?: 1
        } catch (_: Exception) {
            1
        }

    @PhotoSize
    private fun restorePhotoPreviewSize(): Int {
        return try {
            getPreferences(app).getString("photo_preview_size", PhotoSize.Y.toString())!!
                .trim()
                .toInt()
        } catch (_: Exception) {
            PhotoSize.Y
        }
    }

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

    override fun notifyPrefPreviewSizeChanged() {
        preferPhotoPreviewSize = empty()
    }

    @PhotoSize
    override fun getPrefDisplayImageSize(@PhotoSize byDefault: Int): Int {
        return getPreferences(app).getInt("pref_display_photo_size", byDefault)
    }

    override val photoRoundMode: Int
        get() = try {
            getPreferences(app).getString("photo_rounded_view", "0")?.trim()?.toInt()
                ?: 0
        } catch (_: Exception) {
            0
        }
    override val fontSize: Int
        get() = getPreferences(app).getInt("font_size_int", 0)

    override val fontOnlyForChats: Boolean
        get() = getPreferences(app).getBoolean("font_only_for_chats", false)

    override fun setPrefDisplayImageSize(@PhotoSize size: Int) {
        getPreferences(app)
            .edit {
                putInt("pref_display_photo_size", size)
            }
    }

    override val isOpenUrlInternal: Int
        get() = try {
            getPreferences(app).getString("is_open_url_internal", "1")?.trim()?.toInt()
                ?: 1
        } catch (_: Exception) {
            1
        }
    override val isWebview_night_mode: Boolean
        get() = getPreferences(app).getBoolean("webview_night_mode", true)
    override val isLoad_history_notif: Boolean
        get() = getPreferences(app).getBoolean("load_history_notif", false)
    override val single_line_videos: Boolean
        get() = getPreferences(app).getBoolean("single_line_videos", false)
    override val single_line_photos: Boolean
        get() = getPreferences(app).getBoolean("single_line_photos", false)
    override val isSnow_mode: Boolean
        get() = getPreferences(app).getBoolean("snow_mode", false)
    override val isDont_write: Boolean
        get() = getPreferences(app).getBoolean("dont_write", false)
    override val isOver_ten_attach: Boolean
        get() = getPreferences(app).getBoolean("over_ten_attach", false)

    override val userNameChangesMap: Map<String, String>
        get() = HashMap(userNameChangesTypes)

    override fun isOwnerInChangesMonitor(ownerId: Long): Boolean {
        return ownerChangesMonitor.contains(ownerId)
    }

    override fun reloadOwnerChangesMonitor() {
        val preferences = getPreferences(app)
        ownerChangesMonitor.clear()
        for (i in preferences.getStringSet("owner_changes_monitor_uids", HashSet(1)) ?: return) {
            ownerChangesMonitor.add(i.toLong())
        }
    }

    override fun reloadUserNameChangesSettings(onlyRoot: Boolean) {
        val preferences = getPreferences(app)
        userNameChangesKeys.clear()
        userNameChangesKeys.addAll(
            preferences.getStringSet(KEY_USERNAME_UIDS, HashSet(1)) ?: return
        )
        if (onlyRoot) {
            return
        }
        userNameChangesTypes.clear()
        for (i in userNameChangesKeys) {
            val rs = preferences.getString(i, null)
            if (rs.nonNullNoEmpty()) {
                userNameChangesTypes[i] = rs
            }
        }
    }

    override fun putOwnerInChangesMonitor(ownerId: Long) {
        val preferences = getPreferences(app)
        ownerChangesMonitor.add(ownerId)
        val ownerChangesMonitorSet = HashSet<String>()
        for (i in ownerChangesMonitor) {
            ownerChangesMonitorSet.add(i.toString())
        }
        preferences.edit {
            putStringSet("owner_changes_monitor_uids", ownerChangesMonitorSet)
        }
    }

    override fun removeOwnerInChangesMonitor(ownerId: Long) {
        val preferences = getPreferences(app)
        ownerChangesMonitor.remove(ownerId)
        val ownerChangesMonitorSet = HashSet<String>()
        for (i in ownerChangesMonitor) {
            ownerChangesMonitorSet.add(i.toString())
        }
        preferences.edit {
            putStringSet("owner_changes_monitor_uids", ownerChangesMonitorSet)
        }
    }

    override fun resetAllChangesMonitor() {
        val preferences = getPreferences(app)
        ownerChangesMonitor.clear()
        preferences.edit {
            putStringSet("owner_changes_monitor_uids", HashSet<String>())
        }
    }

    override fun resetAllUserNameChanges() {
        val preferences = getPreferences(app)
        for (i in userNameChangesKeys) {
            preferences.edit { remove(i) }
        }
        userNameChangesKeys.clear()
        userNameChangesTypes.clear()
        preferences.edit {
            putStringSet(KEY_USERNAME_UIDS, userNameChangesKeys)
        }
    }

    override fun setUserNameChanges(userId: Long, name: String?) {
        val preferences = getPreferences(app)
        if (name.isNullOrEmpty()) {
            userNameChangesKeys.remove(keyForUserNameChanges(userId))
            userNameChangesTypes.remove(keyForUserNameChanges(userId))
            preferences.edit {
                remove(keyForUserNameChanges(userId))
                    .putStringSet(KEY_USERNAME_UIDS, userNameChangesKeys)
            }
        } else {
            userNameChangesKeys.add(keyForUserNameChanges(userId))
            userNameChangesTypes[keyForUserNameChanges(userId)] = name
            preferences.edit {
                putString(keyForUserNameChanges(userId), name)
                    .putStringSet(KEY_USERNAME_UIDS, userNameChangesKeys)
            }
        }
    }

    override fun getUserNameChanges(userId: Long): String? {
        if (userNameChangesTypes.containsKey(keyForUserNameChanges(userId))) {
            val v = userNameChangesTypes[keyForUserNameChanges(userId)]
            if (v.nonNullNoEmpty()) {
                return v
            }
        }
        return null
    }

    override fun getFeedSourceIds(accountId: Long): String? {
        return getPreferences(app)
            .getString("source_ids$accountId", null)
    }

    override fun setFeedSourceIds(accountId: Long, sourceIds: String?) {
        getPreferences(app)
            .edit {
                putString("source_ids$accountId", sourceIds)
            }
    }

    override fun storeFeedScrollState(accountId: Long, state: String?) {
        if (state != null) {
            getPreferences(app)
                .edit {
                    putString(KEY_JSON_STATE + accountId, state)
                }
        } else {
            getPreferences(app)
                .edit {
                    remove(KEY_JSON_STATE + accountId)
                }
        }
    }

    override fun restoreFeedScrollState(accountId: Long): String? {
        return getPreferences(app).getString(KEY_JSON_STATE + accountId, null)
    }

    override fun restoreFeedNextFrom(accountId: Long): String? {
        return getPreferences(app)
            .getString("next_from$accountId", null)
    }

    override fun storeFeedNextFrom(accountId: Long, nextFrom: String?) {
        getPreferences(app)
            .edit {
                putString("next_from$accountId", nextFrom)
            }
    }

    override val isAudioBroadcastActive: Boolean
        get() = getPreferences(app).getBoolean("broadcast", false)
    override val isCommentsDesc: Boolean
        get() = getPreferences(app).getBoolean("comments_desc", true)

    override val toggleCommentsDirection: Boolean
        get() {
            val descNow = isCommentsDesc
            getPreferences(app)
                .edit {
                    putBoolean("comments_desc", !descNow)
                }
            return !descNow
        }

    override val isKeepLongpoll: Boolean
        get() = getPreferences(app).getBoolean("keep_longpoll", false)

    override fun setDisableErrorFCM(en: Boolean) {
        getPreferences(app).edit { putBoolean("disable_error_fcm", en) }
    }

    override val isDisabledErrorFCM: Boolean
        get() = getPreferences(app).getBoolean("disable_error_fcm", false)
    override val isSettings_no_push: Boolean
        get() = getPreferences(app).getBoolean("settings_no_push", false)
    override val maxBitmapResolution: Int
        get() = try {
            getPreferences(app).getString("max_bitmap_resolution", "4000")!!
                .trim().toInt()
        } catch (_: Exception) {
            4000
        }
    override val servicePlaylist: List<Int>
        get() = try {
            val rs = getPreferences(app).getString("service_playlists", "-21 -22 -25 -26 -27 -28")!!
                .trim()
            if (rs.isEmpty()) {
                emptyList()
            } else {
                val integerStrings = rs.split(Regex(" ")).toTypedArray()
                if (integerStrings.isEmpty()) {
                    emptyList()
                } else {
                    val integers: MutableList<Int> = ArrayList(integerStrings.size)
                    for (i in integerStrings.indices) {
                        integers.add(i, integerStrings[i].trim().toInt())
                    }
                    integers
                }
            }
        } catch (_: Exception) {
            listOf(-21, -22, -25, -26, -27, -28)
        }
    override val fFmpegPlugin: Int
        get() = try {
            getPreferences(app).getString("ffmpeg_audio_codecs", "1")!!
                .trim().toInt()
        } catch (_: Exception) {
            1
        }

    override val isLimitImage_cache: Int
        get() = try {
            getPreferences(app).getString("limit_cache_images", "2")!!
                .trim().toInt()
        } catch (_: Exception) {
            2
        }

    override val musicLifecycle: Int
        get() = try {
            var v = getPreferences(app).getString(
                "lifecycle_music_service", Constants.AUDIO_PLAYER_SERVICE_IDLE.toString()
            )!!
                .trim().toInt()
            if (v < 60000) {
                getPreferences(app).edit { putString("lifecycle_music_service", "60000") }
                v = 60000
            }
            v
        } catch (_: Exception) {
            Constants.AUDIO_PLAYER_SERVICE_IDLE
        }
    override val isAutoplay_gif: Boolean
        get() = getPreferences(app).getBoolean("autoplay_gif", true)
    override val isStrip_news_repost: Boolean
        get() = getPreferences(app).getBoolean("strip_news_repost", false)
    override val isCommunities_in_page_search: Boolean
        get() = getPreferences(app).getBoolean("communities_in_page_search", false)
    override val isAd_block_story_news: Boolean
        get() = getPreferences(app).getBoolean("ad_block_story_news", true)
    override val isBlock_news_by_words: Set<String>?
        get() = getPreferences(app).getStringSet("block_news_by_words_set", null)
    override val isNew_loading_dialog: Boolean
        get() = getPreferences(app).getBoolean("new_loading_dialog", true)

    override val apiDomain: String
        get() = getPreferences(app)
            .getString("vk_api_domain", "api.vk.com")!!.trim()

    override val authDomain: String
        get() = getPreferences(app)
            .getString("vk_auth_domain", "oauth.vk.com")!!.trim()

    override val isDeveloper_mode: Boolean
        get() = getPreferences(app).getBoolean("developer_mode", Constants.forceDeveloperMode)
    override val isForce_cache: Boolean
        get() = getPreferences(app).getBoolean("force_cache", false)
    override val isUse_api_5_90_for_audio: Boolean
        get() = getPreferences(app).getBoolean("use_api_5_90_for_audio", true)
    override val isDisable_history: Boolean
        get() = getPreferences(app).getBoolean("disable_history", false)
    override val isShow_wall_cover: Boolean
        get() = getPreferences(app).getBoolean("show_wall_cover", true)
    override val colorChat: Int
        get() = getPreferences(app).getInt("custom_chat_color", Color.argb(255, 255, 255, 255))
    override val secondColorChat: Int
        get() = getPreferences(app).getInt(
            "custom_chat_color_second",
            Color.argb(255, 255, 255, 255)
        )
    override val isCustom_chat_color: Boolean
        get() = getPreferences(app).getBoolean("custom_chat_color_usage", false)
    override val colorMyMessage: Int
        get() = getPreferences(app).getInt("custom_message_color", "#CBD438FF".toColor())
    override val secondColorMyMessage: Int
        get() = getPreferences(app).getInt(
            "custom_second_message_color",
            "#BF6539DF".toColor()
        )
    override val isCustom_MyMessage: Boolean
        get() = getPreferences(app).getBoolean("custom_message_color_usage", false)
    override val isInfo_reading: Boolean
        get() = getPreferences(app).getBoolean("info_reading", true)
    override val isAuto_read: Boolean
        get() = getPreferences(app).getBoolean("auto_read", false)
    override val isMarkListenedVoice: Boolean
        get() = getPreferences(app).getBoolean("mark_listened_voice", true)
    override val isNot_update_dialogs: Boolean
        get() = getPreferences(app).getBoolean("not_update_dialogs", false)
    override val isBe_online: Boolean
        get() = getPreferences(app).getBoolean("be_online", false)
    override val donate_anim_set: Int
        get() = try {
            getPreferences(app).getString("donate_anim_set", "2")!!
                .trim().toInt()
        } catch (_: Exception) {
            2
        }
    override val isUse_stop_audio: Boolean
        get() = getPreferences(app).getBoolean("use_stop_audio", false)
    override val isPlayer_Has_Background: Boolean
        get() = getPreferences(app).getBoolean("player_has_background", true)
    override val isShow_mini_player: Boolean
        get() = getPreferences(app).getBoolean("show_mini_player", true)
    override val isEnable_last_read: Boolean
        get() = getPreferences(app).getBoolean("enable_last_read", false)
    override val isNot_read_show: Boolean
        get() = getPreferences(app).getBoolean("not_read_show", true)
    override val isHeaders_in_dialog: Boolean
        get() = getPreferences(app).getBoolean("headers_in_dialog", true)
    override val isEnable_show_recent_dialogs: Boolean
        get() = getPreferences(app).getBoolean("show_recent_dialogs", true)

    override val is_side_navigation: Boolean
        get() = getPreferences(app).getBoolean("is_side_navigation", false)

    override val is_side_no_stroke: Boolean
        get() = getPreferences(app).getBoolean("is_side_no_stroke", false)

    override val is_side_transition: Boolean
        get() = getPreferences(app).getBoolean("is_side_transition", true)

    override val last_audio_sync: Long
        get() = getPreferences(app).getLong("last_audio_sync", -1)

    override fun set_last_audio_sync(time: Long) {
        getPreferences(app).edit { putLong("last_audio_sync", time) }
    }

    override fun get_last_sticker_sets_sync(accountId: Long): Long {
        return getPreferences(app).getLong("last_sticker_sets_sync_$accountId", -1)
    }

    override fun set_last_sticker_sets_sync(accountId: Long, time: Long) {
        getPreferences(app).edit { putLong("last_sticker_sets_sync_$accountId", time) }
    }

    override fun get_last_sticker_sets_custom_sync(accountId: Long): Long {
        return getPreferences(app).getLong("last_sticker_sets_sync_custom_$accountId", -1)
    }

    override fun set_last_sticker_sets_custom_sync(accountId: Long, time: Long) {
        getPreferences(app).edit { putLong("last_sticker_sets_sync_custom_$accountId", time) }
    }

    override fun get_last_sticker_keywords_sync(accountId: Long): Long {
        return getPreferences(app).getLong("last_sticker_keywords_sync_$accountId", -1)
    }

    override fun get_last_reaction_assets_sync(accountId: Long): Long {
        return getPreferences(app).getLong("last_reaction_assets_sync_$accountId", -1)
    }

    override fun set_last_sticker_keywords_sync(accountId: Long, time: Long) {
        getPreferences(app).edit { putLong("last_sticker_keywords_sync_$accountId", time) }
    }

    override fun set_last_reaction_assets_sync(accountId: Long, time: Long) {
        getPreferences(app).edit { putLong("last_reaction_assets_sync_$accountId", time) }
    }

    override fun del_last_sticker_sets_sync(accountId: Long) {
        getPreferences(app).edit { remove("last_sticker_sets_sync_$accountId") }
    }

    override fun del_last_sticker_sets_custom_sync(accountId: Long) {
        getPreferences(app).edit { remove("last_sticker_sets_sync_custom_$accountId") }
    }

    override fun del_last_sticker_keywords_sync(accountId: Long) {
        getPreferences(app).edit { remove("last_sticker_keywords_sync_$accountId") }
    }

    override fun del_last_reaction_assets_sync(accountId: Long) {
        getPreferences(app).edit { remove("last_reaction_assets_sync_$accountId") }
    }

    override val is_notification_force_link: Boolean
        get() = getPreferences(app).getBoolean("notification_force_link", false)

    override val isEnable_show_audio_top: Boolean
        get() = getPreferences(app).getBoolean("show_audio_top", false)
    override val isUse_internal_downloader: Boolean
        get() = getPreferences(app).getBoolean("use_internal_downloader", true)

    override val appStoredVersionEqual: Boolean
        get() {
            val ret =
                getPreferences(app).getInt("app_stored_version", 0) == BuildConfig.VERSION_CODE
            if (!ret) {
                getPreferences(app).edit {
                    putInt("app_stored_version", BuildConfig.VERSION_CODE)
                }
            }
            return ret
        }

    override val musicDir: String
        get() {
            var ret = getPreferences(app).getString("music_dir", null)
            if (ret.isNullOrEmpty() || !File(ret).exists()) {
                ret =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
                getPreferences(app).edit { putString("music_dir", ret) }
            }
            return ret!!
        }

    override val photoDir: String
        get() {
            var ret = getPreferences(app).getString("photo_dir", null)
            if (ret.isNullOrEmpty() || !File(ret).exists()) {
                ret =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/Fenrir"
                getPreferences(app).edit { putString("photo_dir", ret) }
            }
            return ret
        }

    override val videoDir: String
        get() {
            var ret = getPreferences(app).getString("video_dir", null)
            if (ret.isNullOrEmpty() || !File(ret).exists()) {
                ret =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath + "/Fenrir"
                getPreferences(app).edit { putString("video_dir", ret) }
            }
            return ret
        }

    override val docDir: String
        get() {
            var ret = getPreferences(app).getString("docs_dir", null)
            if (ret.isNullOrEmpty() || !File(ret).exists()) {
                ret =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Fenrir"
                getPreferences(app).edit { putString("docs_dir", ret) }
            }
            return ret
        }

    override val stickerDir: String
        get() {
            var ret = getPreferences(app).getString("sticker_dir", null)
            if (ret.isNullOrEmpty() || !File(ret).exists()) {
                ret =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Fenrir_Stickers"
                getPreferences(app).edit { putString("sticker_dir", ret) }
            }
            return ret
        }

    @get:ParserType
    override val currentParser: Int
        get() = try {
            getPreferences(app).getString("current_parser", "0")!!
                .trim().toInt()
        } catch (_: Exception) {
            ParserType.JSON
        }
    override val isPhoto_to_user_dir: Boolean
        get() = getPreferences(app).getBoolean("photo_to_user_dir", true)
    override val isDownload_voice_ogg: Boolean
        get() = getPreferences(app).getBoolean("download_voice_ogg", true)
    override val isDelete_cache_images: Boolean
        get() = getPreferences(app).getBoolean("delete_cache_images", false)
    override val isCompress_incoming_traffic: Boolean
        get() = getPreferences(app).getBoolean("compress_incoming_traffic", true)
    override val isCompress_outgoing_traffic: Boolean
        get() = getPreferences(app).getBoolean("compress_outgoing_traffic", false)
    override val isDo_not_clear_back_stack: Boolean
        get() = getPreferences(app).getBoolean("do_not_clear_back_stack", false)
    override val isMention_fave: Boolean
        get() = getPreferences(app).getBoolean("mention_fave", false)
    override val isDisabled_encryption: Boolean
        get() = getPreferences(app).getBoolean("disable_encryption", false)
    override val isDownload_photo_tap: Boolean
        get() = getPreferences(app).getBoolean("download_photo_tap", true)
    override val isAudio_save_mode_button: Boolean
        get() = getPreferences(app).getBoolean("audio_save_mode_button", true)
    override val isShow_mutual_count: Boolean
        get() = getPreferences(app).getBoolean("show_mutual_count", false)
    override val isDo_zoom_photo: Boolean
        get() = getPreferences(app).getBoolean("do_zoom_photo", true)
    override val isChange_upload_size: Boolean
        get() = getPreferences(app).getBoolean("change_upload_size", false)
    override val isInstant_photo_display: Boolean
        get() {
            if (!getPreferences(app).contains("instant_photo_display")) {
                getPreferences(app).edit {
                    putBoolean(
                        "instant_photo_display",
                        FenrirNative.isNativeLoaded && FileUtils.threadsCount > 4
                    )
                }
            }
            return getPreferences(app).getBoolean("instant_photo_display", false)
        }
    override val isShow_photos_line: Boolean
        get() = getPreferences(app).getBoolean("show_photos_line", true)
    override val isShow_photos_date: Boolean
        get() = getPreferences(app).getBoolean("show_photos_date", false)
    override val isDo_auto_play_video: Boolean
        get() = getPreferences(app).getBoolean("do_auto_play_video", false)
    override val isVideo_controller_to_decor: Boolean
        get() = getPreferences(app).getBoolean("video_controller_to_decor", false)
    override val isVideo_swipes: Boolean
        get() = getPreferences(app).getBoolean("video_swipes", true)
    override var isDisable_likes: Boolean
        get() = getPreferences(app).getBoolean("disable_likes", false)
        set(disabled) {
            getPreferences(app).edit { putBoolean("disable_likes", disabled) }
        }
    override var isRememberLocalAudioAlbum: Boolean
        get() = getPreferences(app).getBoolean("remember_local_audio_album", false)
        set(remember) {
            if (!remember) {
                getPreferences(app).edit { remove("current_local_audio_album") }
            }
            getPreferences(app).edit { putBoolean("remember_local_audio_album", remember) }
        }
    override var currentLocalAudioAlbum: Int
        get() = getPreferences(app).getInt("current_local_audio_album", 0)
        set(current) {
            getPreferences(app).edit { putInt("current_local_audio_album", current) }
        }
    override var isDisable_notifications: Boolean
        get() = getPreferences(app).getBoolean("disable_notifications", false)
        set(disabled) {
            getPreferences(app).edit { putBoolean("disable_notifications", disabled) }
        }
    override val isNative_parcel_photo: Boolean
        get() = getPreferences(app).getBoolean("native_parcel_photo", true)
    override val isValidate_tls: Boolean
        get() = getPreferences(app).getBoolean("validate_tls", true)
    override val isNative_parcel_story: Boolean
        get() = getPreferences(app).getBoolean("native_parcel_story", true)
    override val isNative_parcel_docs: Boolean
        get() = getPreferences(app).getBoolean("native_parcel_docs", true)
    override val isDoLogs: Boolean
        get() = isDeveloper_mode && getPreferences(app).getBoolean("do_logs", false)
    override val isDump_fcm: Boolean
        get() = isDoLogs && getPreferences(app).getBoolean("dump_fcm", false)
    override val isHint_stickers: Boolean
        get() = getPreferences(app).getBoolean("hint_stickers", true)
    override val isEnable_native: Boolean
        get() = getPreferences(app).getBoolean("enable_native", true)
    override val isRecording_to_opus: Boolean
        get() = getPreferences(app).getBoolean("recording_to_opus", false)
    override val isDisable_sensored_voice: Boolean
        get() = getPreferences(app).getBoolean("disable_sensored_voice", false)
    override var isInvertPhotoRev: Boolean
        get() = getPreferences(app).getBoolean("invert_photo_rev", false)
        set(rev) {
            getPreferences(app).edit { putBoolean("invert_photo_rev", rev) }
        }
    override var localServer: LocalServerSettings
        get() {
            val ret = getPreferences(app).getString("local_media_server", null)
            return if (ret == null) {
                LocalServerSettings()
            } else {
                kJson.decodeFromString(LocalServerSettings.serializer(), ret)
            }
        }
        set(settings) {
            getPreferences(app).edit {
                putString(
                    "local_media_server",
                    kJson.encodeToString(LocalServerSettings.serializer(), settings)
                )
            }
        }
    override var playerCoverBackgroundSettings: PlayerCoverBackgroundSettings
        get() {
            val ret = getPreferences(app).getString("player_background_settings_json", null)
            return if (ret == null) {
                PlayerCoverBackgroundSettings().set_default()
            } else {
                kJson.decodeFromString(PlayerCoverBackgroundSettings.serializer(), ret)
            }
        }
        set(settings) {
            getPreferences(app).edit {
                putString(
                    "player_background_settings_json",
                    kJson.encodeToString(PlayerCoverBackgroundSettings.serializer(), settings)
                )
            }
        }
    override var slidrSettings: SlidrSettings
        get() {
            val ret = getPreferences(app).getString("slidr_settings_json", null)
            return if (ret == null) {
                SlidrSettings().set_default()
            } else {
                kJson.decodeFromString(SlidrSettings.serializer(), ret)
            }
        }
        set(settings) {
            getPreferences(app).edit {
                putString(
                    "slidr_settings_json",
                    kJson.encodeToString(SlidrSettings.serializer(), settings)
                )
            }
        }

    override var catalogV2ListSort: List<Int>
        get() {
            val ret = getPreferences(app).getString("catalog_v2_list_json", null)
            return if (ret == null) {
                listOf(
                    CatalogV2SortListCategory.TYPE_CATALOG,
                    CatalogV2SortListCategory.TYPE_LOCAL_AUDIO,
                    CatalogV2SortListCategory.TYPE_LOCAL_SERVER_AUDIO,
                    CatalogV2SortListCategory.TYPE_AUDIO,
                    CatalogV2SortListCategory.TYPE_PLAYLIST,
                    CatalogV2SortListCategory.TYPE_RECOMMENDATIONS
                )
            } else {
                kJson.decodeFromString(ListSerializer(Int.serializer()), ret)
            }
        }
        set(settings) {
            getPreferences(app).edit {
                putString(
                    "catalog_v2_list_json",
                    kJson.encodeToString(ListSerializer(Int.serializer()), settings)
                )
            }
        }

    @get:Lang
    override val language: Int
        get() = try {
            getPreferences(app).getString("language_ui", "0")!!
                .trim().toInt()
        } catch (_: Exception) {
            Lang.DEFAULT
        }
    override val rendering_mode: Int
        get() = try {
            getPreferences(app).getString("rendering_bitmap_mode", "0")!!
                .trim().toInt()
        } catch (_: Exception) {
            0
        }
    override val endListAnimation: Int
        get() = try {
            getPreferences(app).getString("end_list_anim", "0")!!
                .trim().toInt()
        } catch (_: Exception) {
            0
        }

    override val isAudio_catalog_v2: Boolean
        get() = getPreferences(app).getBoolean(
            "audio_catalog_v2_enable",
            true
        ) && Utils.isOfficialVKCurrent

    override val isRunes_show: Boolean
        get() = getPreferences(app).getBoolean("runes_show", true)

    override val paganSymbol: Int
        get() = try {
            getPreferences(app).getString("pagan_symbol", "1")!!
                .trim().toInt()
        } catch (_: Exception) {
            1
        }
    override val customChannelNotif: Int
        get() = getPreferences(app).getInt("custom_notification_channel", 0)

    override fun nextCustomChannelNotif() {
        val vl = customChannelNotif
        getPreferences(app).edit { putInt("custom_notification_channel", vl + 1) }
    }

    override val videoExt: Set<String>
        get() = getPreferences(app)
            .getStringSet("videos_ext", setOf("mp4", "avi", "mov", "mpeg"))!!

    override val photoExt: Set<String>
        get() = getPreferences(app)
            .getStringSet("photo_ext", setOf("jpg", "jpeg", "heic", "webp", "png", "tiff"))!!

    override val audioExt: Set<String>
        get() = getPreferences(app)
            .getStringSet("audio_ext", setOf("mp3", "ogg", "flac", "opus"))!!

    override val maxThumbResolution: Int
        get() = try {
            getPreferences(app).getString("max_thumb_resolution", "384")!!.trim()
                .toInt()
        } catch (_: Exception) {
            384
        }

    override val isEnable_dirs_files_count: Boolean
        get() = getPreferences(app).getBoolean("enable_dirs_files_count", true)

    override val picassoDispatcher: Int
        get() = try {
            if (!getPreferences(app).contains("picasso_dispatcher")) {
                getPreferences(app).edit {
                    putString(
                        "picasso_dispatcher",
                        if (FenrirNative.isNativeLoaded && FileUtils.threadsCount > 4) "1" else "0"
                    )
                }
            }
            getPreferences(app).getString("picasso_dispatcher", "0")!!
                .trim().toInt()
        } catch (_: Exception) {
            0
        }

    companion object {
        private const val KEY_IMAGE_SIZE = "image_size"
        private const val KEY_JSON_STATE = "json_list_state"
        private const val KEY_USERNAME_UIDS = "user_name_changes_uids"
        internal fun keyForUserNameChanges(userId: Long): String {
            return "custom_user_name_$userId"
        }
    }

    init {
        preferPhotoPreviewSize = empty()
        reloadUserNameChangesSettings(false)
        reloadOwnerChangesMonitor()
    }
}