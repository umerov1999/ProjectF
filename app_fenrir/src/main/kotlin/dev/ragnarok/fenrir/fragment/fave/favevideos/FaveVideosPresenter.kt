package dev.ragnarok.fenrir.fragment.fave.favevideos

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Video
import io.reactivex.rxjava3.disposables.CompositeDisposable

class FaveVideosPresenter(accountId: Int, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IFaveVideosView>(accountId, savedInstanceState) {
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val mVideos: ArrayList<Video> = ArrayList()
    private val cacheDisposable = CompositeDisposable()
    private val netDisposable = CompositeDisposable()
    private var mEndOfContent = false
    private var cacheLoadingNow = false
    private var netLoadingNow = false
    private var doLoadTabs = false
    private fun resolveRefreshingView() {
        view?.showRefreshing(netLoadingNow)
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
        requestAtLast()
    }

    private fun loadCachedData() {
        cacheLoadingNow = true
        cacheDisposable.add(faveInteractor.getCachedVideos(accountId)
            .fromIOToMain()
            .subscribe({ videos -> onCachedDataReceived(videos) }) { t ->
                onCacheGetError(
                    t
                )
            })
    }

    private fun onCacheGetError(t: Throwable) {
        cacheLoadingNow = false
        showError(t)
    }

    private fun onCachedDataReceived(videos: List<Video>) {
        cacheLoadingNow = false
        mVideos.clear()
        mVideos.addAll(videos)
        view?.notifyDataSetChanged()
    }

    override fun onDestroyed() {
        cacheDisposable.dispose()
        netDisposable.dispose()
        super.onDestroyed()
    }

    private fun request(offset: Int) {
        netLoadingNow = true
        resolveRefreshingView()
        netDisposable.add(faveInteractor.getVideos(accountId, COUNT_PER_REQUEST, offset)
            .fromIOToMain()
            .subscribe({ videos ->
                onNetDataReceived(
                    offset,
                    videos
                )
            }) { t -> onNetDataGetError(t) })
    }

    private fun onNetDataGetError(t: Throwable) {
        netLoadingNow = false
        resolveRefreshingView()
        showError(t)
    }

    private fun onNetDataReceived(offset: Int, videos: List<Video>) {
        cacheDisposable.clear()
        cacheLoadingNow = false
        mEndOfContent = videos.isEmpty()
        netLoadingNow = false
        if (offset == 0) {
            mVideos.clear()
            mVideos.addAll(videos)
            view?.notifyDataSetChanged()
        } else {
            val startSize = mVideos.size
            mVideos.addAll(videos)
            view?.notifyDataAdded(
                startSize,
                videos.size
            )
        }
        resolveRefreshingView()
    }

    private fun requestAtLast() {
        request(0)
    }

    private fun requestNext() {
        request(mVideos.size)
    }

    override fun onGuiCreated(viewHost: IFaveVideosView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mVideos)
    }

    private fun canLoadMore(): Boolean {
        return mVideos.isNotEmpty() && !cacheLoadingNow && !netLoadingNow && !mEndOfContent
    }

    fun fireRefresh() {
        cacheDisposable.clear()
        netDisposable.clear()
        netLoadingNow = false
        requestAtLast()
    }

    fun fireVideoClick(video: Video) {
        view?.goToPreview(
            accountId,
            video
        )
    }

    fun fireVideoDelete(index: Int, video: Video) {
        netDisposable.add(faveInteractor.removeVideo(accountId, video.ownerId, video.id)
            .fromIOToMain()
            .subscribe({
                mVideos.removeAt(index)
                view?.notifyDataSetChanged()
            }) { t -> onNetDataGetError(t) })
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestNext()
        }
    }

    companion object {
        private const val COUNT_PER_REQUEST = 25
    }

    init {
        loadCachedData()
    }
}