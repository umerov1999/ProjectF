package dev.ragnarok.fenrir.fragment.wall.wallattachments

import androidx.fragment.app.Fragment
import dev.ragnarok.fenrir.fragment.wall.wallattachments.wallmultiattachments.WallMultiAttachmentsFragment
import dev.ragnarok.fenrir.fragment.wall.wallattachments.wallpostqueryattachments.WallPostQueryAttachmentsFragment
import dev.ragnarok.fenrir.util.FindAttachmentType

object WallAttachmentsFragmentFactory {
    fun newInstance(accountId: Long, ownerId: Long, type: String?): Fragment? {
        requireNotNull(type) { "Type cant bee null" }
        var fragment: Fragment? = null
        when (type) {
            FindAttachmentType.TYPE_MULTI -> fragment =
                WallMultiAttachmentsFragment.newInstance(accountId, ownerId)

            FindAttachmentType.TYPE_POST_WITH_QUERY -> fragment =
                WallPostQueryAttachmentsFragment.newInstance(accountId, ownerId)
        }
        return fragment
    }
}