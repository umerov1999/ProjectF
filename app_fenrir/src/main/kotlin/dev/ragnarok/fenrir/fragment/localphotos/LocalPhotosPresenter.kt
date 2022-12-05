package dev.ragnarok.fenrir.fragment.localphotos

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.util.AppPerms.hasReadStoragePermission
import dev.ragnarok.fenrir.util.Utils.countOfSelection
import dev.ragnarok.fenrir.util.Utils.getSelected

class LocalPhotosPresenter(
    private val mLocalImageAlbum: LocalImageAlbum?, private val mSelectionCountMax: Int,
    savedInstanceState: Bundle?
) : RxSupportPresenter<ILocalPhotosView>(savedInstanceState) {
    private var mLocalPhotos: List<LocalPhoto>
    private var mLoadingNow = false
    private fun loadData() {
        if (mLoadingNow) return
        changeLoadingState(true)
        if (mLocalImageAlbum != null) {
            appendDisposable(Stores.instance
                .localMedia()
                .getPhotos(mLocalImageAlbum.getId().toLong())
                .fromIOToMain()
                .subscribe({ onDataLoaded(it) }) {
                    onLoadError()
                })
        } else {
            appendDisposable(Stores.instance
                .localMedia()
                .photos
                .fromIOToMain()
                .subscribe({ onDataLoaded(it) }) {
                    onLoadError()
                })
        }
    }

    private fun onLoadError() {
        changeLoadingState(false)
    }

    private fun onDataLoaded(data: List<LocalPhoto>) {
        changeLoadingState(false)
        mLocalPhotos = data
        resolveListData()
        resolveEmptyTextVisibility()
    }

    override fun onGuiCreated(viewHost: ILocalPhotosView) {
        super.onGuiCreated(viewHost)
        resolveListData()
        resolveProgressView()
        resolveFabVisibility(false)
        resolveEmptyTextVisibility()
    }

    private fun resolveEmptyTextVisibility() {
        view?.setEmptyTextVisible(
            mLocalPhotos
                .isEmpty()
        )
    }

    private fun resolveListData() {
        view?.displayData(mLocalPhotos)
    }

    private fun changeLoadingState(loading: Boolean) {
        mLoadingNow = loading
        resolveProgressView()
    }

    private fun resolveProgressView() {
        view?.displayProgress(mLoadingNow)
    }

    fun fireFabClick() {
        val localPhotos = getSelected(mLocalPhotos)
        if (localPhotos.isNotEmpty()) {
            view?.returnResultToParent(
                localPhotos
            )
        } else {
            view?.showError(R.string.select_attachments)
        }
    }

    fun firePhotoClick(photo: LocalPhoto) {
        photo.setSelected(!photo.isSelected)
        if (mSelectionCountMax == 1 && photo.isSelected) {
            val single = ArrayList<LocalPhoto>(1)
            single.add(photo)
            view?.returnResultToParent(
                single
            )
            return
        }
        onSelectPhoto(photo)
        view?.updateSelectionAndIndexes()
    }

    private fun onSelectPhoto(selectedPhoto: LocalPhoto) {
        if (selectedPhoto.isSelected) {
            var targetIndex = 1
            for (photo in mLocalPhotos) {
                if (photo.getIndex() >= targetIndex) {
                    targetIndex = photo.getIndex() + 1
                }
            }
            selectedPhoto.setIndex(targetIndex)
        } else {
            for (i in mLocalPhotos.indices) {
                val photo = mLocalPhotos[i]
                if (photo.getIndex() > selectedPhoto.getIndex()) {
                    photo.setIndex(photo.getIndex() - 1)
                }
            }
            selectedPhoto.setIndex(0)
        }
        if (selectedPhoto.isSelected) {
            resolveFabVisibility(visible = true, anim = true)
        } else {
            resolveFabVisibility(true)
        }
    }

    private fun resolveFabVisibility(anim: Boolean) {
        resolveFabVisibility(countOfSelection(mLocalPhotos) > 0, anim)
    }

    private fun resolveFabVisibility(visible: Boolean, anim: Boolean) {
        view?.setFabVisible(
            visible,
            anim
        )
    }

    fun fireRefresh() {
        loadData()
    }

    fun fireReadExternalStoregePermissionResolved() {
        if (hasReadStoragePermission(applicationContext)) {
            loadData()
        }
    }

    init {
        mLocalPhotos = emptyList()
        /*
        if(mLocalImageAlbum == null && !AppPerms.hasReadStoragePermission(getApplicationContext())){
            if(!permissionRequestedOnce){
                permissionRequestedOnce = true;
                callView(v -> v.requestReadExternalStoragePermission();
            }
        } else {
            loadData();
        }
         */loadData()
    }
}