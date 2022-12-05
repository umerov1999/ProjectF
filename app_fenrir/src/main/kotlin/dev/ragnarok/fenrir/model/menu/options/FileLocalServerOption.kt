package dev.ragnarok.fenrir.model.menu.options

import androidx.annotation.IntDef

@IntDef(
    FileLocalServerOption.play_item_audio,
    FileLocalServerOption.save_item,
    FileLocalServerOption.play_item_after_current_audio,
    FileLocalServerOption.delete_item,
    FileLocalServerOption.update_time_item,
    FileLocalServerOption.edit_item,
    FileLocalServerOption.upload_item_audio
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class FileLocalServerOption {
    companion object {
        const val play_item_audio = 1
        const val save_item = 2
        const val play_item_after_current_audio = 3
        const val delete_item = 4
        const val update_time_item = 5
        const val edit_item = 6
        const val upload_item_audio = 7
    }
}