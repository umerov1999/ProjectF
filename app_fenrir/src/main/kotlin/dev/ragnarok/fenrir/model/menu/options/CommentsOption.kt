package dev.ragnarok.fenrir.model.menu.options

import androidx.annotation.IntDef

@IntDef(
    CommentsOption.copy_item_comment,
    CommentsOption.reply_item_comment,
    CommentsOption.report_item_comment,
    CommentsOption.delete_item_comment,
    CommentsOption.edit_item_comment,
    CommentsOption.block_author_item_comment,
    CommentsOption.like_item_comment,
    CommentsOption.dislike_item_comment,
    CommentsOption.who_like_item_comment,
    CommentsOption.send_to_friend_item_comment
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class CommentsOption {
    companion object {
        const val copy_item_comment = 1
        const val reply_item_comment = 2
        const val report_item_comment = 3
        const val delete_item_comment = 4
        const val edit_item_comment = 5
        const val block_author_item_comment = 6
        const val like_item_comment = 7
        const val dislike_item_comment = 8
        const val who_like_item_comment = 9
        const val send_to_friend_item_comment = 10
    }
}