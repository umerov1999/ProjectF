package dev.ragnarok.filegallery.activity.photopager

import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.filegallery.Includes.networkInterfaces
import dev.ragnarok.filegallery.api.interfaces.ILocalServerApi
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.fromIOToMain
import kotlinx.coroutines.flow.flow

class PhotoAlbumPagerPresenter(
    index: Int, source: Long, invertPhotoRev: Boolean
) : PhotoPagerPresenter(ArrayList(0)) {
    private val photosInteractor: ILocalServerApi = networkInterfaces.localServerApi()
    private val invertPhotoRev: Boolean
    private var canLoad: Boolean
    private fun loadData() {
        if (!canLoad) return
        changeLoadingNowState(true)
        appendJob(
            photosInteractor.getPhotos(mPhotos.size, COUNT_PER_LOAD, invertPhotoRev)
                .fromIOToMain({ onActualPhotosReceived(it) }) { t ->
                    onActualDataGetError(
                        t
                    )
                })
    }

    private fun onActualDataGetError(t: Throwable) {
        view?.let { showError(it, Utils.getCauseIfRuntime(t)) }
    }

    override fun close() {
        if (FenrirNative.isNativeLoaded) {
            val ptr = ParcelNative.createParcelableList(mPhotos, ParcelFlags.NULL_LIST)
            view?.returnInfo(
                currentIndex,
                ptr
            )
        } else {
            view?.closeOnly()
        }
    }

    private fun onActualPhotosReceived(data: MutableList<Photo>) {
        changeLoadingNowState(false)
        if (data.isEmpty()) {
            canLoad = false
            return
        }
        mPhotos.addAll(data)
        refreshPagerView()
        resolveButtonsBarVisible()
        resolveToolbarVisibility()
        refreshInfoViews()
    }

    override fun afterPageChangedFromUi(oldPage: Int, newPage: Int) {
        super.afterPageChangedFromUi(oldPage, newPage)
        if (oldPage == newPage) return
        if (newPage == count() - 1) {
            loadData()
        }
    }

    private fun loadDataFromParcelNative(parcelNative: Long) {
        changeLoadingNowState(true)
        appendJob(
            flow {
                emit(
                    ParcelNative.loadParcelableArrayList(
                        parcelNative, Photo.NativeCreator, ParcelFlags.MUTABLE_LIST
                    ) ?: ArrayList()
                )
            }
                .fromIOToMain({ onInitialLoadingFinished(it) }) {
                    it.printStackTrace()
                })
    }

    private fun onInitialLoadingFinished(photos: List<Photo>) {
        changeLoadingNowState(false)
        mPhotos.addAll(photos)
        refreshPagerView()
        resolveButtonsBarVisible()
        resolveToolbarVisibility()
        refreshInfoViews()
    }

    companion object {
        private const val COUNT_PER_LOAD = 100
    }

    init {
        canLoad = true
        this.invertPhotoRev = invertPhotoRev
        currentIndex = index
        loadDataFromParcelNative(source)
    }
}
