package dev.ragnarok.fenrir.fragment.wallattachments

import androidx.fragment.app.Fragment
import dev.ragnarok.fenrir.fragment.wallattachments.wallaudiosattachments.WallAudiosAttachmentsFragment
import dev.ragnarok.fenrir.fragment.wallattachments.walldocsattachments.WallDocsAttachmentsFragment
import dev.ragnarok.fenrir.fragment.wallattachments.walllinksattachments.WallLinksAttachmentsFragment
import dev.ragnarok.fenrir.fragment.wallattachments.wallphotoalbumattachments.WallPhotoAlbumAttachmentsFragment
import dev.ragnarok.fenrir.fragment.wallattachments.wallphotosattachments.WallPhotosAttachmentsFragment
import dev.ragnarok.fenrir.fragment.wallattachments.wallpostcommentattachments.WallPostCommentAttachmentsFragment
import dev.ragnarok.fenrir.fragment.wallattachments.wallpostqueryattachments.WallPostQueryAttachmentsFragment
import dev.ragnarok.fenrir.fragment.wallattachments.wallvideosattachments.WallVideosAttachmentsFragment
import dev.ragnarok.fenrir.util.FindAttachmentType

object WallAttachmentsFragmentFactory {
    fun newInstance(accountId: Long, ownerId: Long, type: String?): Fragment? {
        requireNotNull(type) { "Type cant bee null" }
        var fragment: Fragment? = null
        when (type) {
            FindAttachmentType.TYPE_PHOTO -> fragment =
                WallPhotosAttachmentsFragment.newInstance(accountId, ownerId)
            FindAttachmentType.TYPE_VIDEO -> fragment =
                WallVideosAttachmentsFragment.newInstance(accountId, ownerId)
            FindAttachmentType.TYPE_DOC -> fragment =
                WallDocsAttachmentsFragment.newInstance(accountId, ownerId)
            FindAttachmentType.TYPE_LINK -> fragment =
                WallLinksAttachmentsFragment.newInstance(accountId, ownerId)
            FindAttachmentType.TYPE_AUDIO -> fragment =
                WallAudiosAttachmentsFragment.newInstance(accountId, ownerId)
            FindAttachmentType.TYPE_POST_WITH_COMMENT -> fragment =
                WallPostCommentAttachmentsFragment.newInstance(accountId, ownerId)
            FindAttachmentType.TYPE_ALBUM -> fragment =
                WallPhotoAlbumAttachmentsFragment.newInstance(accountId, ownerId)
            FindAttachmentType.TYPE_POST_WITH_QUERY -> fragment =
                WallPostQueryAttachmentsFragment.newInstance(accountId, ownerId)
        }
        return fragment
    }
}