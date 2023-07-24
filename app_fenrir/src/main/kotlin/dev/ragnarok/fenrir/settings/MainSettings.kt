package dev.ragnarok.fenrir.settings

import android.annotation.SuppressLint
import android.content.Context
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.settings.ISettings.IMainSettings
import dev.ragnarok.fenrir.settings.theme.ThemeOverlay
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.view.pager.Transformers_Types

internal class MainSettings(context: Context) : IMainSettings {
    private val app: Context = context.applicationContext
    private var prefferedPhotoPreviewSize: Optional<Int>
    override val isSendByEnter: Boolean
        get() = getPreferences(app).getBoolean("send_by_enter", false)

    @get:ThemeOverlay
    override val themeOverlay: Int
        get() = try {
            getPreferences(app).getString("theme_overlay", "0")?.trim { it <= ' ' }?.toInt()
                ?: ThemeOverlay.OFF
        } catch (e: Exception) {
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
            getPreferences(app).edit().putString(KEY_IMAGE_SIZE, uploadImgSize?.toString() ?: "0")
                .apply()
        }

    override val uploadImageSizePref: Int
        get() = try {
            getPreferences(app).getString(KEY_IMAGE_SIZE, "0")?.trim { it <= ' ' }?.toInt() ?: 2
        } catch (e: Exception) {
            0
        }
    override val start_newsMode: Int
        get() = try {
            getPreferences(app).getString("start_news", "2")?.trim { it <= ' ' }?.toInt() ?: 2
        } catch (e: Exception) {
            2
        }

    @get:PhotoSize
    @get:SuppressLint("WrongConstant")
    override val prefPreviewImageSize: Int
        get() {
            if (prefferedPhotoPreviewSize.isEmpty) {
                prefferedPhotoPreviewSize = wrap(restorePhotoPreviewSize())
            }
            return prefferedPhotoPreviewSize.get()!!
        }

    override val cryptVersion: Int
        get() = try {
            getPreferences(app).getString("crypt_version", "1")?.trim { it <= ' ' }?.toInt() ?: 1
        } catch (e: Exception) {
            1
        }

    @PhotoSize
    private fun restorePhotoPreviewSize(): Int {
        return try {
            getPreferences(app).getString("photo_preview_size", PhotoSize.Y.toString())!!
                .trim { it <= ' ' }
                .toInt()
        } catch (e: Exception) {
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
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            Transformers_Types.OFF
        }

    @get:Transformers_Types
    override val player_cover_transform: Int
        get() = try {
            getPreferences(app).getString(
                "player_cover_transform",
                Transformers_Types.DEPTH_TRANSFORMER.toString()
            )!!
                .trim { it <= ' ' }.toInt()
        } catch (e: Exception) {
            Transformers_Types.DEPTH_TRANSFORMER
        }

    override fun notifyPrefPreviewSizeChanged() {
        prefferedPhotoPreviewSize = empty()
    }

    @PhotoSize
    override fun getPrefDisplayImageSize(@PhotoSize byDefault: Int): Int {
        return getPreferences(app).getInt("pref_display_photo_size", byDefault)
    }

    override val photoRoundMode: Int
        get() = try {
            getPreferences(app).getString("photo_rounded_view", "0")?.trim { it <= ' ' }?.toInt()
                ?: 0
        } catch (e: Exception) {
            0
        }
    override val fontSize: Int
        get() = getPreferences(app).getInt("font_size_int", 0)

    override val fontOnlyForChats: Boolean
        get() = getPreferences(app).getBoolean("font_only_for_chats", false)

    override fun setPrefDisplayImageSize(@PhotoSize size: Int) {
        getPreferences(app)
            .edit()
            .putInt("pref_display_photo_size", size)
            .apply()
    }

    override val isOpenUrlInternal: Int
        get() = try {
            getPreferences(app).getString("is_open_url_internal", "1")?.trim { it <= ' ' }?.toInt()
                ?: 1
        } catch (e: Exception) {
            1
        }
    override val isWebview_night_mode: Boolean
        get() = getPreferences(app).getBoolean("webview_night_mode", true)
    override val isLoad_history_notif: Boolean
        get() = getPreferences(app).getBoolean("load_history_notif", false)
    override val isSnow_mode: Boolean
        get() = getPreferences(app).getBoolean("snow_mode", false)
    override val isDont_write: Boolean
        get() = getPreferences(app).getBoolean("dont_write", false)
    override val isOver_ten_attach: Boolean
        get() = getPreferences(app).getBoolean("over_ten_attach", false)

    companion object {
        private const val KEY_IMAGE_SIZE = "image_size"
    }

    init {
        prefferedPhotoPreviewSize = empty()
    }
}