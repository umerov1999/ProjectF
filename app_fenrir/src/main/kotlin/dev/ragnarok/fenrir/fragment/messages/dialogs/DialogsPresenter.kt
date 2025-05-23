package dev.ragnarok.fenrir.fragment.messages.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import dev.ragnarok.fenrir.domain.IAccountsInteractor
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IStickersInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.exception.UnauthorizedException
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fragment.messages.dialogs.IDialogsView.IContextView
import dev.ragnarok.fenrir.longpoll.ILongpollManager
import dev.ragnarok.fenrir.longpoll.LongpollInstance
import dev.ragnarok.fenrir.model.Dialog
import dev.ragnarok.fenrir.model.FwdMessages
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.ModelsBundle
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.PeerUpdate
import dev.ragnarok.fenrir.model.SaveMessageBuilder
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AssertUtils.assertPositive
import dev.ragnarok.fenrir.util.HelperSimple
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.PersistentLogger.logThrowable
import dev.ragnarok.fenrir.util.ShortcutUtils.addDynamicShortcut
import dev.ragnarok.fenrir.util.ShortcutUtils.createChatShortcutRx
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.idsListOfOwner
import dev.ragnarok.fenrir.util.Utils.indexOf
import dev.ragnarok.fenrir.util.Utils.isHiddenAccount
import dev.ragnarok.fenrir.util.Utils.isHiddenCurrent
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.needReloadDialogs
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.dummy
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain

