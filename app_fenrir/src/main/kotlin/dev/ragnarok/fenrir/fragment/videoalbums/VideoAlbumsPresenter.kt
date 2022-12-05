package dev.ragnarok.fenrir.fragment.videoalbums

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IVideosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.VideoAlbum
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.disposables.CompositeDisposable

class VideoAlbumsPresenter(
    accountId: Int,
    private val ownerId: Int,
    private val action: String?,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IVideoAlbumsView>(accountId, savedInstanceState) {
    private val data: MutableList<VideoAlbum>
    private val videosInteractor: IVideosInteractor = InteractorFactory.createVideosInteractor()
    private val netDisposable = CompositeDisposable()
    private val cacheDisposable = CompositeDisposable()
    private var endOfContent = false
    private var actualDataReceived = false
    private var netLoadingNow = false
    private var cacheNowLoading = false
    private var doLoadTabs = false
    private fun resolveRefreshingView() {
        view?.displayLoading(
            netLoadingNow
        )
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
        requestActualData(0)
    }

    private fun requestActualData(offset: Int) {
        netLoadingNow = true
        resolveRefreshingView()
        netDisposable.add(videosInteractor.getActualAlbums(
            accountId,
            ownerId,
            COUNT_PER_LOAD,
            offset
        )
            .fromIOToMain()
            .subscribe({ albums ->
                onActualDataReceived(
                    offset,
                    albums
                )
            }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        netLoadingNow = false
        resolveRefreshingView()
        showError(t)
    }

    private fun onActualDataReceived(offset: Int, albums: List<VideoAlbum>) {
        cacheDisposable.clear()
        cacheNowLoading = false
        netLoadingNow = false
        actualDataReceived = true
        endOfContent = albums.isEmpty()
        resolveRefreshingView()
        if (offset == 0) {
            data.clear()
            data.addAll(albums)
            view?.notifyDataSetChanged()
        } else {
            val startSize = data.size
            data.addAll(albums)
            view?.notifyDataAdded(
                startSize,
                albums.size
            )
        }
    }

    private fun loadAllDataFromDb() {
        cacheDisposable.add(
            videosInteractor.getCachedAlbums(accountId, ownerId)
                .fromIOToMain()
                .subscribe({ albums -> onCachedDataReceived(albums) }, ignore())
        )
    }

    private fun onCachedDataReceived(albums: List<VideoAlbum>) {
        cacheNowLoading = false
        data.clear()
        data.addAll(albums)
        view?.notifyDataSetChanged()
    }

    override fun onDestroyed() {
        cacheDisposable.dispose()
        netDisposable.dispose()
        super.onDestroyed()
    }

    override fun onGuiCreated(viewHost: IVideoAlbumsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
    }

    private fun canLoadMore(): Boolean {
        return !endOfContent && actualDataReceived && !netLoadingNow && !cacheNowLoading && data.isNotEmpty()
    }

    fun fireItemClick(album: VideoAlbum) {
        view?.openAlbum(
            accountId,
            ownerId,
            album.getId(),
            action,
            album.getTitle()
        )
    }

    fun fireRefresh() {
        cacheDisposable.clear()
        cacheNowLoading = false
        netDisposable.clear()
        requestActualData(0)
    }

    fun fireScrollToLast() {
        if (canLoadMore()) {
            requestActualData(data.size)
        }
    }

    companion object {
        private const val COUNT_PER_LOAD = 40
    }

    init {
        data = ArrayList()
        loadAllDataFromDb()
    }
}