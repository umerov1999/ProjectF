package dev.ragnarok.filegallery.activity.photopager

import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.fromIOToMain
import kotlinx.coroutines.flow.flow

class TmpGalleryPagerPresenter(
    source: Long, index: Int
) : PhotoPagerPresenter(ArrayList(0)) {
    override fun close() {
        if (mPhotos.isEmpty()) {
            view?.closeOnly()
        } else {
            currentFile?.let { view?.returnFileInfo(it) }
        }
    }

    private fun onInitialLoadingFinished(photos: List<Photo>) {
        changeLoadingNowState(false)
        mPhotos.addAll(photos)
        refreshPagerView()
        resolveButtonsBarVisible()
        resolveToolbarVisibility()
        refreshInfoViews()
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

    init {
        currentIndex = index
        loadDataFromParcelNative(source)
    }
}