package dev.ragnarok.fenrir.fragment.wall.wallattachments

import androidx.fragment.app.Fragment
import dev.ragnarok.fenrir.fragment.wall.wallattachments.wallmultiattachments.WallMultiAttachmentsFragment
import dev.ragnarok.fenrir.fragment.wall.wallattachments.wallpostqueryattachments.WallPostQueryAttachmentsFragment
import dev.ragnarok.fenrir.util.FindAttachmentType

object WallAttachmentsFragmentFactory {
    fun newInstance(accountId: Long, ownerId: Long, type: String?): Fragment? {
        requireNotNull(type) { "Type cant bee null" }
        val fragment: Fragment? = when (type) {
            FindAttachmentType.TYPE_MULTI -> WallMultiAttachmentsFragment.newInstance(
                accountId,
                ownerId
            )

            FindAttachmentType.TYPE_POST_WITH_QUERY -> WallPostQueryAttachmentsFragment.newInstance(
                accountId,
                ownerId
            )

            else -> null
        }
        return fragment
    }
}