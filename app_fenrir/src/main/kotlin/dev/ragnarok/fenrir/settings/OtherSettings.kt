package dev.ragnarok.fenrir.settings

import android.content.Context
import android.graphics.Color
import android.os.Environment
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.api.model.LocalServerSettings
import dev.ragnarok.fenrir.api.model.PlayerCoverBackgroundSettings
import dev.ragnarok.fenrir.api.model.SlidrSettings
import dev.ragnarok.fenrir.model.Lang
import dev.ragnarok.fenrir.model.ParserType
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_AUDIO
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_CATALOG
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_LOCAL_AUDIO
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_LOCAL_SERVER_AUDIO
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_PLAYLIST
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_RECOMMENDATIONS
import dev.ragnarok.fenrir.settings.ISettings.IOtherSettings
import dev.ragnarok.fenrir.util.Utils
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File
import java.util.Collections

internal class OtherSettings(context: Context) : IOtherSettings {
    private val app: Context = context.applicationContext
    private val userNameChanges: MutableSet<String> = Collections.synchronizedSet(HashSet(1))
    private val types: MutableMap<String, String> = Collections.synchronizedMap(HashMap(1))
    private val ownerChangesMonitor: MutableSet<Long> = Collections.synchronizedSet(HashSet(1))
    override fun getUserNameChangesMap(): Map<String, String> {
        return HashMap(types)
    }

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

    override val userNameChangesKeys: Set<String>
        get() = HashSet(userNameChanges)

