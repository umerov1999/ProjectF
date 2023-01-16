package dev.ragnarok.fenrir.activity.photopager

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.AccessIdPair
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.util.Utils

class SimplePhotoPresenter(
    photos: ArrayList<Photo>, index: Int, needToRefreshData: Boolean,
    accountId: Long, context: Context, savedInstanceState: Bundle?
) : PhotoPagerPresenter(photos, accountId, !needToRefreshData, context, savedInstanceState) {
    private var mDataRefreshSuccessfull = false
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
        mDataRefreshSuccessfull = true
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

    init {
        currentIndex = index
        if (needToRefreshData && !mDataRefreshSuccessfull) {
            refreshData()
        }
    }
}