package dev.ragnarok.fenrir.fragment.photos.localimagealbums

import android.os.Bundle
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.util.AppPerms.hasReadStoragePermission
import dev.ragnarok.fenrir.util.Objects.safeEquals
import dev.ragnarok.fenrir.util.PersistentLogger
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import java.util.Locale

class LocalPhotoAlbumsPresenter(savedInstanceState: Bundle?) :
    RxSupportPresenter<ILocalPhotoAlbumsView>(savedInstanceState) {
    private val mLocalImageAlbums: MutableList<LocalImageAlbum> = ArrayList()
    private val mLocalImageAlbums_Search: MutableList<LocalImageAlbum> = ArrayList()
    private var permissionRequestedOnce = false
    private var mLoadingNow = false
    private var q: String? = null
    fun fireSearchRequestChanged(q: String?, force: Boolean) {
        val query = q?.trim()
        if (!force && safeEquals(query, this.q)) {
            return
        }
        this.q = query
        mLocalImageAlbums_Search.clear()
        if (!this.q.isNullOrEmpty()) {
            for (i in mLocalImageAlbums) {
                if (i.name.isNullOrEmpty()) {
                    continue
                }
                if (this.q?.lowercase(Locale.getDefault())
                        ?.let { i.name?.lowercase(Locale.getDefault())?.contains(it) } == true
                ) {
                    mLocalImageAlbums_Search.add(i)
                }
            }
        }
        if (!this.q.isNullOrEmpty()) view?.displayData(
            mLocalImageAlbums_Search
        )
        else view?.displayData(
            mLocalImageAlbums
        )
    }

    override fun onGuiCreated(viewHost: ILocalPhotoAlbumsView) {
        super.onGuiCreated(viewHost)
        if (!hasReadStoragePermission(applicationContext)) {
            if (!permissionRequestedOnce) {
                permissionRequestedOnce = true
                view?.requestReadExternalStoragePermission()
            }
        } else {
            if (mLocalImageAlbums.isEmpty()) {
                loadData()
                view?.displayData(
                    mLocalImageAlbums
                )
            } else {
                if (q.isNullOrEmpty()) {
                    view?.displayData(
                        mLocalImageAlbums
                    )
                } else {
                    view?.displayData(
                        mLocalImageAlbums_Search
                    )
                }
            }
        }
        resolveProgressView()
        resolveEmptyTextView()
    }

    private fun changeLoadingNowState(loading: Boolean) {
        mLoadingNow = loading
        resolveProgressView()
    }

    private fun resolveProgressView() {
        view?.displayProgress(
            mLoadingNow
        )
    }

    private fun loadData() {
        if (mLoadingNow) return
        changeLoadingNowState(true)
        appendJob(
            Stores.instance
                .localMedia()
                .imageAlbums
                .fromIOToMain({ onDataLoaded(it) }) { throwable ->
                    onLoadError(
                        throwable
                    )
                })
    }

    private fun onLoadError(throwable: Throwable) {
        PersistentLogger.logThrowable("LocalPhotoAlbumsPresenter", throwable)
        changeLoadingNowState(false)
    }

    private fun onDataLoaded(data: List<LocalImageAlbum>) {
        changeLoadingNowState(false)
        mLocalImageAlbums.clear()
        mLocalImageAlbums.addAll(data)
        view?.notifyDataChanged()
        resolveEmptyTextView()
        if (!q.isNullOrEmpty()) {
            fireSearchRequestChanged(q, true)
        }
    }

    private fun resolveEmptyTextView() {
        view?.setEmptyTextVisible(
            mLocalImageAlbums.isEmpty()
        )
    }

    fun fireRefresh() {
        loadData()
    }

    fun fireAlbumClick(album: LocalImageAlbum) {
        view?.openAlbum(album)
    }

    fun fireReadExternalStoregePermissionResolved() {
        if (hasReadStoragePermission(applicationContext)) {
            loadData()
        }
    }
}