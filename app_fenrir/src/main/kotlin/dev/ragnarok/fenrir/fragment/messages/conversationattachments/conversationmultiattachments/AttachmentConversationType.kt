package dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationmultiattachments

import androidx.annotation.IntDef

@IntDef(
    AttachmentConversationType.PHOTO,
    AttachmentConversationType.PHOTO_ALBUM,
    AttachmentConversationType.VIDEO,
    AttachmentConversationType.DOC,
    AttachmentConversationType.LINK,
    AttachmentConversationType.POST,
    AttachmentConversationType.AUDIO
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class AttachmentConversationType {
    companion object {
        const val PHOTO = 0
        const val PHOTO_ALBUM = 1
        const val VIDEO = 2
        const val DOC = 3
        const val LINK = 4
        const val POST = 5
        const val AUDIO = 6
    }
}