    override fun reloadUserNameChangesSettings(onlyRoot: Boolean) {
        val preferences = getPreferences(app)
        userNameChanges.clear()
        userNameChanges.addAll(preferences.getStringSet(KEY_USERNAME_UIDS, HashSet(1)) ?: return)
        if (onlyRoot) {
            return
        }
        types.clear()
        for (i in userNameChanges) {
            val rs = preferences.getString(i, null)
            if (rs.nonNullNoEmpty()) {
                types[i] = rs
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
        preferences.edit()
            .putStringSet("owner_changes_monitor_uids", ownerChangesMonitorSet)
            .apply()
    }

    override fun removeOwnerInChangesMonitor(ownerId: Long) {
        val preferences = getPreferences(app)
        ownerChangesMonitor.remove(ownerId)
        val ownerChangesMonitorSet = HashSet<String>()
        for (i in ownerChangesMonitor) {
            ownerChangesMonitorSet.add(i.toString())
        }
        preferences.edit()
            .putStringSet("owner_changes_monitor_uids", ownerChangesMonitorSet)
            .apply()
    }

    override fun setUserNameChanges(userId: Long, name: String?) {
        val preferences = getPreferences(app)
        if (name.isNullOrEmpty()) {
            userNameChanges.remove(keyForUserNameChanges(userId))
            types.remove(keyForUserNameChanges(userId))
            preferences.edit()
                .remove(keyForUserNameChanges(userId))
                .putStringSet(KEY_USERNAME_UIDS, userNameChanges)
                .apply()
        } else {
            userNameChanges.add(keyForUserNameChanges(userId))
            types[keyForUserNameChanges(userId)] = name
            preferences.edit()
                .putString(keyForUserNameChanges(userId), name)
                .putStringSet(KEY_USERNAME_UIDS, userNameChanges)
                .apply()
        }
    }

    override fun getUserNameChanges(userId: Long): String? {
        if (types.containsKey(keyForUserNameChanges(userId))) {
            val v = types[keyForUserNameChanges(userId)]
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
            .edit()
            .putString("source_ids$accountId", sourceIds)
            .apply()
    }

    override fun storeFeedScrollState(accountId: Long, state: String?) {
        if (state != null) {
            getPreferences(app)
                .edit()
                .putString(KEY_JSON_STATE + accountId, state)
                .apply()
        } else {
            getPreferences(app)
                .edit()
                .remove(KEY_JSON_STATE + accountId)
                .apply()
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
            .edit()
            .putString("next_from$accountId", nextFrom)
            .apply()
    }

    override val isAudioBroadcastActive: Boolean
        get() = getPreferences(app).getBoolean("broadcast", false)
    override val isCommentsDesc: Boolean
        get() = getPreferences(app).getBoolean("comments_desc", true)

    override fun toggleCommentsDirection(): Boolean {
        val descNow = isCommentsDesc
        getPreferences(app)
            .edit()
            .putBoolean("comments_desc", !descNow)
            .apply()
        return !descNow
    }

    override val isKeepLongpoll: Boolean
        get() = getPreferences(app).getBoolean("keep_longpoll", false)

    override fun setDisableErrorFCM(en: Boolean) {
        getPreferences(app).edit().putBoolean("disable_error_fcm", en).apply()
    }

    override val isDisabledErrorFCM: Boolean
        get() = getPreferences(app).getBoolean("disable_error_fcm", false)
    override val isSettings_no_push: Boolean
        get() = getPreferences(app).getBoolean("settings_no_push", false)
    override val maxBitmapResolution: Int
        get() = try {
            getPreferences(app).getString("max_bitmap_resolution", "4000")!!
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            4000
        }
    override val servicePlaylist: List<Int>
        get() = try {
            val rs = getPreferences(app).getString("service_playlists", "-21 -22 -25 -26 -27 -28")!!
                .trim { it <= ' ' }
            if (rs.isEmpty()) {
                emptyList()
            } else {
                val integerStrings = rs.split(Regex(" ")).toTypedArray()
                if (integerStrings.isEmpty()) {
                    emptyList()
                } else {
                    val integers: MutableList<Int> = ArrayList(integerStrings.size)
                    for (i in integerStrings.indices) {
                        integers.add(i, integerStrings[i].trim { it <= ' ' }.toInt())
                    }
                    integers
                }
            }
        } catch (e: Exception) {
            listOf(-21, -22, -25, -26, -27, -28)
        }
    override val fFmpegPlugin: Int
        get() = try {
            getPreferences(app).getString("ffmpeg_audio_codecs", "1")!!
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            1
        }

    override val isLimitImage_cache: Int
        get() = try {
            getPreferences(app).getString("limit_cache_images", "2")!!
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            2
        }

    override val musicLifecycle: Int
        get() = try {
            var v = getPreferences(app).getString(
                "lifecycle_music_service", Constants.AUDIO_PLAYER_SERVICE_IDLE.toString()
            )!!
                .trim { it <= ' ' }.toInt()
            if (v < 60000) {
                getPreferences(app).edit().putString("lifecycle_music_service", "60000").apply()
                v = 60000
            }
            v
        } catch (e: Exception) {
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

    override fun get_Api_Domain(): String {
        return getPreferences(app)
            .getString("vk_api_domain", "api.vk.com")!!.trim { it <= ' ' }
    }

    override fun get_Auth_Domain(): String {
        return getPreferences(app)
            .getString("vk_auth_domain", "oauth.vk.com")!!.trim { it <= ' ' }
    }

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
        get() = getPreferences(app).getInt("custom_message_color", Color.parseColor("#CBD438FF"))
    override val secondColorMyMessage: Int
        get() = getPreferences(app).getInt(
            "custom_second_message_color",
            Color.parseColor("#BF6539DF")
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
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
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

    override fun is_side_navigation(): Boolean {
        return getPreferences(app).getBoolean("is_side_navigation", false)
    }

    override fun is_side_no_stroke(): Boolean {
        return getPreferences(app).getBoolean("is_side_no_stroke", false)
    }

    override fun is_side_transition(): Boolean {
        return getPreferences(app).getBoolean("is_side_transition", true)
    }

    override fun get_last_audio_sync(): Long {
        return getPreferences(app).getLong("last_audio_sync", -1)
    }

    override fun set_last_audio_sync(time: Long) {
        getPreferences(app).edit().putLong("last_audio_sync", time).apply()
    }

    override fun get_last_sticker_sets_sync(accountId: Long): Long {
        return getPreferences(app).getLong("last_sticker_sets_sync_$accountId", -1)
    }

    override fun set_last_sticker_sets_sync(accountId: Long, time: Long) {
        getPreferences(app).edit().putLong("last_sticker_sets_sync_$accountId", time).apply()
    }

    override fun get_last_sticker_sets_custom_sync(accountId: Long): Long {
        return getPreferences(app).getLong("last_sticker_sets_sync_custom_$accountId", -1)
    }

    override fun set_last_sticker_sets_custom_sync(accountId: Long, time: Long) {
        getPreferences(app).edit().putLong("last_sticker_sets_sync_custom_$accountId", time).apply()
    }

    override fun get_last_sticker_keywords_sync(accountId: Long): Long {
        return getPreferences(app).getLong("last_sticker_keywords_sync_$accountId", -1)
    }

    override fun set_last_sticker_keywords_sync(accountId: Long, time: Long) {
        getPreferences(app).edit().putLong("last_sticker_keywords_sync_$accountId", time).apply()
    }

    override fun del_last_sticker_sets_sync(accountId: Long) {
        getPreferences(app).edit().remove("last_sticker_sets_sync_$accountId").apply()
    }

    override fun del_last_sticker_sets_custom_sync(accountId: Long) {
        getPreferences(app).edit().remove("last_sticker_sets_sync_custom_$accountId").apply()
    }

    override fun del_last_sticker_keywords_sync(accountId: Long) {
        getPreferences(app).edit().remove("last_sticker_keywords_sync_$accountId").apply()
    }

    override fun is_notification_force_link(): Boolean {
        return getPreferences(app).getBoolean("notification_force_link", false)
    }

    override val isEnable_show_audio_top: Boolean
        get() = getPreferences(app).getBoolean("show_audio_top", false)
    override val isUse_internal_downloader: Boolean
        get() = getPreferences(app).getBoolean("use_internal_downloader", true)

    override fun appStoredVersionEqual(): Boolean {
        val ret = getPreferences(app).getInt("app_stored_version", 0) == BuildConfig.VERSION_CODE
        if (!ret) {
            getPreferences(app).edit().putInt("app_stored_version", BuildConfig.VERSION_CODE)
                .apply()
        }
        return ret
    }

    @Suppress("DEPRECATION")
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

    @Suppress("DEPRECATION")
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

    @Suppress("DEPRECATION")
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

    @Suppress("DEPRECATION")
    override val docDir: String
        get() {
            var ret = getPreferences(app).getString("docs_dir", null)
            if (ret.isNullOrEmpty() || !File(ret).exists()) {
                ret =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Fenrir"
                getPreferences(app).edit().putString("docs_dir", ret).apply()
            }
            return ret
        }

    @Suppress("DEPRECATION")
    override val stickerDir: String
        get() {
            var ret = getPreferences(app).getString("sticker_dir", null)
            if (ret.isNullOrEmpty() || !File(ret).exists()) {
                ret =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Fenrir_Stickers"
                getPreferences(app).edit().putString("sticker_dir", ret).apply()
            }
            return ret
        }

    @get:ParserType
    override val currentParser: Int
        get() = try {
            getPreferences(app).getString("current_parser", "0")!!
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
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
            getPreferences(app).edit().putBoolean("disable_likes", disabled).apply()
        }
    override var isDisable_notifications: Boolean
        get() = getPreferences(app).getBoolean("disable_notifications", false)
        set(disabled) {
            getPreferences(app).edit().putBoolean("disable_notifications", disabled).apply()
        }
    override val isNative_parcel_photo: Boolean
        get() = getPreferences(app).getBoolean("native_parcel_photo", true)
    override val isValidate_tls: Boolean
        get() = getPreferences(app).getBoolean("validate_tls", true)
    override val isNative_parcel_story: Boolean
        get() = getPreferences(app).getBoolean("native_parcel_story", true)
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
    override val isOngoing_player_notification: Boolean
        get() = getPreferences(app).getBoolean("ongoing_player_notification", false)
    override var isInvertPhotoRev: Boolean
        get() = getPreferences(app).getBoolean("invert_photo_rev", false)
        set(rev) {
            getPreferences(app).edit().putBoolean("invert_photo_rev", rev).apply()
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
            getPreferences(app).edit().putString(
                "local_media_server",
                kJson.encodeToString(LocalServerSettings.serializer(), settings)
            )
                .apply()
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
            getPreferences(app).edit()
                .putString(
                    "player_background_settings_json",
                    kJson.encodeToString(PlayerCoverBackgroundSettings.serializer(), settings)
                ).apply()
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
            getPreferences(app).edit().putString(
                "slidr_settings_json",
                kJson.encodeToString(SlidrSettings.serializer(), settings)
            )
                .apply()
        }

    override var catalogV2ListSort: List<Int>
        get() {
            val ret = getPreferences(app).getString("catalog_v2_list_json", null)
            return if (ret == null) {
                listOf(
                    TYPE_CATALOG,
                    TYPE_LOCAL_AUDIO,
                    TYPE_LOCAL_SERVER_AUDIO,
                    TYPE_AUDIO,
                    TYPE_PLAYLIST,
                    TYPE_RECOMMENDATIONS
                )
            } else {
                kJson.decodeFromString(ListSerializer(Int.serializer()), ret)
            }
        }
        set(settings) {
            getPreferences(app).edit().putString(
                "catalog_v2_list_json",
                kJson.encodeToString(ListSerializer(Int.serializer()), settings)
            )
                .apply()
        }

    @get:Lang
    override val language: Int
        get() = try {
            getPreferences(app).getString("language_ui", "0")!!
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            Lang.DEFAULT
        }
    override val rendering_mode: Int
        get() = try {
            getPreferences(app).getString("rendering_bitmap_mode", "2")!!
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            2
        }
    override val endListAnimation: Int
        get() = try {
            getPreferences(app).getString("end_list_anim", "0")!!
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
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
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            1
        }
    override val customChannelNotif: Int
        get() = getPreferences(app).getInt("custom_notification_channel", 0)

    override fun nextCustomChannelNotif() {
        val vl = customChannelNotif
        getPreferences(app).edit().putInt("custom_notification_channel", vl + 1).apply()
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

    override fun getMaxThumbResolution(): Int {
        return try {
            getPreferences(app).getString("max_thumb_resolution", "384")!!.trim()
                .toInt()
        } catch (e: Exception) {
            384
        }
    }

    override fun isEnable_dirs_files_count(): Boolean {
        return getPreferences(app).getBoolean("enable_dirs_files_count", true)
    }

    companion object {
        private const val KEY_JSON_STATE = "json_list_state"
        private const val KEY_USERNAME_UIDS = "user_name_changes_uids"
        internal fun keyForUserNameChanges(userId: Long): String {
            return "custom_user_name_$userId"
        }
    }

    init {
        reloadUserNameChangesSettings(false)
        reloadOwnerChangesMonitor()
    }
}