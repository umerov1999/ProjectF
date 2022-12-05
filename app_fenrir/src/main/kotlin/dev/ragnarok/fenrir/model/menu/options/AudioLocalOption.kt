package dev.ragnarok.fenrir.model.menu.options

import androidx.annotation.IntDef

@IntDef(
    AudioLocalOption.play_item_audio,
    AudioLocalOption.upload_item_audio,
    AudioLocalOption.play_item_after_current_audio,
    AudioLocalOption.play_via_local_server,
    AudioLocalOption.bitrate_item_audio,
    AudioLocalOption.delete_item_audio,
    AudioLocalOption.strip_metadata_item_audio
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class AudioLocalOption {
    companion object {
        const val play_item_audio = 1
        const val upload_item_audio = 2
        const val play_item_after_current_audio = 3
        const val play_via_local_server = 4
        const val bitrate_item_audio = 5
        const val delete_item_audio = 6
        const val strip_metadata_item_audio = 7
    }
}