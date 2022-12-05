package dev.ragnarok.fenrir.activity.photopager

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.db.serialize.Serializers
import dev.ragnarok.fenrir.domain.ILocalServerInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.PersistentLogger
import dev.ragnarok.fenrir.util.Utils

class PhotoAlbumPagerPresenter : PhotoPagerPresenter {
    private val localServerInteractor: ILocalServerInteractor
    private val mOwnerId: Int
    private val mAlbumId: Int
    private val invertPhotoRev: Boolean
    private var canLoad: Boolean

    constructor(
        index: Int,
        accountId: Int,
        ownerId: Int,
        albumId: Int,
        photos: ArrayList<Photo>,
        readOnly: Boolean,
        invertPhotoRev: Boolean,
        context: Context,
        savedInstanceState: Bundle?
    ) : super(ArrayList<Photo>(0), accountId, readOnly, context, savedInstanceState) {
        localServerInteractor = InteractorFactory.createLocalServerInteractor()
        mOwnerId = ownerId
        mAlbumId = albumId
        canLoad = true
        this.invertPhotoRev = invertPhotoRev
        mPhotos.addAll(photos)
        currentIndex = index
        refreshPagerView()
        resolveButtonsBarVisible()
        resolveToolbarVisibility()
        refreshInfoViews(true)
    }

    constructor(
        index: Int,
        accountId: Int,
        ownerId: Int,
        albumId: Int,
        source: TmpSource,
        readOnly: Boolean,
        invertPhotoRev: Boolean,
        context: Context,
        savedInstanceState: Bundle?
    ) : super(ArrayList<Photo>(0), accountId, readOnly, context, savedInstanceState) {
        localServerInteractor = InteractorFactory.createLocalServerInteractor()
        mOwnerId = ownerId
        mAlbumId = albumId
        canLoad = true
        this.invertPhotoRev = invertPhotoRev
        currentIndex = index
        loadDataFromDatabase(source)
    }

    private fun loadDataFromDatabase(source: TmpSource) {
        changeLoadingNowState(true)
        appendDisposable(Stores.instance
            .tempStore()
            .getTemporaryData(source.ownerId, source.sourceId, Serializers.PHOTOS_SERIALIZER)
            .fromIOToMain()
            .subscribe({ onInitialLoadingFinished(it) }) {
                PersistentLogger.logThrowable("PhotoAlbumPagerPresenter", it)
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

    override fun need_update_info(): Boolean {
        return true
    }

    private fun loadData() {
        if (!canLoad) return
        changeLoadingNowState(true)
        if (mAlbumId != -9001 && mAlbumId != -9000 && mAlbumId != -311) {
            appendDisposable(photosInteractor[accountId, mOwnerId, mAlbumId, COUNT_PER_LOAD, mPhotos.size, !invertPhotoRev]
                .fromIOToMain()
                .subscribe({ onActualPhotosReceived(it) }) { t ->
                    onActualDataGetError(
                        t
                    )
                })
        } else if (mAlbumId == -9000) {
            appendDisposable(photosInteractor.getUsersPhoto(
                accountId,
                mOwnerId,
                1,
                if (invertPhotoRev) 1 else 0,
                mPhotos.size,
                COUNT_PER_LOAD
            )
                .fromIOToMain()
                .subscribe({ onActualPhotosReceived(it) }) { t ->
                    onActualDataGetError(
                        t
                    )
                })
        } else if (mAlbumId == -9001) {
            appendDisposable(photosInteractor.getAll(
                accountId,
                mOwnerId,
                1,
                1,
                mPhotos.size,
                COUNT_PER_LOAD
            )
                .fromIOToMain()
                .subscribe({ onActualPhotosReceived(it) }) {
                    onActualDataGetError(
                        it
                    )
                })
        } else {
            appendDisposable(localServerInteractor.getPhotos(
                mPhotos.size,
                COUNT_PER_LOAD,
                invertPhotoRev
            )
                .fromIOToMain()
                .subscribe({ onActualPhotosReceived(it) }) {
                    onActualDataGetError(
                        it
                    )
                })
        }
    }

    private fun onActualDataGetError(t: Throwable) {
        view?.let {
            showError(
                it,
                Utils.getCauseIfRuntime(t)
            )
        }
    }

    override fun close() {
        if (Settings.get().other().isNative_parcel_photo && FenrirNative.isNativeLoaded) {
            val ptr = ParcelNative.createParcelableList(mPhotos, ParcelFlags.NULL_LIST)
            view?.returnInfo(
                currentIndex,
                ptr
            )
        } else {
            view?.closeOnly()
        }
    }

    private fun onActualPhotosReceived(data: List<Photo>) {
        changeLoadingNowState(false)
        if (data.isEmpty()) {
            canLoad = false
            return
        }
        mPhotos.addAll(data)
        refreshPagerView()
        resolveButtonsBarVisible()
        resolveToolbarVisibility()
        refreshInfoViews(true)
    }

    override fun afterPageChangedFromUi(oldPage: Int, newPage: Int) {
        super.afterPageChangedFromUi(oldPage, newPage)
        if (oldPage == newPage) return
        if (newPage == count() - 1) {
            loadData()
        }
    }

    companion object {
        private const val COUNT_PER_LOAD = 100
    }
}