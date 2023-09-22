package dev.ragnarok.fenrir.settings.backup

import androidx.annotation.Keep
import de.maxr1998.modernpreferences.PreferenceScreen
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.model.ShortcutStored
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.serializeble.json.JsonObject
import dev.ragnarok.fenrir.util.serializeble.json.JsonObjectBuilder
import dev.ragnarok.fenrir.util.serializeble.prefs.Preferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class SettingsBackup {
    @Keep
    @Serializable
    @Suppress("unused")
    class AppPreferencesList {
        //Main
        var send_by_enter: Boolean? = null
        var theme_overlay: String? = null
        var audio_round_icon: Boolean? = null
        var use_long_click_download: Boolean? = null
        var revert_play_audio: Boolean? = null
        var is_player_support_volume: Boolean? = null
        var show_bot_keyboard: Boolean? = null
        var my_message_no_color: Boolean? = null
        var notification_bubbles: Boolean? = null
        var messages_menu_down: Boolean? = null
        var expand_voice_transcript: Boolean? = null
        var chat_popup_menu: Boolean? = null
        var image_size: String? = null
        var start_news: String? = null
        var crypt_version: String? = null
        var photo_preview_size: String? = null
        var viewpager_page_transform: String? = null
        var player_cover_transform: String? = null
        var pref_display_photo_size: Int? = null
        var photo_rounded_view: String? = null
        var font_size_int: Int? = null
        var font_only_for_chats: Boolean? = null
        var is_open_url_internal: String? = null
        var webview_night_mode: Boolean? = null
        var load_history_notif: Boolean? = null
        var snow_mode: Boolean? = null
        var dont_write: Boolean? = null
        var over_ten_attach: Boolean? = null

        //UI
        var avatar_style: Int? = null
        var app_theme: String? = null
        var night_switch: String? = null
        var default_category: String? = null
        var last_closed_place_type: Int? = null
        var emojis_type: Boolean? = null
        var emojis_full_screen: Boolean? = null
        var stickers_by_theme: Boolean? = null
        var stickers_by_new: Boolean? = null
        var show_profile_in_additional_page: Boolean? = null
        var display_writing: Boolean? = null

        //Other
        var swipes_for_chats: String? = null
        var broadcast: Boolean? = null
        var comments_desc: Boolean? = null
        var keep_longpoll: Boolean? = null
        var disable_error_fcm: Boolean? = null
        var settings_no_push: Boolean? = null
        var videos_ext: Set<String>? = null
        var photo_ext: Set<String>? = null
        var audio_ext: Set<String>? = null
        var enable_dirs_files_count: Boolean? = null
        var max_bitmap_resolution: String? = null
        var max_thumb_resolution: String? = null
        var ffmpeg_audio_codecs: String? = null
        var lifecycle_music_service: String? = null
        var autoplay_gif: Boolean? = null
        var strip_news_repost: Boolean? = null
        var communities_in_page_search: Boolean? = null
        var ad_block_story_news: Boolean? = null
        var block_news_by_words_set: Set<String>? = null
        var new_loading_dialog: Boolean? = null
        var vk_api_domain: String? = null
        var vk_auth_domain: String? = null
        var developer_mode: Boolean? = null
        var do_logs: Boolean? = null
        var force_cache: Boolean? = null
        var disable_history: Boolean? = null
        var show_wall_cover: Boolean? = null
        var custom_chat_color: Int? = null
        var custom_chat_color_second: Int? = null
        var custom_chat_color_usage: Boolean? = null
        var custom_message_color: Int? = null
        var custom_second_message_color: Int? = null
        var custom_message_color_usage: Boolean? = null
        var info_reading: Boolean? = null
        var auto_read: Boolean? = null
        var mark_listened_voice: Boolean? = null
        var not_update_dialogs: Boolean? = null
        var be_online: Boolean? = null
        var donate_anim_set: String? = null
        var use_stop_audio: Boolean? = null
        var player_has_background: Boolean? = null
        var show_mini_player: Boolean? = null
        var enable_last_read: Boolean? = null
        var not_read_show: Boolean? = null
        var headers_in_dialog: Boolean? = null
        var show_recent_dialogs: Boolean? = null
        var show_audio_top: Boolean? = null
        var use_internal_downloader: Boolean? = null
        var music_dir: String? = null
        var photo_dir: String? = null
        var video_dir: String? = null
        var docs_dir: String? = null
        var sticker_dir: String? = null
        var photo_to_user_dir: Boolean? = null
        var download_voice_ogg: Boolean? = null
        var delete_cache_images: Boolean? = null
        var compress_incoming_traffic: Boolean? = null
        var compress_outgoing_traffic: Boolean? = null
        var limit_cache_images: String? = null
        var do_not_clear_back_stack: Boolean? = null
        var mention_fave: Boolean? = null
        var disable_encryption: Boolean? = null
        var download_photo_tap: Boolean? = null
        var audio_save_mode_button: Boolean? = null
        var show_mutual_count: Boolean? = null
        var do_zoom_photo: Boolean? = null
        var change_upload_size: Boolean? = null
        var instant_photo_display: Boolean? = null
        var picasso_dispatcher: String? = null
        var show_photos_line: Boolean? = null
        var show_photos_date: Boolean? = null
        var do_auto_play_video: Boolean? = null
        var video_controller_to_decor: Boolean? = null
        var video_swipes: Boolean? = null
        var disable_likes: Boolean? = null
        var disable_notifications: Boolean? = null
        var native_parcel_photo: Boolean? = null
        var native_parcel_story: Boolean? = null
        var dump_fcm: Boolean? = null
        var hint_stickers: Boolean? = null
        var enable_native: Boolean? = null
        var disable_sensored_voice: Boolean? = null
        var local_media_server: String? = null
        var pagan_symbol: String? = null
        var language_ui: String? = null
        var end_list_anim: String? = null
        var runes_show: Boolean? = null
        var player_background_settings_json: String? = null
        var slidr_settings_json: String? = null
        var catalog_v2_list_json: String? = null
        var use_api_5_90_for_audio: Boolean? = null
        var is_side_navigation: Boolean? = null
        var is_side_no_stroke: Boolean? = null
        var is_side_transition: Boolean? = null
        var notification_force_link: Boolean? = null
        var recording_to_opus: Boolean? = null
        var service_playlists: String? = null
        var rendering_bitmap_mode: String? = null
        var hidden_peers: Set<String>? = null
        var notif_peer_uids: Set<String>? = null
        var user_name_changes_uids: Set<String>? = null
        var owner_changes_monitor_uids: Set<String>? = null
        var current_parser: String? = null
        var audio_catalog_v2_enable: Boolean? = null
        var remember_local_audio_album: Boolean? = null
        var navigation_menu_order: String? = null
        var side_navigation_menu_order: String? = null
    }

    fun doBackup(): JsonObject {
        val pref = PreferenceScreen.getPreferences(Includes.provideApplicationContext())
        val preferences = Preferences(pref)
        val ret = JsonObjectBuilder()
        ret.put(
            "app",
            kJson.encodeToJsonElement(
                AppPreferencesList.serializer(),
                preferences.decode(AppPreferencesList.serializer(), "")
            )
        )
        val notificatios_pointers = HashMap<String, Int>()
        for ((key) in Settings.get().notifications().chatsNotif) {
            if (pref.contains(key)) {
                notificatios_pointers[key] = pref.getInt(key, -1)
            }
        }
        ret.put(
            "notifications_values",
            kJson.encodeToJsonElement(
                MapSerializer(String.serializer(), Int.serializer()),
                notificatios_pointers
            )
        )
        val user_names_pointers = HashMap<String, String>()
        for ((key) in Settings.get().main().userNameChangesMap) {
            if (pref.contains(key)) {
                user_names_pointers[key] = pref.getString(key, null) ?: continue
            }
        }
        ret.put(
            "user_names_values",
            kJson.encodeToJsonElement(
                MapSerializer(String.serializer(), String.serializer()),
                user_names_pointers
            )
        )
        val yu = Includes.stores.tempStore().getShortcutAll().blockingGet()
        if (yu.nonNullNoEmpty()) {
            ret.put(
                "shortcuts",
                kJson.encodeToJsonElement(ListSerializer(ShortcutStored.serializer()), yu)
            )
        }
        return ret.build()
    }

    fun doRestore(ret: JsonObject?) {
        ret ?: return
        val pref =
            PreferenceScreen.getPreferences(Includes.provideApplicationContext())
        val preferences = Preferences(pref)

        for (i in Settings.get().notifications().chatsNotifKeys) {
            pref.edit().remove(i).apply()
        }

        for (i in Settings.get().main().userNameChangesKeys) {
            pref.edit().remove(i).apply()
        }

        ret["app"]?.let {
            preferences.encode(
                AppPreferencesList.serializer(),
                "",
                kJson.decodeFromJsonElement(AppPreferencesList.serializer(), it)
            )
        }

        Settings.get().security().reloadHiddenDialogSettings()
        Settings.get().notifications().reloadNotifSettings(true)
        Settings.get().main().reloadOwnerChangesMonitor()

        ret["notifications_values"]?.let {
            val notificatios_pointers: Map<String, Int> = kJson.decodeFromJsonElement(
                MapSerializer(String.serializer(), Int.serializer()),
                it
            )
            for ((key, value) in notificatios_pointers) {
                pref.edit().putInt(key, value).apply()
            }
        }
        Settings.get().notifications().reloadNotifSettings(false)
        Settings.get().main().reloadUserNameChangesSettings(true)

        ret["user_names_values"]?.let {
            val user_names_pointers: Map<String, String> = kJson.decodeFromJsonElement(
                MapSerializer(String.serializer(), String.serializer()), it
            )
            for ((key, value) in user_names_pointers) {
                pref.edit().putString(key, value).apply()
            }
        }
        Settings.get().main().reloadUserNameChangesSettings(false)

        ret["shortcuts"]?.let {
            val jp = kJson.decodeFromJsonElement(ListSerializer(ShortcutStored.serializer()), it)
            if (jp.nonNullNoEmpty()) {
                Includes.stores.tempStore().addShortcuts(jp.reversed()).blockingAwait()
            }
        }
    }
}
