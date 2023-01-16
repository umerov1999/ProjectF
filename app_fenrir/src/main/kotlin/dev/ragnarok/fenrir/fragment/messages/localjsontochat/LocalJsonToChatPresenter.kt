package dev.ragnarok.fenrir.fragment.messages.localjsontochat

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.PersistentLogger
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class LocalJsonToChatPresenter(
    accountId: Long,
    private val context: Context,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<ILocalJsonToChatView>(accountId, savedInstanceState) {
    private val mPost: ArrayList<Message> = ArrayList()
    private val mCached: ArrayList<Message> = ArrayList()
    private var AttachmentType: Int
    private var isMy: Boolean
    private var peer: Peer
    private var isLoading: Boolean = false
    private var showEmpty: Boolean = false
    private val fInteractor: IMessagesRepository = messages
    private val actualDataDisposable = CompositeDisposable()
    override fun onGuiCreated(viewHost: ILocalJsonToChatView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mPost)
        resolveToolbar()
    }

    var uAttachmentType: Int
        set(value) {
            AttachmentType = value
        }
        get() = AttachmentType

    private fun isAttachments(message: Message): Boolean {
        if (message.forwardMessagesCount > 0 && message.fwd != null && AttachmentType == 8)
            return true
        else if (AttachmentType == 8)
            return false
        if (!message.isHasAttachments || message.attachments == null)
            return false
        when (AttachmentType) {
            1 -> {
                return message.attachments?.photos.nonNullNoEmpty()
            }
            2 -> {
                return message.attachments?.videos.nonNullNoEmpty()
            }
            3 -> {
                return message.attachments?.docs.nonNullNoEmpty()
            }
            4 -> {
                return message.attachments?.audios.nonNullNoEmpty()
            }
            5 -> {
                return message.attachments?.links.nonNullNoEmpty()
            }
            6 -> {
                return message.attachments?.photoAlbums.nonNullNoEmpty()
            }
            7 -> {
                return message.attachments?.audioPlaylists.nonNullNoEmpty()
            }
            9 -> {
                return message.attachments?.posts.nonNullNoEmpty()
            }
        }
        return true
    }

    fun toggleAttachment() {
        view?.attachments_mode(accountId, AttachmentType)
    }

    private fun checkFWD(message: Message) {
        if (AttachmentType == 8 || message.forwardMessagesCount <= 0) {
            return
        }
        for (i in message.fwd.orEmpty()) {
            checkFWD(i)
            if (isAttachments(i)) {
                mPost.add(i)
            }
        }
    }

    fun updateMessages(isMyTogle: Boolean): Boolean {
        isLoading = true
        resolveRefreshingView()
        if (isMyTogle) {
            isMy = !isMy
        }
        mPost.clear()
        if (AttachmentType == 0) {
            if (!isMy) {
                mPost.addAll(mCached)
            } else {
                for (i in mCached) {
                    if (i.isOut) {
                        mPost.add(i)
                    }
                }
            }
        } else {
            for (i in mCached) {
                if ((!isMy || !i.isOut) && isMy) {
                    continue
                }
                checkFWD(i)
                if (isAttachments(i)) {
                    mPost.add(i)
                }
            }
        }
        resolveToolbar()
        view?.scroll_pos(0)
        view?.notifyDataSetChanged()
        showEmpty = mPost.isEmpty()
        resolveEmptyView()
        isLoading = false
        resolveRefreshingView()
        return isMy
    }

    private fun loadActualData() {
        showEmpty = false
        resolveEmptyView()
        isLoading = true
        resolveRefreshingView()
        val accountId = super.accountId
        actualDataDisposable.add(fInteractor.getMessagesFromLocalJSon(accountId, context)
            .fromIOToMain()
            .subscribe({ onActualDataReceived(it) }) { onActualDataGetError(it) })
    }

    private fun onActualDataGetError(t: Throwable) {
        PersistentLogger.logThrowable("LocalJSON issues", Exception(Utils.getCauseIfRuntime(t)))
        showError(view, Utils.getCauseIfRuntime(t))
        isLoading = false
        resolveRefreshingView()
        showEmpty = mPost.isEmpty()
        resolveEmptyView()
    }

    private fun onActualDataReceived(data: Pair<Peer, List<Message>>) {
        mPost.clear()
        mPost.addAll(data.second)
        mCached.clear()
        mCached.addAll(data.second)
        peer = data.first
        resolveToolbar()
        view?.notifyDataSetChanged()
        isLoading = false
        resolveRefreshingView()
        showEmpty = mPost.isEmpty()
        resolveEmptyView()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        resolveEmptyView()
    }

    private fun resolveRefreshingView() {
        resumedView?.showRefreshing(isLoading)
    }

    private fun resolveEmptyView() {
        resumedView?.resolveEmptyText(showEmpty)
    }

    private fun resolveToolbar() {
        view?.setToolbarTitle(peer.getTitle())
        view?.setToolbarSubtitle(
            getString(
                R.string.messages_in_json,
                Utils.safeCountOf(mPost)
            )
        )
        view?.displayToolbarAvatar(peer)
    }

    override fun onDestroyed() {
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

    init {
        AttachmentType = 0
        isMy = false
        peer = Peer(0)
        loadActualData()
    }
}
