package dev.ragnarok.fenrir.fragment.messages.conversationattachments

import android.os.Bundle
import androidx.fragment.app.Fragment
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationaudios.ConversationAudiosFragment
import dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationdocs.ConversationDocsFragment
import dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationlinks.ConversationLinksFragment
import dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationmultiattachments.ConversationMultiAttachmentsFragment
import dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationphotos.ConversationPhotosFragment
import dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationposts.ConversationPostsFragment
import dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationvideos.ConversationVideosFragment
import dev.ragnarok.fenrir.util.FindAttachmentType

object ConversationFragmentFactory {
    fun newInstance(args: Bundle): Fragment {
        val type = args.getString(Extra.TYPE)
            ?: throw IllegalArgumentException("Type cant bee null")
        val fragment: Fragment = when (type) {
            FindAttachmentType.TYPE_PHOTO -> ConversationPhotosFragment()
            FindAttachmentType.TYPE_VIDEO -> ConversationVideosFragment()
            FindAttachmentType.TYPE_DOC -> ConversationDocsFragment()
            FindAttachmentType.TYPE_AUDIO -> ConversationAudiosFragment()
            FindAttachmentType.TYPE_LINK -> ConversationLinksFragment()
            FindAttachmentType.TYPE_POST -> ConversationPostsFragment()
            FindAttachmentType.TYPE_MULTI -> ConversationMultiAttachmentsFragment()
            else -> {
                throw IllegalArgumentException("Type cant bee null")
            }
        }
        fragment.arguments = args
        return fragment
    }

    fun buildArgs(accountId: Long, peerId: Long, type: String?): Bundle {
        val bundle = Bundle()
        bundle.putLong(Extra.ACCOUNT_ID, accountId)
        bundle.putLong(Extra.PEER_ID, peerId)
        bundle.putString(Extra.TYPE, type)
        return bundle
    }
}