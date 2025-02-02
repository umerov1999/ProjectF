package dev.ragnarok.fenrir.activity.photopager

import android.os.Bundle
import dev.ragnarok.fenrir.model.AccessIdPairModel
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.util.PersistentLogger
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import kotlinx.coroutines.flow.flow

class FavePhotoPagerPresenter : PhotoPagerPresenter {
    private var mUpdated: BooleanArray
    private var refreshing: BooleanArray

    constructor(
        photos: ArrayList<Photo>,
        index: Int,
        accountId: Long,
        savedInstanceState: Bundle?
    ) : super(photos, accountId, false, savedInstanceState) {
        mUpdated = BooleanArray(photos.size)
        refreshing = BooleanArray(photos.size)
        currentIndex = index
        refresh(index)
    }

    constructor(
        parcelNative: Long,
        index: Int,
        accountId: Long,
        savedInstanceState: Bundle?
    ) : super(ArrayList<Photo>(0), accountId, false, savedInstanceState) {
        mUpdated = BooleanArray(1)
        refreshing = BooleanArray(1)
        currentIndex = index
        loadDataFromParcelNative(parcelNative)
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
                    PersistentLogger.logThrowable("PhotoAlbumPagerPresenter", it)
                })
    }

    private fun onInitialLoadingFinished(photos: List<Photo>) {
        mUpdated = BooleanArray(photos.size)
        refreshing = BooleanArray(photos.size)

        changeLoadingNowState(false)
        mPhotos.addAll(photos)
        refreshPagerView()
        resolveButtonsBarVisible()
        resolveToolbarVisibility()
        refreshInfoViews(true)
        refresh(currentIndex)
    }

    override fun close() {
        view?.returnOnlyPos(currentIndex)
    }

    private fun refresh(index: Int) {
        if (mUpdated[index] || refreshing[index]) {
            return
        }
        refreshing[index] = true
        val photo = mPhotos[index]
        val forUpdate =
            listOf(AccessIdPairModel(photo.getObjectId(), photo.ownerId, photo.accessKey))
        appendJob(
            photosInteractor.getPhotosByIds(accountId, forUpdate)
                .fromIOToMain({ photos ->
                    onPhotoUpdateReceived(
                        photos,
                        index
                    )
                }) { t -> onRefreshFailed(index, t) })
    }

    private fun onRefreshFailed(index: Int, t: Throwable) {
        refreshing[index] = false
        view?.let {
            showError(
                it,
                Utils.getCauseIfRuntime(t)
            )
        }
    }

    private fun onPhotoUpdateReceived(result: List<Photo>, index: Int) {
        refreshing[index] = false
        if (result.size == 1) {
            val p = result[0]
            mPhotos[index] = p
            mUpdated[index] = true
            if (currentIndex == index) {
                refreshInfoViews(true)
            }
        }
    }

    override fun afterPageChangedFromUi(oldPage: Int, newPage: Int) {
        super.afterPageChangedFromUi(oldPage, newPage)
        refresh(newPage)
    }
}