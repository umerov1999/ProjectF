package dev.ragnarok.filegallery.activity.photopager

import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.filegallery.fromIOToMain
import dev.ragnarok.filegallery.model.Photo
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter

class TmpGalleryPagerPresenter(
    source: Long, index: Int
) : PhotoPagerPresenter(ArrayList(0)) {
    override fun close() {
        if (mPhotos.isEmpty()) {
            view?.closeOnly()
        } else {
            view?.returnFileInfo(currentFile)
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
        appendDisposable(
            Single.create { v: SingleEmitter<ArrayList<Photo>> ->
                v.onSuccess(
                    ParcelNative.loadParcelableArrayList(
                        parcelNative, Photo.NativeCreator, ParcelFlags.MUTABLE_LIST
                    ) ?: ArrayList()
                )
            }
                .fromIOToMain()
                .subscribe({ onInitialLoadingFinished(it) }) {
                    it.printStackTrace()
                })
    }

    init {
        currentIndex = index
        loadDataFromParcelNative(source)
    }
}