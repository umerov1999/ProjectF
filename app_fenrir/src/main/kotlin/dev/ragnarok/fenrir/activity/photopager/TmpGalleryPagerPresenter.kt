package dev.ragnarok.fenrir.activity.photopager

import android.os.Bundle
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.db.serialize.Serializers
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.util.PersistentLogger

class TmpGalleryPagerPresenter : PhotoPagerPresenter {
    constructor(
        accountId: Long, source: TmpSource, index: Int,
        savedInstanceState: Bundle?
    ) : super(ArrayList<Photo>(0), accountId, false, savedInstanceState) {
        currentIndex = index
        loadDataFromDatabase(source)
    }

    constructor(
        accountId: Long, source: Long, index: Int,
        savedInstanceState: Bundle?
    ) : super(ArrayList<Photo>(0), accountId, false, savedInstanceState) {
        currentIndex = index
        changeLoadingNowState(true)
        onInitialLoadingFinished(
            ParcelNative.fromNative(source).readParcelableList(Photo.NativeCreator)!!
        )
    }

    override fun close() {
        view?.returnOnlyPos(currentIndex)
    }

    private fun loadDataFromDatabase(source: TmpSource) {
        changeLoadingNowState(true)
        appendDisposable(Stores.instance
            .tempStore()
            .getTemporaryData(source.ownerId, source.sourceId, Serializers.PHOTOS_SERIALIZER)
            .fromIOToMain()
            .subscribe({ onInitialLoadingFinished(it) }) {
                PersistentLogger.logThrowable("TmpGalleryPagerPresenter", it)
            })
    }

    private fun onInitialLoadingFinished(photos: List<Photo>) {
        changeLoadingNowState(false)
        mPhotos.addAll(photos)
        refreshPagerView()
        resolveButtonsBarVisible()
        resolveToolbarVisibility()
        refreshInfoViews(true)
    }
}