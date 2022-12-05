package dev.ragnarok.fenrir.fragment.wallattachments.wallphotosattachments

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.db.serialize.Serializers
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import io.reactivex.rxjava3.disposables.CompositeDisposable

class WallPhotosAttachmentsPresenter(
    accountId: Int,
    private val owner_id: Int,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<IWallPhotosAttachmentsView>(accountId, savedInstanceState) {
    private val mPhotos: ArrayList<Photo> = ArrayList()
    private val fInteractor: IWallsRepository = walls
    private val actualDataDisposable = CompositeDisposable()
    private var loaded = 0
    private var actualDataReceived = false
    private var endOfContent = false
    private var actualDataLoading = false
    override fun onGuiCreated(viewHost: IWallPhotosAttachmentsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mPhotos)
        resolveToolbar()
    }

    fun firePhotoClick(position: Int) {
        if (FenrirNative.isNativeLoaded && Settings.get().other().isNative_parcel_photo) {
            view?.goToTempPhotosGallery(
                accountId,
                ParcelNative.createParcelableList(mPhotos, ParcelFlags.NULL_LIST),
                position
            )
        } else {
            val source = TmpSource(instanceId, 0)
            fireTempDataUsage()
            actualDataDisposable.add(Stores.instance
                .tempStore()
                .putTemporaryData(
                    source.ownerId,
                    source.sourceId,
                    mPhotos,
                    Serializers.PHOTOS_SERIALIZER
                )
                .fromIOToMain()
                .subscribe({
                    onPhotosSavedToTmpStore(
                        position,
                        source
                    )
                }) { obj -> obj.printStackTrace() })
        }
    }

    private fun onPhotosSavedToTmpStore(index: Int, source: TmpSource) {
        view?.goToTempPhotosGallery(
            accountId,
            source,
            index
        )
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(fInteractor.getWallNoCache(
            accountId,
            owner_id,
            offset,
            100,
            WallCriteria.MODE_ALL
        )
            .fromIOToMain()
            .subscribe({ data ->
                onActualDataReceived(
                    offset,
                    data
                )
            }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
        resolveRefreshingView()
    }

    private fun update(data: List<Post>) {
        for (i in data) {
            i.attachments?.photos.nonNullNoEmpty {
                mPhotos.addAll(it)
            }
            if (i.hasCopyHierarchy()) i.getCopyHierarchy()?.let { update(it) }
        }
    }

    private fun onActualDataReceived(offset: Int, data: List<Post>) {
        actualDataLoading = false
        endOfContent = data.isEmpty()
        actualDataReceived = true
        if (endOfContent) resumedView?.onSetLoadingStatus(
            2
        )
        if (offset == 0) {
            loaded = data.size
            mPhotos.clear()
            update(data)
            resolveToolbar()
            view?.notifyDataSetChanged()
        } else {
            val startSize = mPhotos.size
            loaded += data.size
            update(data)
            resolveToolbar()
            view?.notifyDataAdded(
                startSize,
                mPhotos.size - startSize
            )
        }
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.showRefreshing(
            actualDataLoading
        )
        if (!endOfContent) resumedView?.onSetLoadingStatus(
            if (actualDataLoading) 1 else 0
        )
    }

    private fun resolveToolbar() {
        view?.let {
            it.toolbarTitle(getString(R.string.attachments_in_wall))
            it.toolbarSubtitle(
                getString(
                    R.string.photos_count,
                    safeCountOf(mPhotos)
                ) + " " + getString(R.string.posts_analized, loaded)
            )
        }
    }

    override fun onDestroyed() {
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

    fun fireScrollToEnd(): Boolean {
        if (!endOfContent && actualDataReceived && !actualDataLoading) {
            loadActualData(loaded)
            return false
        }
        return true
    }

    fun fireRefresh() {
        actualDataDisposable.clear()
        actualDataLoading = false
        loadActualData(0)
    }

    init {
        loadActualData(0)
    }
}