package dev.ragnarok.filegallery.model.menu.options

import androidx.annotation.IntDef

@IntDef(
    AudioLocalServerOption.play_item_audio,
    AudioLocalServerOption.save_item_audio,
    AudioLocalServerOption.play_item_after_current_audio,
    AudioLocalServerOption.play_via_local_server,
    AudioLocalServerOption.bitrate_item_audio,
    AudioLocalServerOption.delete_item_audio,
    AudioLocalServerOption.update_time_item_audio,
    AudioLocalServerOption.edit_item_audio
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class AudioLocalServerOption {
    companion object {
        const val play_item_audio = 1
        const val save_item_audio = 2
        const val play_item_after_current_audio = 3
        const val play_via_local_server = 4
        const val bitrate_item_audio = 5
        const val delete_item_audio = 6
        const val update_time_item_audio = 7
        const val edit_item_audio = 8
    }
}