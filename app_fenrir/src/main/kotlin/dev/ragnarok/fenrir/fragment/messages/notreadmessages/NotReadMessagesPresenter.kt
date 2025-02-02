package dev.ragnarok.fenrir.fragment.messages.notreadmessages

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.Mode
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.fragment.messages.AbsMessageListPresenter
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Side
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.getSelected
import dev.ragnarok.fenrir.util.Utils.indexOf
import dev.ragnarok.fenrir.util.Utils.isHiddenAccount
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.dummy
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO

class NotReadMessagesPresenter(
    accountId: Long,
    focusTo: Int,
    incoming: Int,
    outgoing: Int,
    unreadCount: Int,
    peer: Peer,
    savedInstanceState: Bundle?
) : AbsMessageListPresenter<INotReadMessagesView>(accountId, savedInstanceState) {
    private val messagesInteractor: IMessagesRepository = messages
    private val loadingState: LOADING_STATE
    private val peer: Peer
    private var mFocusMessageId = 0
    private var unreadCount: Int
    override fun onGuiCreated(viewHost: INotReadMessagesView) {
        super.onGuiCreated(viewHost)
        viewHost.displayMessages(accountId, data, lastReadId)
        loadingState.updateState()
        resolveToolbar()
        resolveUnreadCount()
    }

    private fun initRequest() {
        appendJob(
            messagesInteractor.getPeerMessages(
                accountId,
                peer.id,
                COUNT,
                -COUNT / 2,
                mFocusMessageId,
                cacheData = false,
                rev = false
            )
                .fromIOToMain({ messages -> onInitDataLoaded(messages) }) { t ->
                    onDataGetError(
                        t
                    )
                })
    }

    private fun onDataGetError(t: Throwable) {
        loadingState.FooterDisable()
        loadingState.HeaderDisable()
        showError(getCauseIfRuntime(t))
    }

    private fun onUpDataGetError(t: Throwable) {
        loadingState.FooterEnable()
        showError(getCauseIfRuntime(t))
    }

    private fun onDownDataGetError(t: Throwable) {
        loadingState.HeaderEnable()
        showError(getCauseIfRuntime(t))
    }

    fun fireDeleteForMeClick(ids: ArrayList<Int>) {
        deleteSentImpl(ids)
    }

    private fun deleteSentImpl(ids: Collection<Int>) {
        appendJob(
            messagesInteractor.deleteMessages(
                accountId, peer.id, ids,
                forAll = false,
                spam = false
            )
                .fromIOToMain(dummy()) { t ->
                    showError(t)
                })
    }

    fun fireFooterLoadMoreClick() {
        loadMoreUp()
    }

    fun fireHeaderLoadMoreClick() {
        loadMoreDown()
    }

    private fun loadMoreDown() {
        if (!loadingState.canLoadingHeader()) return
        val firstMessageId = firstMessageId
        loadingState.headerLoading()
        val targetMessageId = firstMessageId
        appendJob(
            messagesInteractor.getPeerMessages(
                accountId,
                peer.id,
                COUNT,
                -COUNT,
                targetMessageId,
                cacheData = false,
                rev = false
            )
                .fromIOToMain({ onDownDataLoaded(it) }) { t ->
                    onDownDataGetError(
                        t
                    )
                })
    }

    override fun onActionModeDeleteClick() {
        super.onActionModeDeleteClick()
        val ids = ArrayList<Int>(data.size)
        for (i in data) {
            if (i.isSelected) {
                ids.add(i.getObjectId())
            }
        }
        if (ids.nonNullNoEmpty()) {
            appendJob(
                messagesInteractor.deleteMessages(
                    accountId,
                    peer.id,
                    ids,
                    forAll = false,
                    spam = false
                )
                    .fromIOToMain({ onMessagesDeleteSuccessfully(ids) }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
        }
    }

    override fun onActionModeSpamClick() {
        super.onActionModeDeleteClick()
        val ids = ArrayList<Int>(data.size)
        for (i in data) {
            if (i.isSelected) {
                ids.add(i.getObjectId())
            }
        }
        if (ids.nonNullNoEmpty()) {
            appendJob(
                messagesInteractor.deleteMessages(
                    accountId, peer.id, ids,
                    forAll = false, spam = true
                )
                    .fromIOToMain({ onMessagesDeleteSuccessfully(ids) }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
        }
    }

    private fun loadMoreUp() {
        if (!loadingState.canLoadingFooter()) return
        val lastMessageId = lastMessageId ?: return
        loadingState.footerLoading()
        val targetLastMessageId = lastMessageId
        appendJob(
            messagesInteractor.getPeerMessages(
                accountId,
                peer.id,
                COUNT,
                0,
                targetLastMessageId,
                cacheData = false,
                rev = false
            )
                .fromIOToMain({ onUpDataLoaded(it) }) { t ->
                    onUpDataGetError(
                        t
                    )
                })
    }

    private val lastMessageId: Int?
        get() = if (data.isEmpty()) null else data[data.size - 1].getObjectId()
    private val firstMessageId: Int?
        get() = if (data.isEmpty()) null else data[0].getObjectId()

    override fun onActionModeForwardClick() {
        super.onActionModeForwardClick()
        val selected: ArrayList<Message> = getSelected(data)
        if (selected.nonNullNoEmpty()) {
            view?.forwardMessages(
                accountId,
                selected
            )
        }
    }

    fun fireMessageRestoreClick(message: Message) {
        val id = message.getObjectId()
        appendJob(
            messagesInteractor.restoreMessage(accountId, peer.id, id)
                .fromIOToMain({ onMessageRestoredSuccessfully(id) }) { t ->
                    showError(getCauseIfRuntime(t))
                })
    }

    private fun onMessageRestoredSuccessfully(id: Int) {
        val message = findById(id)
        if (message != null) {
            message.setDeleted(false)
            safeNotifyDataChanged()
        }
    }

    private fun onMessagesDeleteSuccessfully(ids: Collection<Int>) {
        for (id in ids) {
            val message = findById(id)
            message?.setDeleted(true)
        }
        safeNotifyDataChanged()
    }

    private fun onInitDataLoaded(messages: List<Message>) {
        data.clear()
        data.addAll(messages)
        view?.notifyDataChanged()
        loadingState.reset()
        val index = indexOf(messages, mFocusMessageId)
        if (index != -1) {
            view?.focusTo(index)
        } else if (mFocusMessageId == 0) {
            view?.focusTo(
                messages.size - 1
            )
        }
    }

    private fun resolveToolbar() {
        view?.displayToolbarAvatar(
            peer
        )
    }

    private fun resolveUnreadCount() {
        view?.displayUnreadCount(
            unreadCount
        )
    }

    override fun onMessageClick(message: Message, position: Int, x: Int?, y: Int?) {
        if (!readUnreadMessagesUpIfExists(message)) {
            if (x != null && y != null) {
                resolvePopupMenu(message, position, x, y)
            }
        }
    }

    private fun resolvePopupMenu(message: Message, position: Int, x: Int, y: Int) {
        if (isHiddenAccount(accountId) || !Settings.get()
                .main().isChat_popup_menu || Utils.countOfSelection(
                data
            ) > 0
        ) {
            return
        }
        message.isSelected = true
        safeNotifyItemChanged(position)

        view?.showPopupOptions(
            position,
            x,
            y,
            canEdit = false,
            canPin = false,
            canStar = false,
            doStar = false,
            canSpam = !message.isOut
        )
    }

    private fun readUnreadMessagesUpIfExists(message: Message): Boolean {
        if (isHiddenAccount(accountId)) return false

        if (!message.isOut && message.originalId > lastReadId.incoming) {
            lastReadId.incoming = message.originalId
            view?.notifyDataChanged()
            appendJob(
                messagesInteractor.markAsRead(accountId, peer.id, message.originalId)
                    .fromIOToMain({
                        appendJob(
                            messagesInteractor.getConversationSingle(
                                accountId,
                                peer.id, Mode.NET
                            )
                                .fromIOToMain({ e ->
                                    unreadCount = e.unreadCount
                                    resolveUnreadCount()
                                }) { s ->
                                    showError(s)
                                })
                    }) { t ->
                        showError(t)
                    })
            return true
        }
        return false
    }

    private fun onUpDataLoaded(messages: List<Message>) {
        if (messages.isEmpty()) {
            loadingState.FooterDisable()
        } else {
            loadingState.FooterEnable()
        }
        val size = data.size
        data.addAll(messages)
        view?.notifyMessagesUpAdded(
            size,
            messages.size
        )
    }

    fun fireFinish() {
        view?.doFinish(
            lastReadId.incoming,
            lastReadId.outgoing,
            true
        )
    }

    private fun onDownDataLoaded(messages: List<Message>) {
        if (messages.isEmpty()) {
            view?.doFinish(
                lastReadId.incoming,
                lastReadId.outgoing,
                false
            )
            loadingState.HeaderDisable()
        } else {
            loadingState.HeaderEnable()
        }
        data.addAll(0, messages)
        view?.notifyMessagesDownAdded(
            messages.size
        )
    }

    private class LOADING_STATE(private val changes: NotifyChanges) {
        @Side
        private var Header = Side.DISABLED

        @Side
        private var Footer = Side.DISABLED
        fun updateState() {
            changes.updateState(Header, Footer)
        }

        fun reset() {
            Header = Side.NO_LOADING
            Footer = Side.NO_LOADING
            updateState()
        }

        fun footerLoading() {
            Footer = Side.LOADING
            updateState()
        }

        fun headerLoading() {
            Header = Side.LOADING
            updateState()
        }

        fun FooterDisable() {
            Footer = Side.DISABLED
            updateState()
        }

        fun HeaderEnable() {
            Header = Side.NO_LOADING
            updateState()
        }

        fun FooterEnable() {
            Footer = Side.NO_LOADING
            updateState()
        }

        fun canLoadingHeader(): Boolean {
            return Header == Side.NO_LOADING && Footer != Side.LOADING
        }

        fun canLoadingFooter(): Boolean {
            return Footer == Side.NO_LOADING && Header != Side.LOADING
        }

        fun HeaderDisable() {
            Header = Side.DISABLED
            updateState()
        }

        interface NotifyChanges {
            fun updateState(@Side Header: Int, @Side Footer: Int)
        }
    }

    fun fireTranscript(voiceMessageId: String?, messageId: Int) {
        appendJob(
            messages.recogniseAudioMessage(accountId, messageId, voiceMessageId)
                .hiddenIO()
        )
    }

    companion object {
        private const val COUNT = 30
    }

    init {
        lastReadId.incoming = incoming
        lastReadId.outgoing = outgoing
        this.peer = peer
        this.unreadCount = unreadCount
        loadingState = LOADING_STATE(object : LOADING_STATE.NotifyChanges {
            override fun updateState(Header: Int, Footer: Int) {
                @LoadMoreState val header: Int = when (Header) {
                    Side.LOADING -> LoadMoreState.LOADING
                    Side.NO_LOADING -> LoadMoreState.CAN_LOAD_MORE
                    else -> LoadMoreState.INVISIBLE
                }
                @LoadMoreState val footer: Int = when (Footer) {
                    Side.DISABLED -> LoadMoreState.END_OF_LIST
                    Side.LOADING -> LoadMoreState.LOADING
                    Side.NO_LOADING -> LoadMoreState.CAN_LOAD_MORE
                    else -> LoadMoreState.INVISIBLE
                }
                view?.setupHeaders(
                    footer,
                    header
                )
            }
        })
        if (savedInstanceState == null) {
            mFocusMessageId = focusTo
            initRequest()
        }
    }
}