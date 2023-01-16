package dev.ragnarok.fenrir.fragment.groupchats

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IUtilsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.GroupChats
import dev.ragnarok.fenrir.model.LoadMoreState
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GroupChatsPresenter(accountId: Long, private val groupId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IGroupChatsView>(accountId, savedInstanceState) {
    private val chats: MutableList<GroupChats>
    private val utilsInteractor: IUtilsInteractor
    private val owners: IOwnersRepository
    private val netDisposable = CompositeDisposable()
    private var endOfContent = false
    private var actualDataReceived = false
    private var cacheLoadingNow = false
    private var netLoadingNow = false
    private var netLoadingNowOffset = 0
    private fun requestActualData(offset: Int) {
        netLoadingNow = true
        netLoadingNowOffset = offset
        resolveRefreshingView()
        resolveLoadMoreFooter()
        netDisposable.add(owners.getGroupChats(accountId, groupId, offset, COUNT_PER_REQUEST)
            .fromIOToMain()
            .subscribe({ rec_chats ->
                onActualDataReceived(
                    offset,
                    rec_chats
                )
            }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        netLoadingNow = false
        resolveRefreshingView()
        resolveLoadMoreFooter()
        showError(t)
    }

    private fun onActualDataReceived(offset: Int, rec_chats: List<GroupChats>) {
        cacheLoadingNow = false
        netLoadingNow = false
        resolveRefreshingView()
        resolveLoadMoreFooter()
        actualDataReceived = true
        endOfContent = rec_chats.isEmpty()
        if (offset == 0) {
            chats.clear()
            chats.addAll(rec_chats)
            view?.notifyDataSetChanged()
        } else {
            val startCount = chats.size
            chats.addAll(rec_chats)
            view?.notifyDataAdd(
                startCount,
                rec_chats.size
            )
        }
    }

    override fun onGuiCreated(viewHost: IGroupChatsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(chats)
        resolveRefreshingView()
        resolveLoadMoreFooter()
    }

    override fun onDestroyed() {
        netDisposable.dispose()
        super.onDestroyed()
    }

    private fun resolveRefreshingView() {
        view?.showRefreshing(netLoadingNow)
    }

    private fun resolveLoadMoreFooter() {
        if (netLoadingNow && netLoadingNowOffset > 0) {
            view?.setupLoadMore(
                LoadMoreState.LOADING
            )
            return
        }
        if (actualDataReceived && !netLoadingNow) {
            view?.setupLoadMore(
                LoadMoreState.CAN_LOAD_MORE
            )
        }
        view?.setupLoadMore(LoadMoreState.END_OF_LIST)
    }

    fun fireLoadMoreClick() {
        if (canLoadMore()) {
            requestActualData(chats.size)
        }
    }

    private fun canLoadMore(): Boolean {
        return actualDataReceived && !cacheLoadingNow && !endOfContent && chats.isNotEmpty()
    }

    fun fireRefresh() {
        netDisposable.clear()
        netLoadingNow = false
        cacheLoadingNow = false
        requestActualData(0)
    }

    fun fireGroupChatsClick(chat: GroupChats) {
        netDisposable.add(utilsInteractor.joinChatByInviteLink(accountId, chat.getInvite_link())
            .fromIOToMain()
            .subscribe({ t ->
                view?.goToChat(
                    accountId,
                    t.chat_id
                )
            }) { t -> onActualDataGetError(t) })
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestActualData(chats.size)
        }
    }

    companion object {
        private const val COUNT_PER_REQUEST = 20
    }

    init {
        chats = ArrayList()
        utilsInteractor = InteractorFactory.createUtilsInteractor()
        owners = Repository.owners
        requestActualData(0)
    }
}