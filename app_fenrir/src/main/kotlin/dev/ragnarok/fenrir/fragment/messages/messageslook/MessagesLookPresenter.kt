package dev.ragnarok.fenrir.fragment.messages.messageslook

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.Mode
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.fragment.messages.AbsMessageListPresenter
import dev.ragnarok.fenrir.model.Conversation
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
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.dummy
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO

class MessagesLookPresenter(
    accountId: Long,
    peerId: Long,
    focusTo: Int,
    message: Message?,
    savedInstanceState: Bundle?
) : AbsMessageListPresenter<IMessagesLookView>(accountId, savedInstanceState) {
    private val messagesInteractor: IMessagesRepository = messages
    private val peer = Peer(peerId)
    private val loadingState: LOADING_STATE
    private var mFocusMessageId = 0

    private fun fetchConversationThenActual(needFetchChat: Boolean) {
        appendJob(
            messagesInteractor.getConversationSingle(
                accountId,
                peer.id,
                Mode.ANY
            )
                .fromIOToMain(
                    { onConversationFetched(needFetchChat, it) },
                    { onConversationFetchFail(needFetchChat, it) })
        )
    }

    private fun onConversationFetchFail(needFetchChat: Boolean, throwable: Throwable) {
        showError(view, throwable)
        view?.displayToolbarAvatar(accountId, peer)
        if (needFetchChat) {
            initRequest()
        }
    }

    private fun onConversationFetched(needFetchChat: Boolean, data: Conversation) {
        if (peer.getTitle().isNullOrEmpty()) {
            peer.setTitle(data.getDisplayTitle())
        }
        if (peer.avaUrl.isNullOrEmpty()) {
            peer.setAvaUrl(data.imageUrl)
        }
        view?.displayToolbarAvatar(accountId, peer)
        lastReadId.incoming = data.inRead
        lastReadId.outgoing = data.outRead
        if (needFetchChat) {
            initRequest()
        }
    }

    override fun onGuiCreated(viewHost: IMessagesLookView) {
        super.onGuiCreated(viewHost)
        viewHost.displayMessages(accountId, data, lastReadId)
        loadingState.updateState()
        view?.displayToolbarAvatar(accountId, peer)
    }

    override fun onMessageClick(message: Message, position: Int, x: Int?, y: Int?) {
        if (x != null && y != null) {
            resolvePopupMenu(message, position, x, y)
        }
    }

    private fun resolvePopupMenu(message: Message, position: Int, x: Int, y: Int) {
        if (Utils.isHiddenAccount(accountId) || !Settings.get()
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
                forAll = false, spam = false
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
        val firstMessageId = firstMessageId ?: return
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
            findById(id)?.setDeleted(true)
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
            view?.focusTo(messages.size - 1)
        }
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

    private fun onDownDataLoaded(messages: List<Message>) {
        if (messages.isEmpty()) {
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
        private const val COUNT = 40
    }

    init {
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
            if (message != null && focusTo == 0) {
                mFocusMessageId = 0
                data.clear()
                data.add(message)
                view?.notifyDataChanged()
                view?.focusTo(0)
                fetchConversationThenActual(false)
            } else {
                mFocusMessageId = focusTo
                fetchConversationThenActual(true)
            }
        }
    }
}