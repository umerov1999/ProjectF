package dev.ragnarok.fenrir.fragment.conversation.conversationvideos

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.conversation.abschatattachments.AbsChatAttachmentsFragment
import dev.ragnarok.fenrir.fragment.videos.VideosAdapter
import dev.ragnarok.fenrir.model.Video

class ConversationVideosFragment :
    AbsChatAttachmentsFragment<Video, ChatAttachmentVideoPresenter, IChatAttachmentVideoView>(),
    VideosAdapter.VideoOnClickListener, IChatAttachmentVideoView {
    override fun createLayoutManager(): RecyclerView.LayoutManager {
        val columns = resources.getInteger(R.integer.videos_column_count)
        return StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
    }

    override fun createAdapter(): RecyclerView.Adapter<*> {
        val adapter = VideosAdapter(requireActivity(), emptyList())
        adapter.setVideoOnClickListener(this)
        return adapter
    }

    override fun onVideoClick(position: Int, video: Video) {
        presenter?.fireVideoClick(
            video
        )
    }

    override fun onVideoLongClick(position: Int, video: Video): Boolean {
        presenter?.fireGoToMessagesLookup(
            video.msgPeerId,
            video.msgId
        )
        return true
    }

    override fun displayAttachments(data: MutableList<Video>) {
        val adapter = adapter as VideosAdapter?
        adapter?.setData(data)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ChatAttachmentVideoPresenter> {
        return object : IPresenterFactory<ChatAttachmentVideoPresenter> {
            override fun create(): ChatAttachmentVideoPresenter {
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                val peerId = requireArguments().getLong(Extra.PEER_ID)
                return ChatAttachmentVideoPresenter(peerId, accountId, saveInstanceState)
            }
        }
    }
}