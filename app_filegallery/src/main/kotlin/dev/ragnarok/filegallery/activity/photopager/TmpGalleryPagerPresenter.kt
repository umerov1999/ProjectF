package dev.ragnarok.filegallery.activity.photopager

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.filegallery.model.Photo

class TmpGalleryPagerPresenter(
    source: Long, index: Int, context: Context?,
    savedInstanceState: Bundle?
) : PhotoPagerPresenter(ArrayList(0), context!!, savedInstanceState) {
    override fun close() {
        view?.returnFileInfo(currentFile)
    }

    private fun onInitialLoadingFinished(photos: List<Photo>) {
        changeLoadingNowState(false)
        mPhotos.addAll(photos)
        refreshPagerView()
        resolveButtonsBarVisible()
        resolveToolbarVisibility()
        refreshInfoViews()
    }

    init {
        currentIndex = index
        changeLoadingNowState(true)
        onInitialLoadingFinished(
            ParcelNative.fromNative(source).readParcelableList(Photo.NativeCreator)!!
        )
    }
}