package dev.ragnarok.fenrir.activity.photopager

import android.os.Bundle
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.db.serialize.Serializers
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.util.PersistentLogger
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import kotlinx.coroutines.flow.flow

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
        loadDataFromParcelNative(source)
    }

    override fun close() {
        view?.returnOnlyPos(currentIndex)
    }

    private fun loadDataFromDatabase(source: TmpSource) {
        changeLoadingNowState(true)
        appendJob(
            Stores.instance
                .tempStore()
                .getTemporaryData(source.ownerId, source.sourceId, Serializers.PHOTOS_SERIALIZER)
                .fromIOToMain({ onInitialLoadingFinished(it) }) {
                    PersistentLogger.logThrowable("TmpGalleryPagerPresenter", it)
                })
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
        refreshInfoViews(true)
    }
}