package dev.ragnarok.filegallery.fragment.localserver.photoslocalserver

import android.os.Bundle
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.filegallery.Includes.networkInterfaces
import dev.ragnarok.filegallery.api.interfaces.ILocalServerApi
import dev.ragnarok.filegallery.fragment.base.RxSupportPresenter
import dev.ragnarok.filegallery.fromIOToMain
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.util.FindAt
import dev.ragnarok.filegallery.util.Utils
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

class PhotosLocalServerPresenter(savedInstanceState: Bundle?) :
    RxSupportPresenter<IPhotosLocalServerView>(savedInstanceState) {
    private val photos: MutableList<Photo>
    private val fInteractor: ILocalServerApi
    private var actualDataDisposable = Disposable.disposed()
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
        appendDisposable(fInteractor.getPhotos(offset, GET_COUNT, reverse)
            .fromIOToMain()
            .subscribe({
                onActualDataReceived(
                    offset,
                    it
                )
            }) { onActualDataGetError(it) })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        view?.let {
            showError(
                it,
                Utils.getCauseIfRuntime(t)
            )
        }
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

    public override fun onGuiResumed() {
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
        view?.displayLoading(
            actualDataLoading
        )
    }

    override fun onDestroyed() {
        actualDataDisposable.dispose()
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
        appendDisposable(fInteractor.searchPhotos(
            search_at.getQuery(),
            search_at.getOffset(),
            SEARCH_COUNT,
            reverse
        )
            .fromIOToMain()
            .subscribe({
                onSearched(
                    FindAt(
                        search_at.getQuery() ?: return@subscribe,
                        search_at.getOffset() + SEARCH_COUNT,
                        it.size < SEARCH_COUNT
                    ), it
                )
            }) { onActualDataGetError(it) })
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
        actualDataDisposable.dispose()
        actualDataDisposable = Single.just(Any())
            .delay(WEB_SEARCH_DELAY.toLong(), TimeUnit.MILLISECONDS)
            .fromIOToMain()
            .subscribe({ doSearch() }) { onActualDataGetError(it) }
    }

    fun fireSearchRequestChanged(q: String?) {
        val query = q?.trim { it <= ' ' }
        if (!search_at.do_compare(query)) {
            actualDataLoading = false
            if (query.isNullOrEmpty()) {
                actualDataDisposable.dispose()
                fireRefresh(false)
            } else {
                fireRefresh(true)
            }
        }
    }

    fun updateInfo(position: Int, ptr: Long) {
        val p = ParcelNative.fromNative(ptr).readParcelableList(Photo.NativeCreator) ?: return
        photos.clear()
        photos.addAll(p)
        view?.scrollTo(
            position
        )
    }

    fun firePhotoClick(wrapper: Photo) {
        var Index = 0
        var trig = false
        val mem = ParcelNative.create(ParcelFlags.NULL_LIST)
        mem.writeInt(photos.size)
        for (i in photos.indices) {
            val photo = photos[i]
            mem.writeParcelable(photo)
            if (!trig && photo.id == wrapper.id && photo.ownerId == wrapper.ownerId) {
                Index = i
                trig = true
            }
        }
        val finalIndex = Index
        view?.displayGalleryUnSafe(
            mem.nativePointer,
            finalIndex,
            reverse
        )
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
        photos = ArrayList()
        fInteractor = networkInterfaces.localServerApi()
        search_at = FindAt()
    }
}