class DialogsPresenter(
    accountId: Long,
    initialDialogsOwnerId: Long,
    private val models: ModelsBundle?,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IDialogsView>(accountId, savedInstanceState, true) {
    private val dialogs: ArrayList<Dialog> = ArrayList()
    private val messagesInteractor: IMessagesRepository
    private val stickerInteractor: IStickersInteractor
    private val accountsInteractor: IAccountsInteractor
    private val longpollManager: ILongpollManager
    private val netDisposable = CompositeJob()
    private val cacheLoadingDisposable = CompositeJob()
    private var dialogsOwnerId = 0L
    private var endOfContent = false
    private var netLoadingNow = false
    private var cacheNowLoading = false
    private var needAskReloadDialogsWhenGuiReady = false
    private var needShowDialogSendHelperWhenGuiReady = false
    private var lastSnackShow = 0L
    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putLong(SAVE_DIALOGS_OWNER_ID, dialogsOwnerId)
    }

    override fun onGuiCreated(viewHost: IDialogsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(dialogs, dialogsOwnerId)

        // only for user dialogs
        viewHost.setCreateGroupChatButtonVisible(dialogsOwnerId > 0)
        if (needAskReloadDialogsWhenGuiReady) {
            viewHost.askToReload()
            needAskReloadDialogsWhenGuiReady = false
        }

        if (needShowDialogSendHelperWhenGuiReady) {
            viewHost.showDialogSendHelper()
            needShowDialogSendHelperWhenGuiReady = false
        }
    }

    private fun onDialogsFirstResponse(data: List<Dialog>) {
        if (!Settings.get().main().isBe_online || isHiddenAccount(accountId)) {
            netDisposable.add(
                accountsInteractor.setOffline(accountId)
                    .hiddenIO()
            )
        }
        setNetLoadingNow(false)
        endOfContent = data.isEmpty()
        dialogs.clear()
        dialogs.addAll(data)
        safeNotifyDataSetChanged()
        if (!isHiddenCurrent) {
            receiveSubItems()
        }
    }

    private fun onDialogsGetError(t: Throwable) {
        val cause = getCauseIfRuntime(t)
        cause.printStackTrace()
        setNetLoadingNow(false)
        if (cause is UnauthorizedException) {
            return
        }
        logThrowable("Dialogs issues", cause)
        showError(cause)
    }

    private fun setNetLoadingNow(netLoadingNow: Boolean) {
        this.netLoadingNow = netLoadingNow
        resolveRefreshingView()
    }

    private fun requestAtLast() {
        if (netLoadingNow) {
            return
        }
        setNetLoadingNow(true)
        netDisposable.add(
            messagesInteractor.getDialogs(dialogsOwnerId, COUNT, null)
                .fromIOToMain({ onDialogsFirstResponse(it) }) { t ->
                    onDialogsGetError(
                        t
                    )
                })
        resolveRefreshingView()
    }

    private fun requestNext() {
        if (netLoadingNow) {
            return
        }
        val lastMid = lastDialogMessageId ?: return
        setNetLoadingNow(true)
        netDisposable.add(
            messagesInteractor.getDialogs(dialogsOwnerId, COUNT, lastMid)
                .fromIOToMain(
                    { onNextDialogsResponse(it) }
                ) { throwable -> onDialogsGetError(getCauseIfRuntime(throwable)) })
    }

    private fun onNextDialogsResponse(data: List<Dialog>) {
        if (!Settings.get().main().isBe_online || isHiddenAccount(accountId)) {
            netDisposable.add(
                accountsInteractor.setOffline(accountId)
                    .hiddenIO()
            )
        }
        setNetLoadingNow(false)
        val tmp = Utils.stripEqualsWithCounter(data, dialogs, COUNT)
        if (tmp.isEmpty()) {
            endOfContent = true
        } else {
            val startSize = dialogs.size
            dialogs.addAll(tmp)
            view?.notifyDataAdded(
                startSize,
                tmp.size
            )
        }
    }

    private fun onDialogRemovedSuccessfully(accountId: Long, peeId: Long) {
        view?.showSnackbar(
            R.string.deleted,
            true
        )
        onDialogDeleted(accountId, peeId)
    }

    private fun removeDialog(peeId: Long) {
        val accountId = dialogsOwnerId
        appendJob(
            messagesInteractor.deleteDialog(accountId, peeId)
                .fromIOToMain({ onDialogRemovedSuccessfully(accountId, peeId) }) { t ->
                    showError(t)
                })
    }

    private fun resolveRefreshingView() {
        // on resume only !!!
        resumedView?.showRefreshing(
            cacheNowLoading || netLoadingNow
        )
    }

    private fun loadCachedThenActualData() {
        cacheNowLoading = true
        resolveRefreshingView()
        cacheLoadingDisposable.add(
            messagesInteractor.getCachedDialogs(dialogsOwnerId)
                .fromIOToMain({ onCachedDataReceived(it) }) { ignored ->
                    ignored.printStackTrace()
                    onCachedDataReceived(emptyList())
                })
    }

    fun fireRepost(dialog: Dialog) {
        if (models == null) {
            return
        }
        val fwds = ArrayList<Message>()
        val builder = SaveMessageBuilder(accountId, dialog.peerId)
        for (model in models) {
            if (model is FwdMessages) {
                fwds.addAll(model.fwds)
            } else {
                builder.attach(model)
            }
        }
        builder.setForwardMessages(fwds)
        val encryptionEnabled =
            Settings.get().security().isMessageEncryptionEnabled(accountId, dialog.peerId)
        @KeyLocationPolicy var keyLocationPolicy = KeyLocationPolicy.PERSIST
        if (encryptionEnabled) {
            keyLocationPolicy =
                Settings.get().security().getEncryptionLocationPolicy(accountId, dialog.peerId)
        }
        builder.setRequireEncryption(encryptionEnabled).setKeyLocationPolicy(keyLocationPolicy)
        appendJob(
            messagesInteractor.put(builder)
                .fromIOToMain({
                    messagesInteractor.runSendingQueue()

                    view?.showSnackbar(
                        R.string.success,
                        false
                    )
                }) { t -> onDialogsGetError(t) })
    }

    @SuppressLint("CheckResult")
    private fun receiveSubItems() {
        if (accountId <= 0) {
            return
        }
        try {
            if (Utils.needReloadReactionAssets(accountId)) {
                messagesInteractor
                    .getReactionsAssets(accountId)
                    .fromIOToMain({
                        if (Utils.getReactionsAssets()[accountId] == null) {
                            Utils.getReactionsAssets()[accountId] = HashMap()
                        } else {
                            Utils.getReactionsAssets()[accountId]?.clear()
                        }
                        for (i in it) {
                            Utils.getReactionsAssets()[accountId]?.set(i.reaction_id, i)
                        }
                    }) {
                        Settings.get().main().del_last_reaction_assets_sync(accountId)
                        if (Settings.get().main().isDeveloper_mode) {
                            showError(it)
                        }
                    }
            }

            if (Utils.needReloadStickerSets(accountId)) {
                stickerInteractor
                    .receiveAndStoreStickerSets(accountId)
                    .fromIOToMain(dummy()) {
                        Settings.get().main().del_last_sticker_sets_sync(accountId)
                        if (Settings.get().main().isDeveloper_mode) {
                            showError(it)
                        }
                    }
            }
            if (Utils.needReloadStickerSetsCustom(accountId)) {
                stickerInteractor
                    .receiveAndStoreCustomStickerSets(accountId)
                    .fromIOToMain(dummy()) {
                        Settings.get().main().del_last_sticker_sets_custom_sync(accountId)
                        if (Settings.get().main().isDeveloper_mode) {
                            showError(it)
                        }
                    }
            }
            if (Settings.get()
                    .main().isHint_stickers && Utils.needReloadStickerKeywords(accountId)
            ) {
                stickerInteractor
                    .receiveAndStoreKeywordsStickers(accountId)
                    .fromIOToMain(dummy()) {
                        Settings.get().main().del_last_sticker_keywords_sync(accountId)
                        if (Settings.get().main().isDeveloper_mode) {
                            showError(it)
                        }
                    }
            }
        } catch (e: Exception) {
            if (Settings.get().main().isDeveloper_mode) {
                showError(e)
            }
        }
    }

    private fun onCachedDataReceived(data: List<Dialog>) {
        cacheNowLoading = false
        dialogs.clear()
        dialogs.addAll(data)
        safeNotifyDataSetChanged()
        resolveRefreshingView()
        view?.notifyHasAttachments(models != null)

        if (models != null && HelperSimple.needHelp(HelperSimple.DIALOG_SEND_HELPER, 3)) {
            if (view == null) {
                needShowDialogSendHelperWhenGuiReady = true
            } else {
                view?.showDialogSendHelper()
            }
        }

        if (Settings.get().main().isNot_update_dialogs || isHiddenCurrent) {
            if (!isHiddenCurrent) {
                receiveSubItems()
            }
            if (needReloadDialogs(accountId)) {
                if (view == null) {
                    needAskReloadDialogsWhenGuiReady = true
                } else {
                    view?.askToReload()
                }
            }
        } else {
            requestAtLast()
        }
    }

    private fun onPeerUpdate(updates: List<PeerUpdate>) {
        for (update in updates) {
            if (update.accountId == dialogsOwnerId) {
                onDialogUpdate(update)
            }
        }
    }

    private fun onDialogUpdate(update: PeerUpdate) {
        if (dialogsOwnerId != update.accountId) {
            return
        }
        val accountId = update.accountId
        val peerId = update.peerId
        if (update.lastMessage != null) {
            val id = listOf((update.lastMessage ?: return).messageId)
            appendJob(
                messagesInteractor.findCachedMessages(accountId, id)
                    .fromIOToMain { messages ->
                        if (messages.isEmpty()) {
                            onDialogDeleted(accountId, peerId)
                        } else {
                            onActualMessagePeerMessageReceived(
                                accountId, peerId, update, wrap(
                                    messages[0]
                                )
                            )
                        }
                    }
            )
        } else {
            onActualMessagePeerMessageReceived(accountId, peerId, update, empty())
        }
    }

    private fun onActualMessagePeerMessageReceived(
        accountId: Long,
        peerId: Long,
        update: PeerUpdate,
        messageOptional: Optional<Message>
    ) {
        if (accountId != dialogsOwnerId) {
            return
        }
        val index = indexOf(dialogs, peerId)
        val dialog = if (index == -1) Dialog().setPeerId(peerId) else dialogs[index]
        if (update.readIn != null) {
            dialog.setInRead((update.readIn ?: return).messageId)
        }
        if (update.readOut != null) {
            dialog.setOutRead((update.readOut ?: return).messageId)
        }
        if (update.unread != null) {
            dialog.setUnreadCount((update.unread ?: return).count)
        }
        if (messageOptional.nonEmpty()) {
            val message = messageOptional.get()
            dialog.setLastMessageId((message ?: return).getObjectId())
            dialog.setMinor_id(message.getObjectId())
            dialog.setMessage(message)
            if (dialog.isChat) {
                dialog.setInterlocutor(message.sender)
            }
        }
        update.title?.title.nonNullNoEmpty {
            dialog.setTitle(it)
        }
        if (index != -1) {
            dialogs.sortWith(COMPARATOR)
            safeNotifyDataSetChanged()
        } else {
            if (Peer.isGroup(peerId) || Peer.isUser(peerId)) {
                appendJob(
                    owners.getBaseOwnerInfo(accountId, peerId, IOwnersRepository.MODE_ANY)
                        .fromIOToMain { o ->
                            dialog.setInterlocutor(o)
                            appendJob(
                                messages.insertDialog(accountId, dialog)
                                    .fromIOToMain {
                                        dialogs.add(dialog)
                                        dialogs.sortWith(COMPARATOR)
                                        safeNotifyDataSetChanged()
                                    }
                            )
                        }
                )
            } else {
                dialogs.add(dialog)
                dialogs.sortWith(COMPARATOR)
                safeNotifyDataSetChanged()
            }
        }
    }

    private fun onDialogDeleted(accountId: Long, peerId: Long) {
        if (dialogsOwnerId != accountId) {
            return
        }
        val index = indexOf(dialogs, peerId)
        if (index != -1) {
            dialogs.removeAt(index)
            safeNotifyDataSetChanged()
        }
    }

    private fun safeNotifyDataSetChanged() {
        view?.notifyDataSetChanged()
    }

    override fun onDestroyed() {
        cacheLoadingDisposable.cancel()
        netDisposable.cancel()
        super.onDestroyed()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        checkLongpoll()
    }

    private fun checkLongpoll() {
        if (accountId != ISettings.IAccountsSettings.INVALID_ID) {
            longpollManager.keepAlive(dialogsOwnerId)
        }
    }

    fun fireRefresh() {
        cacheLoadingDisposable.clear()
        cacheNowLoading = false
        netDisposable.clear()
        netLoadingNow = false
        endOfContent = false
        requestAtLast()
    }

    fun fireRefreshConfirmationHidden() {
        if (isHiddenAccount(accountId)) {
            resolveRefreshingView()
            view?.askToReload()
        } else {
            fireRefresh()
        }
    }

    fun fireSearchClick() {
        assertPositive(dialogsOwnerId)
        view?.goToSearch(accountId)
    }

    fun fireImportantClick() {
        assertPositive(dialogsOwnerId)
        view?.goToImportant(accountId)
    }

    fun fireDialogClick(dialog: Dialog) {
        openChat(dialog)
    }

    private fun openChat(dialog: Dialog) {
        view?.goToChat(
            accountId,
            dialogsOwnerId,
            dialog.peerId,
            dialog.getDisplayTitle(applicationContext),
            dialog.imageUrl
        )
    }

    fun fireDialogAvatarClick(dialog: Dialog) {
        if (Peer.isUser(dialog.peerId) || Peer.isGroup(dialog.peerId)) {
            view?.goToOwnerWall(
                accountId,
                Peer.toOwnerId(dialog.peerId),
                dialog.interlocutor
            )
        } else {
            openChat(dialog)
        }
    }

    private fun canLoadMore(): Boolean {
        return !cacheNowLoading && !endOfContent && !netLoadingNow && dialogs.isNotEmpty()
    }

    fun fireScrollToEndConfirmationHidden() {
        if (canLoadMore()) {
            if (isHiddenAccount(accountId)) {
                val cur = System.currentTimeMillis()
                if (cur - lastSnackShow > 2000) {
                    lastSnackShow = cur
                    view?.askToScrollToEnd()
                }
            } else {
                fireScrollToEnd()
            }
        }
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestNext()
        }
    }

    private val lastDialogMessageId: Int?
        get() = try {
            dialogs[dialogs.size - 1].lastMessageId
        } catch (_: Exception) {
            null
        }

    fun fireNewGroupChatTitleEntered(users: List<User>, title: String?) {
        val targetTitle = if (title.isNullOrEmpty()) getTitleIfEmpty(users) else title
        val accountId = accountId
        appendJob(
            messagesInteractor.createGroupChat(
                accountId,
                idsListOfOwner(users),
                targetTitle
            )
                .fromIOToMain({ chatid ->
                    onGroupChatCreated(
                        chatid,
                        targetTitle
                    )
                }) { t ->
                    showError(getCauseIfRuntime(t))
                })
    }

    private fun onGroupChatCreated(chatId: Long, title: String?) {
        view?.goToChat(
            accountId,
            dialogsOwnerId,
            Peer.fromChatId(chatId),
            title,
            null
        )
    }

    fun fireUsersForChatSelected(owners: ArrayList<Owner>) {
        val users = ArrayList<User>()
        for (i in owners) {
            if (i is User) {
                users.add(i)
            }
        }
        if (users.size == 1) {
            val user = users[0]
            // Post?
            view?.goToChat(
                accountId,
                dialogsOwnerId,
                Peer.fromUserId(user.getOwnerObjectId()),
                user.fullName,
                user.maxSquareAvatar
            )
        } else if (users.size > 1) {
            view?.showEnterNewGroupChatTitle(
                users
            )
        }
    }

    fun fireRemoveDialogClick(dialog: Dialog) {
        removeDialog(dialog.peerId)
    }

    fun fireCreateShortcutClick(dialog: Dialog) {
        assertPositive(dialogsOwnerId)
        val app = applicationContext
        appendJob(
            createChatShortcutRx(
                app, dialog.imageUrl ?: VKApiUser.CAMERA_50, accountId,
                dialog.peerId, dialog.getDisplayTitle(app) ?: ("id" + dialog.peerId)
            )
                .fromIOToMain({ onShortcutCreated() }) { throwable ->
                    view?.showError(throwable.message)
                })
    }

    private fun onShortcutCreated() {
        view?.showSnackbar(
            R.string.success,
            true
        )
    }

    override fun afterAccountChange(oldAccountId: Long, newAccountId: Long) {
        super.afterAccountChange(oldAccountId, newAccountId)

        // если на экране диалоги группы, то ничего не трогаем
        if (dialogsOwnerId < 0 && dialogsOwnerId != ISettings.IAccountsSettings.INVALID_ID) {
            return
        }
        cacheLoadingDisposable.clear()
        cacheNowLoading = false
        netDisposable.clear()
        netLoadingNow = false
        dialogsOwnerId = newAccountId
        view?.updateAccountIdNoRefresh(dialogsOwnerId)

        loadCachedThenActualData()
        longpollManager.forceDestroy(oldAccountId)
        checkLongpoll()
    }

    fun fireUnPin(dialog: Dialog) {
        appendJob(
            messagesInteractor.pinUnPinConversation(accountId, dialog.peerId, false)
                .fromIOToMain({
                    view?.customToast?.showToastSuccessBottom(
                        R.string.success
                    )
                    fireRefresh()
                }) { throwable ->
                    showError(throwable)
                })
    }

    fun firePin(dialog: Dialog) {
        appendJob(
            messagesInteractor.pinUnPinConversation(accountId, dialog.peerId, true)
                .fromIOToMain({
                    view?.customToast?.showToastSuccessBottom(
                        R.string.success
                    )
                    fireRefresh()
                }) { throwable ->
                    showError(throwable)
                })
    }

    fun fireAddToLauncherShortcuts(dialog: Dialog) {
        assertPositive(dialogsOwnerId)
        val peer = Peer(dialog.getOwnerObjectId())
            .setAvaUrl(dialog.imageUrl)
            .setTitle(dialog.getDisplayTitle(applicationContext))
        val completable = addDynamicShortcut(applicationContext, dialogsOwnerId, peer)
        appendJob(
            completable
                .fromIOToMain({
                    view?.customToast?.showToastSuccessBottom(
                        R.string.success
                    )
                }) { obj -> obj.printStackTrace() })
    }

    fun fireRead(dialog: Dialog) {
        appendJob(
            messagesInteractor.markAsRead(
                accountId,
                dialog.peerId,
                dialog.lastMessageId
            )
                .fromIOToMain({
                    view?.customToast?.showToastSuccessBottom(
                        R.string.success
                    )
                    dialog.setInRead(dialog.lastMessageId)
                    view?.notifyDataSetChanged()
                }) { throwable ->
                    showError(throwable)
                })
    }

    fun fireContextViewCreated(contextView: IContextView, dialog: Dialog) {
        val isHide = Settings.get().security().isHiddenDialog(dialog.getOwnerObjectId())
        contextView.setCanDelete(true)
        contextView.setCanRead(!isHiddenCurrent && !dialog.isLastMessageOut && dialog.lastMessageId != dialog.inRead)
        contextView.setCanAddToHomeScreen(dialogsOwnerId > 0 && !isHide)
        contextView.setCanAddToShortcuts(dialogsOwnerId > 0 && !isHide)
        contextView.setCanConfigNotifications(dialogsOwnerId > 0)
        contextView.setPinned(dialog.major_id > 0)
        contextView.setIsHidden(isHide)
    }

    fun fireOptionViewCreated(view: IDialogsView.IOptionView) {
        view.setCanSearch(dialogsOwnerId > 0)
    }

    private class DialogByIdMajorID : Comparator<Dialog> {
        override fun compare(o1: Dialog, o2: Dialog): Int {
            val res = o2.major_id.compareTo(o1.major_id)
            return if (res == 0) o2.getMinor_id().compareTo(o1.getMinor_id()) else res
        }
    }

    companion object {
        private const val COUNT = 30
        private const val SAVE_DIALOGS_OWNER_ID = "save-dialogs-owner-id"
        private val COMPARATOR: Comparator<Dialog> = DialogByIdMajorID()
        internal fun getTitleIfEmpty(users: Collection<User>): String? {
            return join(
                users,
                ", "
            ) {
                it.firstName ?: "null"
            }
        }
    }

    init {
        dialogsOwnerId = savedInstanceState?.getLong(SAVE_DIALOGS_OWNER_ID)
            ?: initialDialogsOwnerId
        messagesInteractor = messages
        stickerInteractor = InteractorFactory.createStickersInteractor()
        accountsInteractor = InteractorFactory.createAccountInteractor()
        longpollManager = LongpollInstance.longpollManager
        appendJob(
            messagesInteractor
                .observePeerUpdates()
                .sharedFlowToMain { onPeerUpdate(it) }
        )
        appendJob(
            messagesInteractor.observePeerDeleting()
                .sharedFlowToMain { dialog ->
                    onDialogDeleted(
                        dialog.accountId,
                        dialog.peerId
                    )
                }
        )
        appendJob(
            longpollManager.observeKeepAlive()
                .sharedFlowToMain { checkLongpoll() }
        )
        loadCachedThenActualData()
    }
}