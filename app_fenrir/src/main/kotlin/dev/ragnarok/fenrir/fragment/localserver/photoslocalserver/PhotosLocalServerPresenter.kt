package dev.ragnarok.fenrir.fragment.localserver.photoslocalserver

import android.os.Bundle
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.db.serialize.Serializers
import dev.ragnarok.fenrir.domain.ILocalServerInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.FindAt
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import kotlinx.coroutines.flow.flow

class PhotosLocalServerPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IPhotosLocalServerView>(accountId, savedInstanceState) {
    private val photos: MutableList<Photo> = ArrayList()
    private val fInteractor: ILocalServerInteractor =
        InteractorFactory.createLocalServerInteractor()
    private var actualDataDisposable = CancelableJob()
    private var Foffset = 0
    private var actualDataReceived = false
    private var endOfContent = false
    private var actualDataLoading = false
    private var reverse = false
    private var search_at: FindAt
    private var doLoadTabs = false
    fun toggleReverse() {
        reverse = !reverse
        fireRefresh(false)
    }

    override fun onGuiCreated(viewHost: IPhotosLocalServerView) {
        super.onGuiCreated(viewHost)
        viewHost.displayList(photos)
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        appendJob(fInteractor.getPhotos(offset, GET_COUNT, reverse)
            .fromIOToMain({
                onActualDataReceived(
                    offset,
                    it
                )
            }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
        resolveRefreshingView()
    }

    private fun onActualDataReceived(offset: Int, data: List<Photo>) {
        Foffset = offset + GET_COUNT
        actualDataLoading = false
        endOfContent = data.isEmpty()
        actualDataReceived = true
        if (offset == 0) {
            photos.clear()
            photos.addAll(data)
            view?.notifyListChanged()
        } else {
            val startSize = photos.size
            photos.addAll(data)
            view?.notifyDataAdded(
                startSize,
                data.size
            )
        }
        resolveRefreshingView()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
        loadActualData(0)
    }

    private fun resolveRefreshingView() {
        resumedView?.displayLoading(
            actualDataLoading
        )
    }

    override fun onDestroyed() {
        actualDataDisposable.cancel()
        super.onDestroyed()
    }

    fun fireScrollToEnd(): Boolean {
        if (!endOfContent && photos.nonNullNoEmpty() && actualDataReceived && !actualDataLoading) {
            if (search_at.isSearchMode()) {
                search(false)
            } else {
                loadActualData(Foffset)
            }
            return false
        }
        return true
    }

    private fun doSearch() {
        actualDataLoading = true
        resolveRefreshingView()
        appendJob(fInteractor.searchPhotos(
            search_at.getQuery(),
            search_at.getOffset(),
            SEARCH_COUNT,
            reverse
        )
            .fromIOToMain({
                onSearched(
                    FindAt(
                        search_at.getQuery(),
                        search_at.getOffset() + SEARCH_COUNT,
                        it.size < SEARCH_COUNT
                    ), it
                )
            }) { t -> onActualDataGetError(t) })
    }

    private fun onSearched(search_at: FindAt, data: List<Photo>) {
        actualDataLoading = false
        actualDataReceived = true
        endOfContent = search_at.isEnded()
        if (this.search_at.getOffset() == 0) {
            photos.clear()
            photos.addAll(data)
            view?.notifyListChanged()
        } else {
            if (data.nonNullNoEmpty()) {
                val startSize = photos.size
                photos.addAll(data)
                view?.notifyDataAdded(
                    startSize,
                    data.size
                )
            }
        }
        this.search_at = search_at
        resolveRefreshingView()
    }

    private fun search(sleep_search: Boolean) {
        if (actualDataLoading) return
        if (!sleep_search) {
            doSearch()
            return
        }
        actualDataDisposable.cancel()
        actualDataDisposable += delayTaskFlow(WEB_SEARCH_DELAY.toLong())
            .toMain { doSearch() }
    }

    fun fireSearchRequestChanged(q: String?) {
        val query = q?.trim { it <= ' ' }
        if (!search_at.do_compare(query)) {
            actualDataLoading = false
            if (query.isNullOrEmpty()) {
                actualDataDisposable.cancel()
                fireRefresh(false)
            } else {
                fireRefresh(true)
            }
        }
    }

    fun updateInfo(position: Int, ptr: Long) {
        actualDataLoading = true
        resolveRefreshingView()
        appendJob(
            flow {
                ParcelNative.fromNative(ptr).readParcelableList(Photo.NativeCreator)?.let {
                    emit(
                        it
                    )
                }
            }.fromIOToMain({
                actualDataLoading = false
                resolveRefreshingView()
                photos.clear()
                photos.addAll(it)
                view?.scrollTo(
                    position
                )
            }) { it.printStackTrace() })
    }

    fun firePhotoClick(wrapper: Photo) {
        var Index = 0
        var trig = false
        if (!FenrirNative.isNativeLoaded || !Settings.get().main().isNative_parcel_photo) {
            for (i in photos.indices) {
                val photo = photos[i]
                if (!trig && photo.getObjectId() == wrapper.getObjectId() && photo.ownerId == wrapper.ownerId) {
                    Index = i
                    trig = true
                }
            }
            val finalIndex = Index
            val source = TmpSource(fireTempDataUsage(), 0)
            appendJob(Stores.instance
                .tempStore()
                .putTemporaryData(
                    source.ownerId,
                    source.sourceId,
                    photos,
                    Serializers.PHOTOS_SERIALIZER
                )
                .fromIOToMain({
                    view?.displayGallery(
                        accountId,
                        -311,
                        accountId,
                        source,
                        finalIndex,
                        reverse
                    )
                }) { obj -> obj.printStackTrace() })
        } else {
            appendJob(
                flow {
                    val mem = ParcelNative.create(ParcelFlags.NULL_LIST)
                    mem.writeInt(photos.size)
                    for (i in photos.indices) {
                        if (!isActive()) {
                            mem.forceDestroy()
                            return@flow
                        }
                        val photo = photos[i]
                        mem.writeParcelable(photo)
                        if (!trig && photo.getObjectId() == wrapper.getObjectId() && photo.ownerId == wrapper.ownerId) {
                            Index = i
                            trig = true
                        }
                    }
                    if (!isActive()) {
                        mem.forceDestroy()
                    } else {
                        emit(Pair(mem.nativePointer, Index))
                    }
                }.fromIOToMain({
                    view?.displayGalleryUnSafe(
                        accountId,
                        -311,
                        accountId,
                        it.first,
                        it.second,
                        reverse
                    )
                }) { obj -> obj.printStackTrace() })
        }
    }

    fun fireRefresh(sleep_search: Boolean) {
        if (actualDataLoading) {
            return
        }
        if (search_at.isSearchMode()) {
            search_at.reset(false)
            search(sleep_search)
        } else {
            loadActualData(0)
        }
    }

    companion object {
        private const val SEARCH_COUNT = 50
        private const val GET_COUNT = 100
        private const val WEB_SEARCH_DELAY = 1000
    }

    init {
        search_at = FindAt()
    }
}