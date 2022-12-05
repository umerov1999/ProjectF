package dev.ragnarok.fenrir.fragment.videoalbumsbyvideo

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IVideosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.VideoAlbum

class VideoAlbumsByVideoPresenter(
    accountId: Int,
    private val ownerId: Int,
    owner: Int,
    video: Int,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IVideoAlbumsByVideoView>(accountId, savedInstanceState) {
    private val videoOwnerId: Int = owner
    private val videoId: Int = video
    private val data: MutableList<VideoAlbum>
    private val videosInteractor: IVideosInteractor = InteractorFactory.createVideosInteractor()
    private var netLoadingNow = false
    private fun resolveRefreshingView() {
        view?.displayLoading(
            netLoadingNow
        )
    }

    private fun requestActualData() {
        netLoadingNow = true
        resolveRefreshingView()
        appendDisposable(videosInteractor.getAlbumsByVideo(
            accountId,
            ownerId,
            videoOwnerId,
            videoId
        )
            .fromIOToMain()
            .subscribe({ albums -> onActualDataReceived(albums) }) { t ->
                onActualDataGetError(
                    t
                )
            })
    }

    private fun onActualDataGetError(t: Throwable) {
        netLoadingNow = false
        resolveRefreshingView()
        showError(
            t
        )
    }

    private fun onActualDataReceived(albums: List<VideoAlbum>) {
        netLoadingNow = false
        resolveRefreshingView()
        data.clear()
        data.addAll(albums)
        view?.notifyDataSetChanged()
    }

    override fun onGuiCreated(viewHost: IVideoAlbumsByVideoView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
        resolveRefreshingView()
    }

    fun fireItemClick(album: VideoAlbum) {
        view?.openAlbum(
            accountId,
            ownerId,
            album.getId(),
            null,
            album.getTitle()
        )
    }

    fun fireRefresh() {
        requestActualData()
    }

    init {
        data = ArrayList()
        requestActualData()
    }
}