package dev.ragnarok.fenrir.fragment.fave.favephotos

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.flow

class FavePhotosPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IFavePhotosView>(accountId, savedInstanceState) {
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val mPhotos: ArrayList<Photo> = ArrayList()
    private val cacheDisposable = CompositeJob()
    private val netDisposable = CompositeJob()
    private var mEndOfContent = false
    private var cacheLoadingNow = false
    private var requestNow = false
    private var doLoadTabs = false
    private fun resolveRefreshingView() {
        view?.showRefreshing(requestNow)
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
        requestAtLast()
    }

    private fun loadAllCachedData() {
        cacheLoadingNow = true
        cacheDisposable.add(
            faveInteractor.getCachedPhotos(accountId)
                .fromIOToMain({ photos -> onCachedDataReceived(photos) }) { t ->
                    onCacheGetError(
                        t
                    )
                })
    }

    private fun onCacheGetError(t: Throwable) {
        cacheLoadingNow = false
        showError(t)
    }

    private fun onCachedDataReceived(photos: List<Photo>) {
        cacheLoadingNow = false
        mPhotos.clear()
        mPhotos.addAll(photos)
        view?.notifyDataSetChanged()
    }

    override fun onDestroyed() {
        cacheDisposable.cancel()
        netDisposable.cancel()
        super.onDestroyed()
    }

    private fun setRequestNow(requestNow: Boolean) {
        this.requestNow = requestNow
        resolveRefreshingView()
    }

    private fun request(offset: Int) {
        setRequestNow(true)
        netDisposable.add(
            faveInteractor.getPhotos(accountId, COUNT_PER_REQUEST, offset)
                .fromIOToMain({ photos ->
                    onActualDataReceived(
                        offset,
                        photos
                    )
                }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        setRequestNow(false)
        showError(getCauseIfRuntime(t))
    }

    private fun onActualDataReceived(offset: Int, photos: List<Photo>) {
        mEndOfContent = photos.isEmpty()
        cacheDisposable.clear()
        setRequestNow(false)
        if (offset == 0) {
            mPhotos.clear()
            mPhotos.addAll(photos)
            view?.notifyDataSetChanged()
        } else {
            val startSize = mPhotos.size
            mPhotos.addAll(photos)
            view?.notifyDataAdded(
                startSize,
                photos.size
            )
        }
    }

    private fun requestAtLast() {
        request(0)
    }

    private fun requestNext() {
        request(mPhotos.size)
    }

    override fun onGuiCreated(viewHost: IFavePhotosView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mPhotos)
    }

    private fun canLoadMore(): Boolean {
        return mPhotos.isNotEmpty() && !requestNow && !mEndOfContent && !cacheLoadingNow
    }

    fun fireRefresh() {
        netDisposable.clear()
        cacheDisposable.clear()
        cacheLoadingNow = false
        requestNow = false
        requestAtLast()
    }

    fun firePhotoClick(position: Int) {
        if (FenrirNative.isNativeLoaded && Settings.get().main().isNative_parcel_photo) {
            appendJob(
                flow {
                    val mem = ParcelNative.create(ParcelFlags.NULL_LIST)
                    mem.writeInt(mPhotos.size)
                    for (i in mPhotos.indices) {
                        if (!isActive()) {
                            mem.forceDestroy()
                            return@flow
                        }
                        val photo = mPhotos[i]
                        mem.writeParcelable(photo)
                    }
                    if (!isActive()) {
                        mem.forceDestroy()
                    } else {
                        emit(mem.nativePointer)
                    }
                }.fromIOToMain({
                    view?.goToGalleryNative(
                        accountId,
                        it,
                        position
                    )
                }) { obj -> obj.printStackTrace() })
        } else {
            view?.goToGallery(
                accountId,
                mPhotos,
                position
            )
        }
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestNext()
        }
    }

    companion object {
        private const val COUNT_PER_REQUEST = 50
    }

    init {
        loadAllCachedData()
    }
}