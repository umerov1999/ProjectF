package dev.ragnarok.fenrir.model.menu.options

import androidx.annotation.IntDef

@IntDef(
    AudioOption.play_item_audio,
    AudioOption.play_item_after_current_audio,
    AudioOption.add_item_audio,
    AudioOption.save_item_audio,
    AudioOption.get_recommendation_by_audio,
    AudioOption.open_album,
    AudioOption.get_lyrics_menu,
    AudioOption.copy_url,
    AudioOption.bitrate_item_audio,
    AudioOption.search_by_artist,
    AudioOption.share_button,
    AudioOption.add_and_download_button,
    AudioOption.goto_artist,
    AudioOption.edit_track
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class AudioOption {
    companion object {
        const val play_item_audio = 1
        const val play_item_after_current_audio = 2
        const val add_item_audio = 3
        const val save_item_audio = 4
        const val get_recommendation_by_audio = 5
        const val open_album = 6
        const val get_lyrics_menu = 7
        const val copy_url = 8
        const val bitrate_item_audio = 9
        const val search_by_artist = 10
        const val share_button = 11
        const val add_and_download_button = 12
        const val goto_artist = 13
        const val edit_track = 14
    }
}