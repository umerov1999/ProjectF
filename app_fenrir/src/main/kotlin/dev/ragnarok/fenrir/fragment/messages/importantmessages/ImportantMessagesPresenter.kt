package dev.ragnarok.fenrir.fragment.messages.importantmessages

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.fragment.messages.AbsMessageListPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.getSelected
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable

class ImportantMessagesPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AbsMessageListPresenter<IImportantMessagesView>(accountId, savedInstanceState) {
    private val fInteractor: IMessagesRepository = messages
    private val actualDataDisposable = CompositeDisposable()
    private var actualDataReceived = false
    private var endOfContent = false
    private var actualDataLoading = false
    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(fInteractor.getImportantMessages(accountId, 50, offset, null)
            .fromIOToMain()
            .subscribe({ data ->
                onActualDataReceived(
                    offset,
                    data
                )
            }) { t -> onActualDataGetError(t) })
    }

    fun fireMessageRestoreClick(message: Message) {
        val id = message.getObjectId()
        appendDisposable(fInteractor.restoreMessage(accountId, accountId, id)
            .fromIOToMain()
            .subscribe({ onMessageRestoredSuccessfully(id) }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    fun fireImportantMessageClick(message: Message, position: Int) {
        val actionModeActive = Utils.countOfSelection(data)
        if (actionModeActive > 0) {
            message.isSelected = !message.isSelected
            resolveActionMode()
            safeNotifyItemChanged(position)
        } else {
            view?.goToMessagesLookup(
                accountId,
                message.peerId,
                message.getObjectId()
            )
        }
    }

    private fun onMessageRestoredSuccessfully(id: Int) {
        val message = findById(id)
        if (message != null) {
            message.setDeleted(false)
            safeNotifyDataChanged()
        }
    }

    private fun onActualDataReceived(offset: Int, recv: List<Message>) {
        actualDataLoading = false
        endOfContent = data.isEmpty()
        actualDataReceived = true
        if (offset == 0) {
            data.clear()
            data.addAll(recv)
            safeNotifyDataChanged()
        } else {
            val startSize = data.size
            data.addAll(recv)
            view?.notifyDataAdded(
                startSize,
                recv.size
            )
        }
        resolveRefreshingView()
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
        resolveRefreshingView()
    }

    override fun onActionModeForwardClick() {
        super.onActionModeForwardClick()
        val selected: ArrayList<Message> = getSelected(data)
        if (selected.isNotEmpty()) {
            view?.forwardMessages(
                accountId,
                selected
            )
        }
    }

    override fun onActionModeDeleteClick() {
        super.onActionModeDeleteClick()
        val ids = Observable.fromIterable(data)
            .filter { it.isSelected }
            .map { it.getObjectId() }
            .toList()
            .blockingGet()
        if (ids.nonNullNoEmpty()) {
            appendDisposable(fInteractor.deleteMessages(
                accountId,
                accountId,
                ids,
                forAll = false,
                spam = false
            )
                .fromIOToMain()
                .subscribe({ onMessagesDeleteSuccessfully(ids) }) { t ->
                    showError(getCauseIfRuntime(t))
                })
        }
    }

    override fun onActionModeSpamClick() {
        super.onActionModeDeleteClick()
        val ids = Observable.fromIterable(data)
            .filter { it.isSelected }
            .map { it.getObjectId() }
            .toList()
            .blockingGet()
        if (ids.nonNullNoEmpty()) {
            appendDisposable(fInteractor.deleteMessages(
                accountId, accountId, ids,
                forAll = false, spam = true
            )
                .fromIOToMain()
                .subscribe({ onMessagesDeleteSuccessfully(ids) }) { t ->
                    showError(getCauseIfRuntime(t))
                })
        }
    }

    private fun onMessagesDeleteSuccessfully(ids: Collection<Int>) {
        for (id in ids) {
            findById(id)?.setDeleted(true)
        }
        safeNotifyDataChanged()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.showRefreshing(
            actualDataLoading
        )
    }

    fun fireScrollToEnd(): Boolean {
        if (!endOfContent && data.nonNullNoEmpty() && actualDataReceived && !actualDataLoading) {
            loadActualData(data.size)
            return false
        }
        return true
    }

    private fun checkPosition(position: Int): Boolean {
        return position >= 0 && data.size > position
    }

    fun fireRemoveImportant(position: Int) {
        if (!checkPosition(position)) {
            return
        }
        val msg = data[position]
        appendDisposable(fInteractor.markAsImportant(
            accountId,
            msg.peerId,
            setOf(msg.getObjectId()),
            0
        )
            .fromIOToMain()
            .subscribe({
                data.removeAt(position)
                safeNotifyDataChanged()
            }) { })
    }

    fun fireRefresh() {
        actualDataDisposable.clear()
        actualDataLoading = false
        loadActualData(0)
    }

    fun fireTranscript(voiceMessageId: String?, messageId: Int) {
        appendDisposable(fInteractor.recogniseAudioMessage(accountId, messageId, voiceMessageId)
            .fromIOToMain()
            .subscribe({ }) { })
    }

    init {
        loadActualData(0)
    }
}