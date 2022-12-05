package dev.ragnarok.filegallery.model.menu.options

import androidx.annotation.IntDef

@IntDef(
    VideoLocalServerOption.play_item_video,
    VideoLocalServerOption.save_item_video,
    VideoLocalServerOption.bitrate_item_video,
    VideoLocalServerOption.delete_item_video,
    VideoLocalServerOption.update_time_item_video,
    VideoLocalServerOption.edit_item_video
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class VideoLocalServerOption {
    companion object {
        const val play_item_video = 1
        const val save_item_video = 2
        const val bitrate_item_video = 5
        const val delete_item_video = 6
        const val update_time_item_video = 7
        const val edit_item_video = 8
    }
}