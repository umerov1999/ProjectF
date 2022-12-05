package dev.ragnarok.fenrir.fragment.conversation.conversationphotos

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.conversation.abschatattachments.AbsChatAttachmentsFragment
import dev.ragnarok.fenrir.fragment.fave.favephotos.FavePhotosAdapter
import dev.ragnarok.fenrir.fragment.fave.favephotos.FavePhotosAdapter.PhotoConversationListener
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.place.PlaceFactory.getTmpSourceGalleryPlace

class ConversationPhotosFragment :
    AbsChatAttachmentsFragment<Photo, ChatAttachmentPhotoPresenter, IChatAttachmentPhotosView>(),
    FavePhotosAdapter.PhotoSelectionListener, PhotoConversationListener, IChatAttachmentPhotosView {
    private val requestPhotoUpdate = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && (result.data
                ?: return@registerForActivityResult)
                .extras != null
        ) {
            val ps = ((result.data ?: return@registerForActivityResult).extras
                ?: return@registerForActivityResult).getInt(Extra.POSITION)
            (adapter as FavePhotosAdapter?)?.updateCurrentPosition(ps)
            mRecyclerView?.scrollToPosition(ps)
        }
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        val columns = resources.getInteger(R.integer.photos_column_count)
        return GridLayoutManager(requireActivity(), columns)
    }

    override fun createAdapter(): RecyclerView.Adapter<*> {
        val apiPhotoFavePhotosAdapter = FavePhotosAdapter(requireActivity(), emptyList())
        apiPhotoFavePhotosAdapter.setPhotoSelectionListener(this)
        apiPhotoFavePhotosAdapter.setPhotoConversationListener(this)
        return apiPhotoFavePhotosAdapter
    }

    override fun onPhotoClicked(position: Int, photo: Photo) {
        presenter?.firePhotoClick(
            position
        )
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ChatAttachmentPhotoPresenter> {
        return object : IPresenterFactory<ChatAttachmentPhotoPresenter> {
            override fun create(): ChatAttachmentPhotoPresenter {
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                val peerId = requireArguments().getInt(Extra.PEER_ID)
                return ChatAttachmentPhotoPresenter(peerId, accountId, saveInstanceState)
            }
        }
    }

    override fun displayAttachments(data: MutableList<Photo>) {
        val adapter = adapter as FavePhotosAdapter?
        adapter?.setData(data)
    }

    override fun goToTempPhotosGallery(accountId: Int, source: TmpSource, index: Int) {
        getTmpSourceGalleryPlace(accountId, source, index).setActivityResultLauncher(
            requestPhotoUpdate
        ).tryOpenWith(requireActivity())
    }

    override fun goToTempPhotosGallery(accountId: Int, ptr: Long, index: Int) {
        getTmpSourceGalleryPlace(
            accountId,
            ptr,
            index
        ).setActivityResultLauncher(requestPhotoUpdate).tryOpenWith(requireActivity())
    }

    override fun onGoPhotoConversation(photo: Photo) {
        presenter?.fireGoToMessagesLookup(
            photo.msgPeerId,
            photo.msgId
        )
    }
}