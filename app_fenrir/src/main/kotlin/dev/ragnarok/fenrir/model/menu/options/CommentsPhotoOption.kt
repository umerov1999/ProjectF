package dev.ragnarok.fenrir.model.menu.options

import androidx.annotation.IntDef

@IntDef(
    CommentsPhotoOption.go_to_photo_item_comment,
    CommentsPhotoOption.copy_item_comment,
    CommentsPhotoOption.report_item_comment,
    CommentsPhotoOption.like_item_comment,
    CommentsPhotoOption.dislike_item_comment,
    CommentsPhotoOption.who_like_item_comment,
    CommentsPhotoOption.send_to_friend_item_comment
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class CommentsPhotoOption {
    companion object {
        const val go_to_photo_item_comment = 1
        const val copy_item_comment = 2
        const val report_item_comment = 3
        const val like_item_comment = 4
        const val dislike_item_comment = 5
        const val who_like_item_comment = 6
        const val send_to_friend_item_comment = 7
    }
}