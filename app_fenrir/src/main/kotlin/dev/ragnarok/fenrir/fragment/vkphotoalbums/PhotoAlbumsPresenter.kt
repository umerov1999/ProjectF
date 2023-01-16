package dev.ragnarok.fenrir.fragment.vkphotoalbums

import android.annotation.SuppressLint
import android.os.Bundle
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IPhotosInteractor
import dev.ragnarok.fenrir.domain.IUtilsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import io.reactivex.rxjava3.disposables.CompositeDisposable

class PhotoAlbumsPresenter(
    accountId: Long,
    ownerId: Long,
    params: AdditionalParams?,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IPhotoAlbumsView>(accountId, savedInstanceState) {
    private val photosInteractor: IPhotosInteractor = InteractorFactory.createPhotosInteractor()
    private val ownersRepository: IOwnersRepository = owners
    private val utilsInteractor: IUtilsInteractor = InteractorFactory.createUtilsInteractor()
    private val mOwnerId: Long = ownerId
    private val netDisposable = CompositeDisposable()
    private val cacheDisposable = CompositeDisposable()
    private var mOwner: Owner? = null
    private var mAction: String? = null
    private var mData: ArrayList<PhotoAlbum> = ArrayList()
    private var offset = 0
    private var endOfContent = false
    private var netLoadingNow = false
    fun fireScrollToEnd() {
        if (!netLoadingNow && mData.nonNullNoEmpty() && !endOfContent) {
            refreshFromNet()
        }
    }

    override fun onGuiCreated(viewHost: IPhotoAlbumsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mData)
        resolveDrawerPhotoSection()
        resolveProgressView()
        resolveSubtitleView()
        resolveCreateAlbumButtonVisibility()
    }

    private fun resolveDrawerPhotoSection() {
        view?.setDrawerPhotoSectionActive(
            isMy
        )
    }

    private val isMy: Boolean
        get() = mOwnerId == accountId

    private fun loadOwnerInfo() {
        if (isMy) {
            return
        }
        appendDisposable(ownersRepository.getBaseOwnerInfo(
            accountId,
            mOwnerId,
            IOwnersRepository.MODE_ANY
        )
            .fromIOToMain()
            .subscribe({ owner -> onOwnerInfoReceived(owner) }) { t ->
                onOwnerGetError(
                    t
                )
            })
    }

    private fun onOwnerGetError(t: Throwable) {
        showError(getCauseIfRuntime(t))
    }

    private fun onOwnerInfoReceived(owner: Owner) {
        mOwner = owner
        resolveSubtitleView()
        resolveCreateAlbumButtonVisibility()
    }

    private fun refreshFromNet() {
        netLoadingNow = true
        resolveProgressView()
        netDisposable.add(photosInteractor.getActualAlbums(accountId, mOwnerId, 50, offset)
            .fromIOToMain()
            .subscribe({
                onActualAlbumsReceived(
                    it
                )
            }) { t -> onActualAlbumsGetError(t) })
    }

    private fun onActualAlbumsGetError(t: Throwable) {
        netLoadingNow = false
        showError(getCauseIfRuntime(t))
        resolveProgressView()
    }

    private fun onActualAlbumsReceived(albums: List<PhotoAlbum>) {
        // reset cache loading
        cacheDisposable.clear()
        endOfContent = albums.isEmpty() || mData.contains(albums[0]) && albums.size < 50
        netLoadingNow = false
        if (offset == 0) {
            mData.clear()
            mData.addAll(albums)
            view?.notifyDataSetChanged()
        } else {
            val startSize = mData.size
            var size = 0
            for (i in albums) {
                if (mData.contains(i)) {
                    val pos = mData.indexOf(i)
                    mData[pos] = i
                    view?.notifyItemChanged(pos)
                    continue
                }
                size++
                mData.add(i)
            }
            view?.notifyDataAdded(
                startSize,
                size
            )
        }
        offset += 50
        resolveProgressView()
    }

    private fun loadAllFromDb() {
        cacheDisposable.add(photosInteractor.getCachedAlbums(accountId, mOwnerId)
            .fromIOToMain()
            .subscribe({ onCachedDataReceived(it) }) { })
    }

    private fun onCachedDataReceived(albums: List<PhotoAlbum>) {
        mData.clear()
        mData.addAll(albums)
        safeNotifyDatasetChanged()
    }

    override fun onDestroyed() {
        cacheDisposable.dispose()
        netDisposable.dispose()
        super.onDestroyed()
    }

    private fun resolveProgressView() {
        view?.displayLoading(
            netLoadingNow
        )
    }

    private fun safeNotifyDatasetChanged() {
        view?.notifyDataSetChanged()
    }

    private fun resolveSubtitleView() {
        view?.setToolbarSubtitle(if (mOwner == null || isMy) null else mOwner?.fullName)
    }

    private fun doAlbumRemove(album: PhotoAlbum) {
        val albumId = album.getObjectId()
        val ownerId = album.ownerId
        appendDisposable(photosInteractor.removedAlbum(
            accountId,
            album.ownerId,
            album.getObjectId()
        )
            .fromIOToMain()
            .subscribe({ onAlbumRemoved(albumId, ownerId) }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    private fun onAlbumRemoved(albumId: Int, ownerId: Long) {
        val index = findIndexById(mData, albumId, ownerId)
        if (index != -1) {
            view?.notifyItemRemoved(
                index
            )
        }
    }

    fun fireCreateAlbumClick() {
        view?.goToAlbumCreation(
            accountId,
            mOwnerId
        )
    }

    fun fireAlbumClick(album: PhotoAlbum) {
        if (VKPhotoAlbumsFragment.ACTION_SELECT_ALBUM == mAction) {
            view?.doSelection(album)
        } else {
            view?.openAlbum(
                accountId,
                album,
                mOwner,
                mAction
            )
        }
    }

    fun fireAlbumLongClick(album: PhotoAlbum): Boolean {
        if (canDeleteOrEdit(album)) {
            view?.showAlbumContextMenu(
                album
            )
            return true
        }
        return false
    }

    private val isAdmin: Boolean
        get() = mOwner is Community && (mOwner as Community).isAdmin

    private fun canDeleteOrEdit(album: PhotoAlbum): Boolean {
        return !album.isSystem() && (isMy || isAdmin)
    }

    private fun resolveCreateAlbumButtonVisibility() {
        val mustBeVisible = isMy || isAdmin
        view?.setCreateAlbumFabVisible(
            mustBeVisible
        )
    }

    fun fireAllComments() {
        view?.goToPhotoComments(
            accountId,
            mOwnerId
        )
    }

    fun fireRefresh() {
        cacheDisposable.clear()
        netDisposable.clear()
        netLoadingNow = false
        offset = 0
        refreshFromNet()
    }

    fun fireAlbumDeletingConfirmed(album: PhotoAlbum) {
        doAlbumRemove(album)
    }

    fun fireAlbumDeleteClick(album: PhotoAlbum) {
        view?.showDeleteConfirmDialog(album)
    }

    fun fireAlbumEditClick(album: PhotoAlbum) {
        @SuppressLint("UseSparseArrays") val privacies: MutableMap<Int, SimplePrivacy> = HashMap()
        album.getPrivacyView()?.let {
            privacies[0] = it
        }
        album.getPrivacyComment()?.let {
            privacies[1] = it
        }
        appendDisposable(utilsInteractor
            .createFullPrivacies(accountId, privacies)
            .fromIOToMain()
            .subscribe({ full ->
                val editor = PhotoAlbumEditor.create()
                    .setPrivacyView(full[0])
                    .setPrivacyComment(full[1])
                    .setTitle(album.getTitle())
                    .setDescription(album.getDescription())
                    .setCommentsDisabled(album.isCommentsDisabled())
                    .setUploadByAdminsOnly(album.isUploadByAdminsOnly())
                view?.goToAlbumEditing(
                    accountId,
                    album,
                    editor
                )
            }) { obj -> obj.printStackTrace() })
    }

    class AdditionalParams {
        var owner: Owner? = null
        var action: String? = null
        fun setOwner(owner: Owner?): AdditionalParams {
            this.owner = owner
            return this
        }

        fun setAction(action: String?): AdditionalParams {
            this.action = action
            return this
        }
    }

    init {

        //do restore this
        if (params != null) {
            mAction = params.action
        }
        if (mOwner == null && params != null) {
            mOwner = params.owner
        }
        loadAllFromDb()
        offset = 0
        refreshFromNet()
        if (mOwner == null && !isMy) {
            loadOwnerInfo()
        }
    }
}