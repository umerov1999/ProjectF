package dev.ragnarok.fenrir.fragment.feed

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.model.PostUpdate
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.IFeedInteractor
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.model.FeedSource
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.News
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.needReloadNews
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain

class FeedPresenter(accountId: Long, savedInstanceState: Bundle?) :
    PlaceSupportPresenter<IFeedView>(accountId, savedInstanceState) {
    private val feedInteractor: IFeedInteractor
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val walls: IWallsRepository = Repository.walls
    private val mFeed: MutableList<News>
    private val mFeedSources: MutableList<FeedSource>
    private val loadingHolder = CompositeJob()
    private val cacheLoadingHolder = CompositeJob()
    private var mNextFrom: String? = null
    private var mSourceIds: String? = null
    private var loadingNow = false
    private var loadingNowNextFrom: String? = null
    private var cacheLoadingNow = false
    private var mTmpFeedScrollOnGuiReady: String? = null
    private var needAskWhenGuiReady = false

    private fun onPostUpdateEvent(update: PostUpdate) {
        update.likeUpdate.requireNonNull {
            val index = indexOf(update.ownerId, update.postId)
            if (index != -1) {
                mFeed[index].setLikeCount(it.count)
                mFeed[index].setUserLike(it.isLiked)
                view?.notifyItemChanged(index)
            }
        }
    }

    private fun requestFeedAtLast(startFrom: String?) {
        loadingHolder.clear()
        val sourcesIds = mSourceIds
        loadingNowNextFrom = startFrom
        loadingNow = true
        resolveLoadMoreFooterView()
        resolveRefreshingView()
        if (setOf("updates_photos", "updates_videos", "updates_full", "updates_audios").contains(
                sourcesIds
            )
        ) {
            val fil = when (sourcesIds) {
                "updates_photos" -> "photo,photo_tag"
                "updates_audios" -> "audio"
                "updates_videos" -> "video"
                "updates_full" -> "photo,photo_tag,wall_photo,audio,video"
                else -> {
                    throw UnsupportedOperationException()
                }
            }
            loadingHolder.add(
                feedInteractor.getActualFeed(
                    accountId,
                    25,
                    startFrom,
                    fil,
                    9,
                    sourcesIds
                )
                    .fromIOToMain({
                        onActualFeedReceived(
                            startFrom,
                            it.first,
                            it.second
                        )
                    }) { t -> onActualFeedGetError(t) })
        } else {
            loadingHolder.add(
                feedInteractor.getActualFeed(
                    accountId,
                    25,
                    startFrom,
                    if (sourcesIds.isNullOrEmpty()) "post" else null,
                    9,
                    sourcesIds
                )
                    .fromIOToMain({
                        onActualFeedReceived(
                            startFrom,
                            it.first,
                            it.second
                        )
                    }) { t -> onActualFeedGetError(t) })
        }
    }

    private fun onActualFeedGetError(t: Throwable) {
        loadingNow = false
        loadingNowNextFrom = null
        resolveLoadMoreFooterView()
        resolveRefreshingView()
        showError(t)
    }

    private fun onActualFeedReceived(startFrom: String?, feed: List<News>, nextFrom: String?) {
        loadingNow = false
        loadingNowNextFrom = null
        mNextFrom = nextFrom
        if (startFrom.isNullOrEmpty()) {
            mFeed.clear()
            mFeed.addAll(feed)
            view?.notifyFeedDataChanged()
        } else {
            val startSize = mFeed.size
            mFeed.addAll(feed)
            view?.notifyDataAdded(
                startSize,
                feed.size
            )
        }
        resolveRefreshingView()
        resolveLoadMoreFooterView()
    }

    fun configureMenu(): List<FeedSource> {
        return mFeedSources
    }

    override fun onGuiCreated(viewHost: IFeedView) {
        super.onGuiCreated(viewHost)
        viewHost.displayFeedSources(mFeedSources)
        val sourceIndex = activeFeedSourceIndex
        if (sourceIndex != -1) {
            viewHost.scrollFeedSourcesToPosition(sourceIndex)
        }
        viewHost.displayFeed(mFeed, mTmpFeedScrollOnGuiReady)
        mTmpFeedScrollOnGuiReady = null
        resolveRefreshingView()
        resolveLoadMoreFooterView()

        if (needAskWhenGuiReady) {
            viewHost.askToReload()
            needAskWhenGuiReady = false
        }
    }

    private fun setCacheLoadingNow(cacheLoadingNow: Boolean) {
        this.cacheLoadingNow = cacheLoadingNow
        resolveRefreshingView()
        resolveLoadMoreFooterView()
    }

    private fun loadCachedFeed(thenScrollToState: String?) {
        setCacheLoadingNow(true)
        cacheLoadingHolder.add(
            feedInteractor
                .getCachedFeed(accountId)
                .fromIOToMain(
                    { onCachedFeedReceived(it, thenScrollToState) },
                    {
                        setCacheLoadingNow(false)
                        requestFeedAtLast(null)
                    }
                )
        )
    }

    override fun onDestroyed() {
        loadingHolder.cancel()
        cacheLoadingHolder.cancel()
        super.onDestroyed()
    }

    private fun onCachedFeedReceived(data: List<News>, thenScrollToState: String?) {
        setCacheLoadingNow(false)
        mFeed.clear()
        mFeed.addAll(data)
        if (thenScrollToState != null) {
            if (guiIsReady) {
                view?.displayFeed(
                    mFeed,
                    thenScrollToState
                )
            } else {
                mTmpFeedScrollOnGuiReady = thenScrollToState
            }
        } else {
            view?.notifyFeedDataChanged()
        }
        if (mFeed.isEmpty()) {
            requestFeedAtLast(null)
        } else {
            if (needReloadNews(accountId)) {
                val vr = Settings.get().main().start_newsMode
                if (vr == 2) {
                    if (view == null) {
                        needAskWhenGuiReady = true
                    } else {
                        view?.askToReload()
                    }
                } else if (vr == 1) {
                    view?.scrollTo(0)
                    requestFeedAtLast(null)
                }
            }
        }
    }

    private fun canLoadNextNow(): Boolean {
        return mNextFrom.nonNullNoEmpty() && !cacheLoadingNow && !loadingNow
    }

    private fun refreshFeedSourcesSelection(): Int {
        var result = -1
        for (i in mFeedSources.indices) {
            val source = mFeedSources[i]
            if (mSourceIds.isNullOrEmpty() && source.value.isNullOrEmpty()) {
                source.setActive(true)
                result = i
                continue
            }
            if (mSourceIds.nonNullNoEmpty() && source.value
                    .nonNullNoEmpty() && mSourceIds == source.value
            ) {
                source.setActive(true)
                result = i
                continue
            }
            source.setActive(false)
        }
        return result
    }

    private fun restoreNextFromAndFeedSources() {
        mSourceIds = Settings.get()
            .main()
            .getFeedSourceIds(accountId)
        mNextFrom = Settings.get()
            .main()
            .restoreFeedNextFrom(accountId)
    }

    private val isRefreshing: Boolean
        get() = cacheLoadingNow || loadingNow && loadingNowNextFrom.isNullOrEmpty()
    private val isMoreLoading: Boolean
        get() = loadingNow && loadingNowNextFrom.nonNullNoEmpty()

    private fun resolveRefreshingView() {
        view?.showRefreshing(isRefreshing)
    }

    private val activeFeedSourceIndex: Int
        get() {
            for (i in mFeedSources.indices) {
                if (mFeedSources[i].isActive) {
                    return i
                }
            }
            return -1
        }

    private fun resolveLoadMoreFooterView() {
        if (mFeed.nonNullNoEmpty() && mNextFrom.isNullOrEmpty()) {
            view?.setupLoadMoreFooter(LoadMoreState.END_OF_LIST)
        } else if (isMoreLoading) {
            view?.setupLoadMoreFooter(LoadMoreState.LOADING)
        } else if (canLoadNextNow()) {
            view?.setupLoadMoreFooter(LoadMoreState.CAN_LOAD_MORE)
        } else {
            view?.setupLoadMoreFooter(LoadMoreState.END_OF_LIST)
        }
    }

    fun fireScrollStateOnPause(json: String?) {
        Settings.get()
            .main()
            .storeFeedScrollState(accountId, json)
    }

    fun fireRefresh() {
        cacheLoadingHolder.clear()
        loadingHolder.clear()
        loadingNow = false
        cacheLoadingNow = false
        requestFeedAtLast(null)
    }

    fun fireScrollToBottom() {
        if (canLoadNextNow()) {
            requestFeedAtLast(mNextFrom)
        }
    }

    fun fireLoadMoreClick() {
        if (canLoadNextNow()) {
            requestFeedAtLast(mNextFrom)
        }
    }

    fun fireFeedSourceClick(entry: FeedSource) {
        mSourceIds = entry.value
        mNextFrom = null
        cacheLoadingHolder.clear()
        loadingHolder.clear()
        loadingNow = false
        cacheLoadingNow = false
        refreshFeedSourcesSelection()
        view?.notifyFeedSourcesChanged()
        requestFeedAtLast(null)
    }

    fun fireNewsShareLongClick(news: News) {
        view?.goToReposts(
            accountId,
            news.type.orEmpty(),
            news.sourceId,
            news.postId
        )
    }

    fun fireNewsLikeLongClick(news: News) {
        view?.goToLikes(
            accountId,
            news.type.orEmpty(),
            news.sourceId,
            news.postId
        )
    }

    fun fireAddBookmark(ownerId: Long, postId: Int) {
        appendJob(
            faveInteractor.addPost(accountId, ownerId, postId, null)
                .fromIOToMain({ onPostAddedToBookmarks() }) { t ->
                    showError(getCauseIfRuntime(t))
                })
    }

    private fun onPostAddedToBookmarks() {
        view?.showSuccessToast()
    }

    fun fireNewsCommentClick(news: News) {
        if ("post".equals(news.type, ignoreCase = true)) {
            view?.goToPostComments(
                accountId,
                news.postId,
                news.sourceId
            )
        }
    }

    fun fireBanClick(news: News) {
        appendJob(
            feedInteractor.addBan(accountId, setOf(news.sourceId))
                .fromIOToMain({ fireRefresh() }) { t -> onActualFeedGetError(t) })
    }

    fun fireIgnoreClick(news: News) {
        val type = if ("post" == news.type) "wall" else news.type
        appendJob(
            feedInteractor.ignoreItem(accountId, type, news.sourceId, news.postId)
                .fromIOToMain({
                    if (it.status) {
                        fireRefresh()
                    } else {
                        view?.showError(it.message)
                    }
                }) { t -> onActualFeedGetError(t) })
    }

    fun fireNewsBodyClick(news: News) {
        if ("post" == news.type) {
            val post = news.toPost()
            if (post != null) {
                view?.openPost(accountId, post)
            }
        }
    }

    fun fireNewsRepostClick(news: News) {
        if ("post" == news.type) {
            news.toPost()?.let {
                view?.repostPost(
                    accountId,
                    it
                )
            }
        }
    }

    fun fireLikeClick(news: News) {
        if (Utils.isHiddenAccount(
                accountId
            )
        ) {
            return
        }
        if ("post".equals(news.type, ignoreCase = true)) {
            val add = !news.isUserLike
            appendJob(
                walls.like(accountId, news.sourceId, news.postId, add)
                    .hiddenIO()
            )
        }
    }

    private fun indexOf(sourceId: Long, postId: Int): Int {
        for (i in mFeed.indices) {
            if (mFeed[i].sourceId == sourceId && mFeed[i].postId == postId) {
                return i
            }
        }
        return -1
    }

    companion object {
        internal fun createDefaultFeedSources(): List<FeedSource> {
            val data: MutableList<FeedSource> = ArrayList(8)
            data.add(FeedSource(null, R.string.news_feed, false))
            data.add(FeedSource("likes", R.string.likes_posts, false))
            data.add(FeedSource("updates_full", R.string.updates, false))
            data.add(FeedSource("friends", R.string.friends, false))
            if (Utils.isOfficialVKCurrent) {
                data.add(FeedSource("top", R.string.interesting, false))
            }
            data.add(FeedSource("groups", R.string.groups, false))
            data.add(FeedSource("pages", R.string.pages, false))
            data.add(FeedSource("following", R.string.subscriptions, false))
            data.add(FeedSource("updates_photos", R.string.photos, false))
            data.add(FeedSource("updates_videos", R.string.videos, false))
            data.add(FeedSource("updates_audios", R.string.audios, false))
            return data
        }
    }

    init {
        appendJob(
            walls.observeMinorChanges()
                .sharedFlowToMain { onPostUpdateEvent(it) })
        feedInteractor = InteractorFactory.createFeedInteractor()
        mFeed = ArrayList()
        mFeedSources = ArrayList(createDefaultFeedSources())
        restoreNextFromAndFeedSources()
        refreshFeedSourcesSelection()
        val scrollState = Settings.get()
            .main()
            .restoreFeedScrollState(accountId)
        loadCachedFeed(scrollState)
    }
}
