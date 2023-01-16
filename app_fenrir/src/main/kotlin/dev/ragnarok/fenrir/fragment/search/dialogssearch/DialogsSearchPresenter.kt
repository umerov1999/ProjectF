package dev.ragnarok.fenrir.fragment.search.dialogssearch

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchPresenter
import dev.ragnarok.fenrir.fragment.search.criteria.DialogsSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Conversation
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import io.reactivex.rxjava3.core.Single

class DialogsSearchPresenter(
    accountId: Long,
    criteria: DialogsSearchCriteria?,
    savedInstanceState: Bundle?
) : AbsSearchPresenter<IDialogsSearchView, DialogsSearchCriteria, Conversation, IntNextFrom>(
    accountId,
    criteria,
    savedInstanceState
) {
    private val messagesInteractor: IMessagesRepository = messages
    override val initialNextFrom: IntNextFrom
        get() = IntNextFrom(0)

    override fun readParcelSaved(savedInstanceState: Bundle, key: String): DialogsSearchCriteria? {
        return savedInstanceState.getParcelableCompat(key)
    }

    override fun isAtLast(startFrom: IntNextFrom): Boolean {
        return startFrom.offset == 0
    }

    override fun doSearch(
        accountId: Long,
        criteria: DialogsSearchCriteria,
        startFrom: IntNextFrom
    ): Single<Pair<List<Conversation>, IntNextFrom>> {
        val offset = startFrom.offset
        if (offset > 0) {
            return Single.just(Pair(emptyList(), startFrom))
        }
        return messagesInteractor.searchConversations(accountId, 255, criteria.query)
            .map {
                create(
                    it,
                    startFrom
                )
            }
        // null because load more not supported
    }

    override fun instantiateEmptyCriteria(): DialogsSearchCriteria {
        return DialogsSearchCriteria("")
    }

    override fun canSearch(criteria: DialogsSearchCriteria?): Boolean {
        return criteria?.query.trimmedNonNullNoEmpty()
    }

    fun fireEntryClick(o: Conversation) {
        val messagesOwnerId = accountId // todo Community dialogs search !!!
        val peer =
            Peer(Peer.fromOwnerId(o.getId())).setAvaUrl(o.imageUrl).setTitle(o.getDisplayTitle())
                .setAvaUrl(o.imageUrl)
        view?.openChatWith(
            accountId,
            messagesOwnerId,
            peer
        )
    }

}