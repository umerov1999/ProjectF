package dev.ragnarok.fenrir.fragment.messages.chatmembers

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.AppChatUser
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import java.util.Locale

class ChatMembersPresenter(accountId: Long, private val chatId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IChatMembersView>(accountId, savedInstanceState) {
    private val messagesInteractor: IMessagesRepository = messages
    private val users: MutableList<AppChatUser> = ArrayList()
    private val original: MutableList<AppChatUser> = ArrayList()
    private var refreshing = false
    private var isOwner = false
    private var query: String? = null

    fun setLoadingNow(loadingNow: Boolean) {
        refreshing = loadingNow
        resolveRefreshing()
    }

    private fun resolveRefreshing() {
        resumedView?.displayRefreshing(
            refreshing
        )
    }

    private fun updateCriteria() {
        setLoadingNow(true)
        users.clear()
        if (query.isNullOrEmpty()) {
            users.addAll(original)
            setLoadingNow(false)
            view?.notifyDataSetChanged()
            return
        }
        for (i in original) {
            val user = i.member
            if (query?.lowercase(Locale.getDefault())?.let {
                    user?.fullName?.lowercase(Locale.getDefault())?.contains(it)
                } == true || query?.lowercase(Locale.getDefault())?.let {
                    user?.domain?.lowercase(Locale.getDefault())?.contains(
                        it
                    )
                } == true
            ) {
                users.add(i)
            }
        }
        setLoadingNow(false)
        view?.notifyDataSetChanged()
    }

    fun fireQuery(q: String?) {
        query = if (q.isNullOrEmpty()) null else {
            q
        }
        updateCriteria()
    }

    override fun onGuiCreated(viewHost: IChatMembersView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(users)
    }

    private fun setRefreshing(refreshing: Boolean) {
        this.refreshing = refreshing
        resolveRefreshing()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshing()
    }

    private fun requestData() {
        setRefreshing(true)
        appendJob(
            messagesInteractor.getChatUsers(accountId, chatId)
                .fromIOToMain({ onDataReceived(it) }) { t ->
                    onDataGetError(
                        t
                    )
                })
    }

    private fun onDataGetError(t: Throwable) {
        setRefreshing(false)
        showError(t)
    }

    private fun onDataReceived(data: List<AppChatUser>) {
        setRefreshing(false)

        original.clear()
        original.addAll(data)
        isOwner = false
        for (i in original) {
            if (i.getOwnerObjectId() == accountId) {
                isOwner = i.isOwner
                break
            }
        }
        view?.setIsOwner(isOwner)
        updateCriteria()
    }

    fun fireRefresh() {
        if (!refreshing) {
            requestData()
        }
    }

    fun fireAddUserClick() {
        view?.startSelectUsersActivity(
            accountId
        )
    }

    fun fireUserDeleteConfirmed(user: AppChatUser) {
        val userId = user.member?.ownerId ?: return
        appendJob(
            messagesInteractor.removeChatMember(accountId, chatId, userId)
                .fromIOToMain({ onUserRemoved(userId) }) { t ->
                    showError(getCauseIfRuntime(t))
                })
    }

    private fun onUserRemoved(id: Long) {
        var index = findIndexById(original, id)
        if (index != -1) {
            original.removeAt(index)
        }

        index = findIndexById(users, id)
        if (index != -1) {
            users.removeAt(index)
            view?.notifyItemRemoved(
                index
            )
        }
    }

    fun fireUserSelected(owners: ArrayList<Owner>?) {
        owners ?: return
        val usersData = ArrayList<User>()
        for (i in owners) {
            if (i is User) {
                usersData.add(i)
            }
        }
        if (usersData.nonNullNoEmpty()) {
            appendJob(
                messagesInteractor.addChatUsers(accountId, chatId, usersData)
                    .fromIOToMain({ onChatUsersAdded(it) }) { t ->
                        onChatUsersAddError(
                            t
                        )
                    })
        }
    }

    private fun onChatUsersAddError(t: Throwable) {
        showError(getCauseIfRuntime(t))
        requestData() // refresh data
    }

    private fun onChatUsersAdded(added: List<AppChatUser>) {
        val startSize = users.size
        original.addAll(added)
        users.addAll(added)

        view?.notifyDataAdded(
            startSize,
            added.size
        )
    }

    fun fireUserClick(user: AppChatUser) {
        view?.openUserWall(
            accountId,
            user.member ?: return
        )
    }

    fun fireAdminToggleClick(isAdmin: Boolean, ownerId: Long) {
        appendJob(
            messagesInteractor.setMemberRole(accountId, chatId, ownerId, isAdmin)
                .fromIOToMain({ fireRefresh() }) { t -> onChatUsersAddError(t) })
    }

    init {
        requestData()
    }
}