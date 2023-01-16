package dev.ragnarok.filegallery.settings.backup

import androidx.annotation.Keep
import de.maxr1998.modernpreferences.PreferenceScreen
import dev.ragnarok.filegallery.Includes
import dev.ragnarok.filegallery.kJson
import dev.ragnarok.filegallery.model.tags.TagFull
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.util.serializeble.json.JsonObject
import dev.ragnarok.filegallery.util.serializeble.json.JsonObjectBuilder
import dev.ragnarok.filegallery.util.serializeble.prefs.Preferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

class SettingsBackup {
    @Keep
    @Serializable
    @Suppress("unused")
    class AppPreferencesList {
        //Main
        var app_theme: String? = null
        var night_switch: String? = null
        var theme_overlay: String? = null
        var language_ui: String? = null
        var delete_disabled: Boolean? = null
        var font_size: String? = null
        var local_media_server: String? = null
        var use_internal_downloader: Boolean? = null
        var video_controller_to_decor: Boolean? = null
        var video_swipes: Boolean? = null
        var download_photo_tap: Boolean? = null
        var show_photos_line: Boolean? = null
        var audio_round_icon: Boolean? = null
        var use_long_click_download: Boolean? = null
        var revert_play_audio: Boolean? = null
        var player_has_background: Boolean? = null
        var player_background: String? = null
        var slidr_settings: String? = null
        var use_stop_audio: Boolean? = null
        var audio_save_mode_button: Boolean? = null
        var show_mini_player: Boolean? = null
        var lifecycle_music_service: String? = null
        var ffmpeg_audio_codecs: String? = null
        var music_dir: String? = null
        var photo_dir: String? = null
        var video_dir: String? = null
        var photo_to_user_dir: Boolean? = null
        var developer_mode: Boolean? = null
        var videos_ext: Set<String>? = null
        var photo_ext: Set<String>? = null
        var audio_ext: Set<String>? = null
        var max_bitmap_resolution: String? = null
        var max_thumb_resolution: String? = null
        var rendering_bitmap_mode: String? = null
        var enable_dirs_files_count: Boolean? = null
        var viewpager_page_transform: String? = null
        var player_cover_transform: String? = null
        var ongoing_player_notification: Boolean? = null
        var open_folder_new_window: Boolean? = null
        var current_parser: String? = null
        var compress_incoming_traffic: Boolean? = null
        var limit_cache_images: String? = null
    }

    fun doBackup(): JsonObject {
        val pref =
            PreferenceScreen.getPreferences(Includes.provideApplicationContext())
        val preferences = Preferences(pref)
        val ret = JsonObjectBuilder()
        ret.put(
            "app",
            kJson.encodeToJsonElement(
                AppPreferencesList.serializer(),
                preferences.decode(AppPreferencesList.serializer(), "")
            )
        )
        val yu = Includes.stores.searchQueriesStore().getTagFull().blockingGet()
        if (yu.nonNullNoEmpty()) {
            ret.put("tags", kJson.encodeToJsonElement(ListSerializer(TagFull.serializer()), yu))
        }
        return ret.build()
    }

    fun doRestore(ret: JsonObject?) {
        ret ?: return
        val pref =
            PreferenceScreen.getPreferences(Includes.provideApplicationContext())

        val preferences = Preferences(pref)

        ret["app"]?.let {
            preferences.encode(
                AppPreferencesList.serializer(),
                "",
                kJson.decodeFromJsonElement(AppPreferencesList.serializer(), it)
            )
        }
        ret["tags"]?.let {
            val tagsList: List<TagFull> =
                kJson.decodeFromJsonElement(ListSerializer(TagFull.serializer()), it)
            if (tagsList.nonNullNoEmpty()) {
                for (i in tagsList) {
                    i.reverseList()
                }
                Includes.stores.searchQueriesStore().putTagFull(tagsList.reversed()).blockingAwait()
            }
        }
    }
}
