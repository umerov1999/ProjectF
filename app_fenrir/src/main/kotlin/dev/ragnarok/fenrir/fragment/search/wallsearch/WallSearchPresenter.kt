package dev.ragnarok.fenrir.fragment.search.wallsearch

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.db.model.PostUpdate
import dev.ragnarok.fenrir.domain.ILikesInteractor
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchPresenter
import dev.ragnarok.fenrir.fragment.search.criteria.WallSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.core.Single

class WallSearchPresenter(
    accountId: Long,
    criteria: WallSearchCriteria?,
    savedInstanceState: Bundle?
) : AbsSearchPresenter<IWallSearchView, WallSearchCriteria, Post, IntNextFrom>(
    accountId,
    criteria,
    savedInstanceState
) {
    override fun readParcelSaved(savedInstanceState: Bundle, key: String): WallSearchCriteria? {
        return savedInstanceState.getParcelableCompat(key)
    }

    private val walls: IWallsRepository = Repository.walls
    private fun onPostMinorUpdates(update: PostUpdate) {
        for (i in data.indices) {
            val post = data[i]
            if (post.vkid == update.postId && post.ownerId == update.ownerId) {
                update.likeUpdate.requireNonNull {
                    post.setLikesCount(it.count)
                    post.setUserLikes(it.isLiked)
                }
                update.deleteUpdate.requireNonNull {
                    post.setDeleted(it.isDeleted)
                }
                var pinStateChanged = false
                update.pinUpdate.requireNonNull {
                    pinStateChanged = true
                    for (p in data) {
                        p.setPinned(false)
                    }
                    post.setPinned(it.isPinned)
                }
                if (pinStateChanged) {
                    view?.notifyDataSetChanged()
                } else {
                    view?.notifyItemChanged(
                        i
                    )
                }
                break
            }
        }
    }

    override val initialNextFrom: IntNextFrom
        get() = IntNextFrom(0)

    override fun isAtLast(startFrom: IntNextFrom): Boolean {
        return startFrom.offset == 0
    }

    override fun doSearch(
        accountId: Long,
        criteria: WallSearchCriteria,
        startFrom: IntNextFrom
    ): Single<Pair<List<Post>, IntNextFrom>> {
        val offset = startFrom.offset
        val nextFrom = IntNextFrom(offset + COUNT)
        return walls.search(accountId, criteria.ownerId, criteria.query, true, COUNT, offset)
            .map { create(it.first, nextFrom) }
    }

    override fun instantiateEmptyCriteria(): WallSearchCriteria {
        // not supported
        throw UnsupportedOperationException()
    }

    override fun canSearch(criteria: WallSearchCriteria?): Boolean {
        return criteria?.query.trimmedNonNullNoEmpty()
    }

    fun fireShowCopiesClick(post: Post) {
        fireCopiesLikesClick("post", post.ownerId, post.vkid, ILikesInteractor.FILTER_COPIES)
    }

    fun fireShowLikesClick(post: Post) {
        fireCopiesLikesClick("post", post.ownerId, post.vkid, ILikesInteractor.FILTER_LIKES)
    }

    fun fireLikeClick(post: Post) {
        if (Settings.get().main().isDisable_likes || Utils.isHiddenAccount(
                accountId
            )
        ) {
            return
        }
        appendDisposable(walls.like(accountId, post.ownerId, post.vkid, !post.isUserLikes)
            .fromIOToMain()
            .subscribe(ignore()) { t ->
                showError(t)
            })
    }

    companion object {
        private const val COUNT = 30
    }

    init {
        appendDisposable(walls.observeMinorChanges()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onPostMinorUpdates(it) })
    }
}