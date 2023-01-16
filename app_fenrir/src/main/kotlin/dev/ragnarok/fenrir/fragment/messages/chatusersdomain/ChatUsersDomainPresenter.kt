package dev.ragnarok.fenrir.fragment.messages.chatusersdomain

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.AppChatUser
import java.util.*

class ChatUsersDomainPresenter(
    accountId: Long,
    private val chatId: Long,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IChatUsersDomainView>(accountId, savedInstanceState) {
    private val messagesInteractor: IMessagesRepository
    private val users: MutableList<AppChatUser>
    private val original: MutableList<AppChatUser>
    private var refreshing = false
    private var query: String? = null
    fun setLoadingNow(loadingNow: Boolean) {
        refreshing = loadingNow
        resolveRefreshing()
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
            val user = i.getMember()
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

    override fun onGuiCreated(viewHost: IChatUsersDomainView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(users)
    }

    private fun resolveRefreshing() {
        view?.displayRefreshing(
            refreshing
        )
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshing()
    }

    private fun setRefreshing(refreshing: Boolean) {
        this.refreshing = refreshing
        resolveRefreshing()
    }

    private fun requestData() {
        setRefreshing(true)
        appendDisposable(messagesInteractor.getChatUsers(accountId, chatId)
            .fromIOToMain()
            .subscribe({ onDataReceived(it) }) { t ->
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
        updateCriteria()
    }

    fun fireRefresh() {
        if (!refreshing) {
            requestData()
        }
    }

    fun fireUserClick(user: AppChatUser) {
        user.getMember()?.let {
            view?.addDomain(
                accountId,
                it
            )
        }
    }

    fun fireUserLongClick(user: AppChatUser) {
        user.getMember()?.let {
            view?.openUserWall(
                accountId,
                it
            )
        }
    }

    init {
        users = ArrayList()
        original = ArrayList()
        messagesInteractor = messages
        requestData()
    }
}