package dev.ragnarok.fenrir.activity.photopager

import android.os.Bundle
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.AccessIdPair
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter

class SimplePhotoPresenter : PhotoPagerPresenter {
    private var mDataRefreshSuccessfully = false

    constructor(
        photos: ArrayList<Photo>,
        index: Int,
        needToRefreshData: Boolean,
        accountId: Long,
        savedInstanceState: Bundle?
    ) : super(photos, accountId, !needToRefreshData, savedInstanceState) {
        currentIndex = index
        if (needToRefreshData && !mDataRefreshSuccessfully) {
            refreshData()
        }
    }

    constructor(
        parcelNativePointer: Long,
        index: Int,
        needToRefreshData: Boolean,
        accountId: Long,
        savedInstanceState: Bundle?
    ) : super(ArrayList<Photo>(0), accountId, !needToRefreshData, savedInstanceState) {
        currentIndex = index
        loadDataFromParcelNative(parcelNativePointer, needToRefreshData)
    }

    private fun loadDataFromParcelNative(parcelNative: Long, needToRefreshData: Boolean) {
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
                .subscribe({ onInitialLoadingParcelNativeFinished(it, needToRefreshData) }) {
                    it.printStackTrace()
                })
    }

    private fun onInitialLoadingParcelNativeFinished(
        photos: List<Photo>,
        needToRefreshData: Boolean
    ) {
        changeLoadingNowState(false)
        mPhotos.addAll(photos)
        refreshPagerView()
        resolveButtonsBarVisible()
        resolveToolbarVisibility()
        refreshInfoViews(true)

        if (needToRefreshData && !mDataRefreshSuccessfully) {
            refreshData()
        }
    }

    private fun refreshData() {
        val ids = ArrayList<AccessIdPair>(mPhotos.size)
        for (photo in mPhotos) {
            ids.add(AccessIdPair(photo.getObjectId(), photo.ownerId, photo.accessKey))
        }
        appendDisposable(photosInteractor.getPhotosByIds(accountId, ids)
            .fromIOToMain()
            .subscribe({ onPhotosReceived(it) }) { t ->
                view?.let {
                    showError(
                        it,
                        Utils.getCauseIfRuntime(t)
                    )
                }
            })
    }

    private fun onPhotosReceived(photos: List<Photo>) {
        mDataRefreshSuccessfully = true
        onPhotoListRefresh(photos)
    }

    private fun onPhotoListRefresh(photos: List<Photo>) {
        val originalData: MutableList<Photo> = mPhotos
        for (photo in photos) {
            //замена старых обьектов новыми
            for (i in originalData.indices) {
                val orig = originalData[i]
                if (orig.getObjectId() == photo.getObjectId() && orig.ownerId == photo.ownerId) {
                    originalData[i] = photo

                    // если у фото до этого не было ссылок на файлы
                    if (orig.sizes == null || orig.sizes?.isEmpty() == true) {
                        view?.rebindPhotoAt(
                            i
                        )
                    }
                    break
                }
            }
        }
        refreshInfoViews(true)
    }

    override fun close() {
        view?.returnOnlyPos(currentIndex)
    }
}