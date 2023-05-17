package dev.ragnarok.fenrir.fragment.wall.wallattachments.wallmultiattachments

import androidx.annotation.IntDef

@IntDef(
    AttachmentWallType.PHOTO,
    AttachmentWallType.PHOTO_ALBUM,
    AttachmentWallType.VIDEO,
    AttachmentWallType.DOC,
    AttachmentWallType.LINK,
    AttachmentWallType.POST_COMMENT,
    AttachmentWallType.AUDIO
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class AttachmentWallType {
    companion object {
        const val PHOTO = 0
        const val PHOTO_ALBUM = 1
        const val VIDEO = 2
        const val DOC = 3
        const val LINK = 4
        const val POST_COMMENT = 5
        const val AUDIO = 6
    }
}
