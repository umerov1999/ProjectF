package dev.ragnarok.filegallery.model.menu.options

import androidx.annotation.IntDef

@IntDef(
    FileManagerOption.play_item_audio,
    FileManagerOption.play_item_after_current_audio,
    FileManagerOption.bitrate_item_audio,
    FileManagerOption.share_item,
    FileManagerOption.open_with_item,
    FileManagerOption.fix_dir_time_item,
    FileManagerOption.update_file_time_item,
    FileManagerOption.add_dir_tag_item,
    FileManagerOption.delete_item,
    FileManagerOption.play_via_local_server
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class FileManagerOption {
    companion object {
        const val play_item_audio = 1
        const val play_item_after_current_audio = 2
        const val bitrate_item_audio = 3
        const val share_item = 4
        const val open_with_item = 5
        const val fix_dir_time_item = 6
        const val update_file_time_item = 7
        const val add_dir_tag_item = 8
        const val delete_item = 9
        const val play_via_local_server = 10
    }
}