package dev.ragnarok.fenrir.fragment.vkphotos

import android.os.Bundle
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.db.serialize.Serializers
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IPhotosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.model.wrappers.SelectablePhotoWrapper
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.IUploadManager
import dev.ragnarok.fenrir.upload.IUploadManager.IProgressUpdate
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadDestination
import dev.ragnarok.fenrir.upload.UploadDestination.Companion.forPhotoAlbum
import dev.ragnarok.fenrir.upload.UploadIntent
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils.createIntents
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.getSelected
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.disposables.CompositeDisposable

class VKPhotosPresenter(
    accountId: Long,
    private val ownerId: Long,
    private val albumId: Int,
    private val action: String,
    owner: Owner?,
    album: PhotoAlbum?,
    private val loadedIdPhoto: Int,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IVKPhotosView>(accountId, savedInstanceState) {
    private val interactor: IPhotosInteractor
    private val ownersRepository: IOwnersRepository
    private val uploadManager: IUploadManager
    private val photos: MutableList<SelectablePhotoWrapper>
    private val uploads: MutableList<Upload>
    private val destination: UploadDestination
    private val cacheDisposable = CompositeDisposable()
    private var album: PhotoAlbum? = null
    private var owner: Owner? = null
    private var requestNow = false
    private var endOfContent = false
    var isShowBDate = Settings.get().other().isShow_photos_date
        private set
    private var invertPhotoRev: Boolean
    fun togglePhotoInvert() {
        invertPhotoRev = !invertPhotoRev
        Settings.get().other().isInvertPhotoRev = invertPhotoRev
        fireRefresh()
    }

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putParcelable(SAVE_ALBUM, album)
        outState.putParcelable(SAVE_OWNER, ParcelableOwnerWrapper(owner))
    }

    private fun refreshOwnerInfoIfNeed() {
        if (!isMy && owner == null) {
            appendDisposable(
                ownersRepository.getBaseOwnerInfo(
                    accountId,
                    ownerId,
                    IOwnersRepository.MODE_NET
                )
                    .fromIOToMain()
                    .subscribe({ owner -> onActualOwnerInfoReceived(owner) }, ignore())
            )
        }
    }

    private fun refreshAlbumInfoIfNeed() {
        if (album == null) {
            appendDisposable(
                interactor.getAlbumById(accountId, ownerId, albumId)
                    .fromIOToMain()
                    .subscribe({ album -> onAlbumInfoReceived(album) }, ignore())
            )
        }
    }

    private fun onAlbumInfoReceived(album: PhotoAlbum) {
        this.album = album
        resolveToolbarView()
        if (!isSelectionMode) {
            resolveButtonAddVisibility(true)
        }
    }

    private fun onActualOwnerInfoReceived(owner: Owner) {
        this.owner = owner
        resolveButtonAddVisibility(true)
    }

    private fun resolveToolbarView() {
        val ownerName = if (owner != null) owner?.fullName else null
        view?.displayToolbarSubtitle(album, getString(R.string.photos_count, photos.size))
        if (ownerName.nonNullNoEmpty()) {
            view?.setToolbarTitle(ownerName)
        } else {
            view?.displayDefaultToolbarTitle()
        }
    }

    private fun onUploadQueueAdded(added: List<Upload>) {
        val startUploadSize = uploads.size
        var count = 0
        for (upload in added) {
            if (destination.compareTo(upload.destination)) {
                uploads.add(upload)
                count++
            }
        }
        if (count > 0) {
            val finalCount = count
            view?.notifyUploadAdded(
                startUploadSize,
                finalCount
            )
        }
    }

    private fun onUploadsRemoved(ids: IntArray) {
        for (id in ids) {
            val index = findIndexById(uploads, id)
            if (index != -1) {
                uploads.removeAt(index)
                view?.notifyUploadRemoved(
                    index
                )
            }
        }
    }

    private fun onUploadResults(pair: Pair<Upload, UploadResult<*>>) {
        if (destination.compareTo(pair.first.destination)) {
            val photo = pair.second.result as Photo
            photos.add(0, SelectablePhotoWrapper(photo))
            view?.notifyPhotosAdded(
                0,
                1
            )
        }
    }

    private fun onUploadStatusUpdate(upload: Upload) {
        val index = findIndexById(uploads, upload.getObjectId())
        if (index != -1) {
            view?.notifyUploadItemChanged(
                index
            )
        }
    }

    private fun onUploadProgressUpdate(updates: List<IProgressUpdate>) {
        for (update in updates) {
            val index = findIndexById(uploads, update.id)
            if (index != -1) {
                view?.notifyUploadProgressChanged(
                    update.id,
                    update.progress
                )
            }
        }
    }

    fun doToggleDate() {
        isShowBDate = !isShowBDate
        view?.onToggleShowDate(isShowBDate)
        view?.notifyDataSetChanged()
    }

    override fun onGuiCreated(viewHost: IVKPhotosView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(photos, uploads)
        viewHost.onToggleShowDate(isShowBDate)
        resolveButtonAddVisibility(false)
        resolveToolbarView()
    }

    private fun setRequestNow(requestNow: Boolean) {
        this.requestNow = requestNow
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            requestNow
        )
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        view?.setDrawerPhotosSelected(isMy)
    }

    private fun requestActualData(offset: Int) {
        setRequestNow(true)
        if (albumId != -9001 && albumId != -9000) {
            appendDisposable(interactor[accountId, ownerId, albumId, COUNT, offset, !invertPhotoRev]
                .map { t ->
                    val wrap = wrappersOf(t)
                    MusicPlaybackController.tracksExist.markExistPhotos(wrap)
                    wrap
                }
                .fromIOToMain()
                .subscribe({ photos ->
                    onActualPhotosReceived(
                        offset,
                        photos
                    )
                }) { t -> onActualDataGetError(t) })
        } else if (albumId == -9000) {
            appendDisposable(interactor.getUsersPhoto(
                accountId,
                ownerId,
                1,
                if (invertPhotoRev) 1 else 0,
                offset,
                COUNT
            )
                .map { t ->
                    val wrap = wrappersOf(t)
                    MusicPlaybackController.tracksExist.markExistPhotos(wrap)
                    wrap
                }
                .fromIOToMain()
                .subscribe({ photos ->
                    onActualPhotosReceived(
                        offset,
                        photos
                    )
                }) { t -> onActualDataGetError(t) })
        } else {
            appendDisposable(interactor.getAll(accountId, ownerId, 1, 1, offset, COUNT)
                .map { t ->
                    val wrap = wrappersOf(t)
                    MusicPlaybackController.tracksExist.markExistPhotos(wrap)
                    wrap
                }
                .fromIOToMain()
                .subscribe({ photos ->
                    onActualPhotosReceived(
                        offset,
                        photos
                    )
                }) { t -> onActualDataGetError(t) })
        }
    }

    private fun onActualDataGetError(t: Throwable) {
        showError(getCauseIfRuntime(t))
        setRequestNow(false)
    }

    private fun onActualPhotosReceived(offset: Int, data: List<SelectablePhotoWrapper>) {
        cacheDisposable.clear()
        endOfContent = data.isEmpty()
        setRequestNow(false)
        if (offset == 0) {
            photos.clear()
            photos.addAll(data)
            view?.notifyDataSetChanged()
        } else {
            val startSize = photos.size
            photos.addAll(data)
            view?.notifyPhotosAdded(
                startSize,
                data.size
            )
        }
        if (loadedIdPhoto > 0) {
            var opt = 0
            for (i in photos) {
                if (i.photo.getObjectId() == loadedIdPhoto) {
                    i.current = true
                    view?.notifyPhotosChanged(opt, 1)
                    break
                }
                opt++
            }
        }
        resolveToolbarView()
    }

    private fun loadInitialData() {
        cacheDisposable.add(interactor.getAllCachedData(accountId, ownerId, albumId, invertPhotoRev)
            .zipWith(
                uploadManager[accountId, destination]
            ) { first: List<Photo>, second: List<Upload> ->
                Pair.create(
                    first,
                    second
                )
            }
            .fromIOToMain()
            .subscribe { data -> onInitialDataReceived(data) })
    }

    fun updateInfo(position: Int, ptr: Long) {
        val p = ParcelNative.fromNative(ptr).readParcelableList(Photo.NativeCreator) ?: return
        photos.clear()
        photos.addAll(wrappersOf(p))
        photos[position].current = true
        MusicPlaybackController.tracksExist.markExistPhotos(photos)
        view?.let {
            it.notifyDataSetChanged()
            it.scrollTo(position)
        }
    }

    private fun onInitialDataReceived(data: Pair<List<Photo>, List<Upload>>) {
        photos.clear()
        photos.addAll(wrappersOf(data.first))
        uploads.clear()
        uploads.addAll(data.second)
        view?.notifyDataSetChanged()
        resolveToolbarView()
        requestActualData(0)
    }

    override fun onDestroyed() {
        cacheDisposable.dispose()
        super.onDestroyed()
    }

    fun fireUploadRemoveClick(o: Upload) {
        uploadManager.cancel(o.getObjectId())
    }

    fun fireRefresh() {
        if (!requestNow) {
            requestActualData(0)
        }
    }

    fun fireScrollToEnd() {
        if (!requestNow && photos.nonNullNoEmpty() && !endOfContent) {
            requestActualData(photos.size)
        }
    }

    private val isMy: Boolean
        get() = accountId == ownerId
    private val isAdmin: Boolean
        get() = owner is Community && (owner as Community).adminLevel >= VKApiCommunity.AdminLevel.MODERATOR

    private fun canUploadToAlbum(): Boolean {
        // можно загружать,
        // 1 - альбом не системный ОБЯЗАТЕЛЬНО
        // 2 - если я админ группы
        // 3 - если альбом мой
        // 4 - если альбом принадлежит группе, но разрешено в него грузить
        return albumId >= 0 && (isAdmin || isMy || album?.isCanUpload() == true)
    }

    fun firePhotosForUploadSelected(photos: List<LocalPhoto>, size: Int) {
        val intents: List<UploadIntent> = createIntents(accountId, destination, photos, size, true)
        uploadManager.enqueue(intents)
    }

    fun firePhotoSelectionChanged(wrapper: SelectablePhotoWrapper) {
        wrapper.isSelected = !wrapper.isSelected
        onPhotoSelected(wrapper)
    }

    private fun onPhotoSelected(selectedPhoto: SelectablePhotoWrapper) {
        if (selectedPhoto.isSelected) {
            var targetIndex = 1
            for (photo in photos) {
                if (photo.index >= targetIndex) {
                    targetIndex = photo.index + 1
                }
            }
            selectedPhoto.index = targetIndex
        } else {
            for (i in photos.indices) {
                val photo = photos[i]
                if (photo.index > selectedPhoto.index) {
                    photo.index = photo.index - 1
                }
            }
            selectedPhoto.index = 0
        }
        if (selectedPhoto.isSelected) {
            view?.setButtonAddVisible(
                visible = true,
                anim = true
            )
        } else {
            resolveButtonAddVisibility(true)
        }
    }

    private val isSelectionMode: Boolean
        get() = IVKPhotosView.ACTION_SELECT_PHOTOS == action

    private fun resolveButtonAddVisibility(anim: Boolean) {
        if (isSelectionMode) {
            var hasSelected = false
            for (wrapper in photos) {
                if (wrapper.isSelected) {
                    hasSelected = true
                    break
                }
            }
            val finalHasSelected = hasSelected
            view?.setButtonAddVisible(
                finalHasSelected,
                anim
            )
        } else {
            view?.setButtonAddVisible(
                canUploadToAlbum(),
                anim
            )
        }
    }

    fun firePhotoClick(wrapper: SelectablePhotoWrapper) {
        var Index = 0
        var trig = false
        if (!FenrirNative.isNativeLoaded || !Settings.get().other().isNative_parcel_photo) {
            val photos_ret = ArrayList<Photo>(photos.size)
            for (i in photos.indices) {
                val photo = photos[i]
                photos_ret.add(photo.photo)
                if (!trig && photo.photo.getObjectId() == wrapper.photo.getObjectId() && photo.photo.ownerId == wrapper.photo.ownerId) {
                    Index = i
                    trig = true
                }
            }
            val finalIndex = Index
            val source = TmpSource(instanceId, 0)
            fireTempDataUsage()
            appendDisposable(Stores.instance
                .tempStore()
                .putTemporaryData(
                    source.ownerId,
                    source.sourceId,
                    photos_ret,
                    Serializers.PHOTOS_SERIALIZER
                )
                .fromIOToMain()
                .subscribe({
                    view?.displayGallery(
                        accountId,
                        albumId,
                        ownerId,
                        source,
                        finalIndex
                    )
                }) { obj -> obj.printStackTrace() })
        } else {
            val mem = ParcelNative.create(ParcelFlags.NULL_LIST)
            mem.writeInt(photos.size)
            for (i in photos.indices) {
                val photo = photos[i]
                mem.writeParcelable(photo.photo)
                if (!trig && photo.photo.getObjectId() == wrapper.photo.getObjectId() && photo.photo.ownerId == wrapper.photo.ownerId) {
                    Index = i
                    trig = true
                }
            }
            val finalIndex = Index
            view?.displayGalleryUnSafe(
                accountId,
                albumId,
                ownerId,
                mem.nativePointer,
                finalIndex
            )
        }
    }

    fun fireSelectionCommitClick() {
        val selected = selected
        if (selected.nonNullNoEmpty()) {
            view?.returnSelectionToParent(
                selected
            )
        } else {
            view?.showSelectPhotosToast()
        }
    }

    private val selectedWrappers: List<SelectablePhotoWrapper>
        get() {
            val result: MutableList<SelectablePhotoWrapper> = getSelected(photos)
            result.sort()
            return result
        }
    private val selected: List<Photo>
        get() {
            val wrappers = selectedWrappers
            val photos: MutableList<Photo> = ArrayList(wrappers.size)
            for (wrapper in wrappers) {
                photos.add(wrapper.photo)
            }
            return photos
        }

    fun fireAddPhotosClick() {
        if (canUploadToAlbum()) {
            view?.startLocalPhotosSelection()
        }
    }

    fun fireReadStoragePermissionChanged() {
        view?.startLocalPhotosSelectionIfHasPermission()
    }

    fun loadDownload() {
        isShowBDate = true
        setRequestNow(true)
        appendDisposable(
            MusicPlaybackController.tracksExist.findLocalImages(photos)
                .fromIOToMain()
                .subscribe({ onCacheLoaded() }, ignore())
        )
    }

    private fun onCacheLoaded() {
        view?.onToggleShowDate(
            isShowBDate
        )
        view?.notifyDataSetChanged()
        setRequestNow(false)
    }

    companion object {
        private const val SAVE_ALBUM = "save-album"
        private const val SAVE_OWNER = "save-owner"
        private const val COUNT = 100
        internal fun wrappersOf(photos: List<Photo>): List<SelectablePhotoWrapper> {
            val wrappers: MutableList<SelectablePhotoWrapper> = ArrayList(photos.size)
            for (photo in photos) {
                wrappers.add(SelectablePhotoWrapper(photo))
            }
            return wrappers
        }
    }

    init {
        invertPhotoRev = Settings.get().other().isInvertPhotoRev
        interactor = InteractorFactory.createPhotosInteractor()
        ownersRepository = owners
        uploadManager = Includes.uploadManager
        destination = forPhotoAlbum(albumId, ownerId)
        photos = ArrayList()
        uploads = ArrayList()
        if (savedInstanceState == null) {
            this.album = album
            this.owner = owner
        } else {
            this.album = savedInstanceState.getParcelableCompat(SAVE_ALBUM)
            val ownerWrapper: ParcelableOwnerWrapper? = savedInstanceState.getParcelableCompat(
                SAVE_OWNER
            )
            this.owner = ownerWrapper?.get()
        }
        loadInitialData()
        appendDisposable(uploadManager.observeAdding()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadQueueAdded(it) })
        appendDisposable(uploadManager.observeDeleting(true)
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadsRemoved(it) })
        appendDisposable(uploadManager.observeResults()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadResults(it) })
        appendDisposable(uploadManager.obseveStatus()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadStatusUpdate(it) })
        appendDisposable(uploadManager.observeProgress()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadProgressUpdate(it) })
        refreshOwnerInfoIfNeed()
        refreshAlbumInfoIfNeed()
    }
}
