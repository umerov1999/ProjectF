package dev.ragnarok.fenrir.activity.photopager

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso3.BitmapTarget
import com.squareup.picasso3.Picasso.LoadedFrom
import dev.ragnarok.fenrir.App.Companion.instance
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.qr.CameraScanActivity
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IPhotosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.model.AccessIdPair
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.FunctionSource
import dev.ragnarok.fenrir.model.IOwnersBundle
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.AssertUtils
import dev.ragnarok.fenrir.util.DownloadWorkUtils.doDownloadPhoto
import dev.ragnarok.fenrir.util.DownloadWorkUtils.fixStart
import dev.ragnarok.fenrir.util.DownloadWorkUtils.makeLegalFilename
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import io.reactivex.rxjava3.core.Completable
import java.io.File
import java.util.Calendar
import kotlin.math.abs

open class PhotoPagerPresenter internal constructor(
    protected var mPhotos: ArrayList<Photo>,
    accountId: Long,
    private val read_only: Boolean,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IPhotoPagerView>(accountId, savedInstanceState) {
    protected val photosInteractor: IPhotosInteractor = InteractorFactory.createPhotosInteractor()
    protected var currentIndex = 0
    private var mLoadingNow = false
    private var mFullScreen = false
    open fun close() {
        view?.closeOnly()
    }

    fun changeLoadingNowState(loading: Boolean) {
        mLoadingNow = loading
        resolveLoadingView()
    }

    private fun resolveLoadingView() {
        view?.displayPhotoListLoading(
            mLoadingNow
        )
    }

    fun refreshPagerView() {
        view?.displayPhotos(
            mPhotos,
            currentIndex
        )
    }

    override fun onViewHostAttached(view: IPhotoPagerView) {
        super.onViewHostAttached(view)
        resolveOptionMenu(view)
    }

    private fun resolveOptionMenu(view: IPhotoPagerView) {
        view.setupOptionMenu(
            canSaveYourself(),
            canDelete()
        )
    }

    private fun canDelete(): Boolean {
        return hasPhotos() && current.ownerId == accountId
    }

    private fun canSaveYourself(): Boolean {
        return hasPhotos() && current.ownerId != accountId
    }

    override fun onGuiCreated(viewHost: IPhotoPagerView) {
        super.onGuiCreated(viewHost)
        view?.displayPhotos(
            mPhotos,
            currentIndex
        )
        refreshInfoViews(true)
        resolveRestoreButtonVisibility()
        resolveToolbarVisibility()
        resolveButtonsBarVisible()
        resolveLoadingView()
    }

    fun firePageSelected(position: Int) {
        val old = currentIndex
        changePageTo(position)
        afterPageChangedFromUi(old, position)
    }

    protected open fun afterPageChangedFromUi(oldPage: Int, newPage: Int) {}
    private fun changePageTo(position: Int) {
        if (currentIndex == position) return
        currentIndex = position
        onPositionChanged()
    }

    private fun resolveLikeView() {
        if (hasPhotos()) {
            if (read_only) {
                view?.setupLikeButton(
                    visible = false,
                    like = false,
                    likes = 0
                )
                return
            }
            val photo = current
            view?.setupLikeButton(
                true,
                photo.isUserLikes,
                photo.likesCount
            )
        }
    }

    private fun resolveWithUserView() {
        if (hasPhotos()) {
            val photo = current
            view?.setupWithUserButton(photo.tagsCount)
        }
    }

    private fun resolveShareView() {
        if (hasPhotos()) {
            val photo = current
            view?.setupShareButton(!read_only, photo.repostsCount)
        }
    }

    private fun resolveCommentsView() {
        if (hasPhotos()) {
            val photo = current
            if (read_only) {
                view?.setupCommentsButton(
                    false,
                    0
                )
                return
            }
            //boolean visible = photo.isCanComment() || photo.getCommentsCount() > 0;
            view?.setupCommentsButton(
                true,
                photo.commentsCount
            )
        }
    }

    fun count(): Int {
        return mPhotos.size
    }

    private fun resolveToolbarTitleSubtitleView() {
        if (!hasPhotos()) return
        view?.setToolbarTitle(currentIndex + 1, count())
        view?.setToolbarSubtitle(current.text)
    }

    private val current: Photo
        get() = mPhotos[currentIndex]

    private fun onPositionChanged() {
        refreshInfoViews(true)
        resolveRestoreButtonVisibility()
        view?.let { resolveOptionMenu(it) }
    }

    private fun showPhotoInfo(
        context: Context,
        photo: Photo,
        album: PhotoAlbum?,
        bundle: IOwnersBundle?
    ) {
        if (photo.albumId == -311) {
            return
        }
        val album_info =
            if (album == null) context.getString(R.string.open_photo_album) else album.getDisplayTitle(
                context
            )
        var user: String? =
            if (photo.ownerId >= 0) context.getString(R.string.goto_user) else context.getString(R.string.goto_community)
        if (bundle != null) {
            user = bundle.getById(photo.ownerId).fullName
        }
        val buttons: MutableList<FunctionSource> = ArrayList(2)
        buttons.add(FunctionSource(
            album_info, R.drawable.photo_album
        ) {
            PlaceFactory.getVKPhotosAlbumPlace(
                accountId,
                photo.ownerId,
                photo.albumId,
                null,
                photo.getObjectId()
            )
                .tryOpenWith(context)
        })
        buttons.add(FunctionSource(
            user, R.drawable.person
        ) {
            PlaceFactory.getOwnerWallPlace(accountId, photo.ownerId, null).tryOpenWith(context)
        })
        val adapter = ButtonAdapter(context, buttons)
        MaterialAlertDialogBuilder(context)
            .setTitle(
                context.getString(R.string.uploaded) + " " + AppTextUtils.getDateFromUnixTime(
                    photo.date
                )
            )
            .setView(
                Utils.createAlertRecycleFrame(
                    context,
                    adapter,
                    if (photo.text.isNullOrEmpty()) null else context.getString(R.string.description_hint) + ": " + photo.text,
                    accountId
                )
            )
            .setPositiveButton(R.string.button_ok, null)
            .setCancelable(true)
            .show()
    }

    private fun getOwnerForPhoto(context: Context, photo: Photo, album: PhotoAlbum?) {
        appendDisposable(
            owners.findBaseOwnersDataAsBundle(
                accountId, setOf(photo.ownerId), IOwnersRepository.MODE_ANY
            )
                .fromIOToMain()
                .subscribe({
                    showPhotoInfo(
                        context,
                        photo,
                        album,
                        it
                    )
                }) { showPhotoInfo(context, photo, album, null) })
    }

    fun fireInfoButtonClick(context: Context) {
        val photo = current
        appendDisposable(photosInteractor.getAlbumById(accountId, photo.ownerId, photo.albumId)
            .fromIOToMain()
            .subscribe({
                getOwnerForPhoto(
                    context,
                    photo,
                    it
                )
            }) { getOwnerForPhoto(context, photo, null) })
    }

    fun fireShareButtonClick() {
        val current = current
        view?.sharePhoto(
            accountId,
            current
        )
    }

    fun firePostToMyWallClick() {
        val photo = current
        view?.postToMyWall(
            photo,
            accountId
        )
    }

    fun refreshInfoViews(need_update: Boolean) {
        resolveToolbarTitleSubtitleView()
        resolveLikeView()
        resolveWithUserView()
        resolveShareView()
        resolveCommentsView()
        view?.let { resolveOptionMenu(it) }
        if (need_update && need_update_info() && hasPhotos()) {
            val photo = current
            if (photo.albumId != -311) {
                appendDisposable(photosInteractor.getPhotosByIds(
                    accountId,
                    setOf(AccessIdPair(photo.getObjectId(), photo.ownerId, photo.accessKey))
                )
                    .fromIOToMain()
                    .subscribe({
                        if (it[0].getObjectId() == photo.getObjectId()) {
                            val ne = it[0]
                            if (ne.accessKey == null) {
                                ne.setAccessKey(photo.accessKey)
                            }
                            val old = mPhotos[currentIndex]
                            mPhotos[currentIndex] = ne
                            mPhotos[currentIndex].let { uit ->
                                uit.setShowPhotoTags(old.showPhotoTags)
                                uit.setPhotoTags(old.photoTags)
                                uit.setMsgId(old.msgId)
                                uit.setDeleted(old.isDeleted)
                                uit.setMsgPeerId(old.msgPeerId)
                            }
                            refreshInfoViews(false)
                        }
                    }) { })
            }
        }
    }

    protected open fun need_update_info(): Boolean {
        return false
    }

    fun fireLikeClick() {
        addOrRemoveLike()
    }

    private fun addOrRemoveLike() {
        if (Settings.get().other().isDisable_likes || Utils.isHiddenAccount(
                accountId
            )
        ) {
            return
        }
        val photo = current
        val ownerId = photo.ownerId
        val photoId = photo.getObjectId()
        val add = !photo.isUserLikes
        appendDisposable(photosInteractor.like(accountId, ownerId, photoId, add, photo.accessKey)
            .fromIOToMain()
            .subscribe({
                interceptLike(
                    ownerId,
                    photoId,
                    it,
                    add
                )
            }) { t ->
                view?.let {
                    showError(it, Utils.getCauseIfRuntime(t))
                }
            })
    }

    private fun onDeleteOrRestoreResult(photoId: Int, ownerId: Long, deleted: Boolean) {
        val index = Utils.findIndexById(mPhotos, photoId, ownerId)
        if (index != -1) {
            val photo = mPhotos[index]
            photo.setDeleted(deleted)
            if (currentIndex == index) {
                resolveRestoreButtonVisibility()
            }
        }
    }

    private fun interceptLike(ownerId: Long, photoId: Int, count: Int, userLikes: Boolean) {
        for (photo in mPhotos) {
            if (photo.getObjectId() == photoId && photo.ownerId == ownerId) {
                photo.setLikesCount(count)
                photo.setUserLikes(userLikes)
                resolveLikeView()
                break
            }
        }
    }

    fun fireSaveOnDriveClick(context: Context) {
        if (!AppPerms.hasReadWriteStoragePermission(instance)) {
            view?.requestWriteToExternalStoragePermission()
            return
        }
        doSaveOnDrive(context)
    }

    private fun doSaveOnDrive(context: Context) {
        val dir = File(Settings.get().other().photoDir)
        if (!dir.isDirectory) {
            val created = dir.mkdirs()
            if (!created) {
                view?.showError("Can't create directory $dir")
                return
            }
        } else dir.setLastModified(Calendar.getInstance().time.time)
        val photo = current
        if (photo.albumId == -311) {
            var path = photo.text
            val ndx = path?.indexOf('/') ?: -1
            if (ndx != -1) {
                path = path?.substring(0, ndx)
            }
            downloadResult(context, fixStart(path), dir, photo)
        } else {
            appendDisposable(OwnerInfo.getRx(context, accountId, photo.ownerId)
                .fromIOToMain()
                .subscribe({
                    downloadResult(
                        context,
                        makeLegalFilename(
                            fixStart(it.owner.fullName) ?: ("id" + photo.ownerId),
                            null
                        ), dir, photo
                    )
                }) { downloadResult(context, null, dir, photo) })
        }
    }

    private fun transform_owner(owner_id: Long): String {
        return if (owner_id < 0) "club" + abs(owner_id) else "id$owner_id"
    }

    private fun downloadResult(context: Context, Prefix: String?, dirL: File, photo: Photo) {
        var dir = dirL
        if (Prefix != null && Settings.get().other().isPhoto_to_user_dir) {
            val dir_final = File(dir.absolutePath + "/" + Prefix)
            if (!dir_final.isDirectory) {
                val created = dir_final.mkdirs()
                if (!created) {
                    view?.showError("Can't create directory $dir_final")
                    return
                }
            } else dir_final.setLastModified(Calendar.getInstance().time.time)
            dir = dir_final
        }
        val url = photo.getUrlForSize(PhotoSize.W, true)
        if (url != null) {
            doDownloadPhoto(
                context,
                url,
                dir.absolutePath,
                (if (Prefix != null) Prefix + "_" else "") + transform_owner(photo.ownerId) + "_" + photo.getObjectId()
            )
        }
    }

    fun fireSaveYourselfClick() {
        val photo = current
        if (photo.albumId == -311) {
            return
        }
        appendDisposable(photosInteractor.copy(
            accountId,
            photo.ownerId,
            photo.getObjectId(),
            photo.accessKey
        )
            .fromIOToMain()
            .subscribe({ onPhotoCopied() }) { t ->
                view?.let {
                    showError(
                        it,
                        Utils.getCauseIfRuntime(t)
                    )
                }
            })
    }

    fun fireDetectQRClick(context: Activity) {
        with().load(current.getUrlForSize(PhotoSize.W, false))
            .into(object : BitmapTarget {
                override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
                    val data = CameraScanActivity.decodeFromBitmap(bitmap)
                    MaterialAlertDialogBuilder(context)
                        .setIcon(R.drawable.qr_code)
                        .setMessage(data)
                        .setTitle(getString(R.string.scan_qr))
                        .setPositiveButton(R.string.open) { _: DialogInterface?, _: Int ->
                            LinkHelper.openUrl(
                                context,
                                accountId,
                                data, false
                            )
                        }
                        .setNeutralButton(R.string.copy_text) { _: DialogInterface?, _: Int ->
                            val clipboard = context.getSystemService(
                                Context.CLIPBOARD_SERVICE
                            ) as ClipboardManager?
                            val clip = ClipData.newPlainText("response", data)
                            clipboard?.setPrimaryClip(clip)
                            createCustomToast(context).showToast(R.string.copied_to_clipboard)
                        }
                        .setCancelable(true)
                        .show()
                }

                override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
                    createCustomToast(context).showToastError(e.localizedMessage)
                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
            })
    }

    private fun onPhotoCopied() {
        view?.customToast?.showToastSuccessBottom(
            R.string.photo_saved_yourself
        )
    }

    fun fireDeleteClick() {
        delete()
    }

    fun fireWriteExternalStoragePermissionResolved(context: Context) {
        if (AppPerms.hasReadWriteStoragePermission(instance)) {
            doSaveOnDrive(context)
        }
    }

    fun fireButtonRestoreClick() {
        restore()
    }

    private fun resolveRestoreButtonVisibility() {
        view?.setButtonRestoreVisible(
            hasPhotos() && current.isDeleted
        )
    }

    private fun restore() {
        deleteOrRestore(false)
    }

    private fun deleteOrRestore(detele: Boolean) {
        val photo = current
        if (photo.albumId == -311) {
            return
        }
        val photoId = photo.getObjectId()
        val ownerId = photo.ownerId
        val completable: Completable = if (detele) {
            photosInteractor.deletePhoto(accountId, ownerId, photoId)
        } else {
            photosInteractor.restorePhoto(accountId, ownerId, photoId)
        }
        appendDisposable(completable.fromIOToMain()
            .subscribe({ onDeleteOrRestoreResult(photoId, ownerId, detele) }) { t ->
                view?.let {
                    showError(
                        it,
                        Utils.getCauseIfRuntime(t)
                    )
                }
            })
    }

    private fun delete() {
        deleteOrRestore(true)
    }

    fun fireCommentsButtonClick() {
        val photo = current
        view?.goToComments(
            accountId,
            Commented.from(photo)
        )
    }

    private fun showWithUserDialog(context: Context, photo: Photo) {
        photo.setShowPhotoTags(true)
        view?.rebindPhotoAtPartial(currentIndex)
        val buttons: MutableList<FunctionSource> = ArrayList(photo.photoTags?.size.orZero())
        for (i in photo.photoTags.orEmpty()) {
            if (i.getUserId() != 0L) {
                buttons.add(FunctionSource(i.getTaggedName(), R.drawable.person) {
                    PlaceFactory.getOwnerWallPlace(
                        accountId, i.getUserId(), null
                    ).tryOpenWith(context)
                })
            } else {
                buttons.add(FunctionSource(i.getTaggedName(), R.drawable.pencil) {})
            }
        }
        val adapter = ButtonAdapter(context, buttons)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.has_tags)
            .setPositiveButton(R.string.button_ok, null)
            .setCancelable(true)
            .setView(
                Utils.createAlertRecycleFrame(
                    context,
                    adapter,
                    null,
                    accountId
                )
            )
            .show()
    }

    fun fireWithUserClick() {
        val photo = current
        if (photo.showPhotoTags) {
            photo.setShowPhotoTags(false)
            view?.rebindPhotoAtPartial(currentIndex)
            return
        }
        if (photo.photoTags.isNullOrEmpty()) {
            appendDisposable(
                InteractorFactory.createPhotosInteractor()
                    .getTags(accountId, photo.ownerId, photo.getObjectId(), photo.accessKey)
                    .fromIOToMain()
                    .subscribe({
                        photo.setPhotoTags(it)
                        photo.setShowPhotoTags(true)
                        view?.rebindPhotoAtPartial(currentIndex)
                    }) { throwable ->
                        view?.let {
                            showError(
                                it,
                                throwable
                            )
                        }
                    })
        } else {
            photo.setShowPhotoTags(true)
            view?.rebindPhotoAtPartial(currentIndex)
        }
    }

    fun fireWithUserLongClick(context: Context) {
        val photo = current
        if (photo.photoTags.isNullOrEmpty()) {
            appendDisposable(
                InteractorFactory.createPhotosInteractor()
                    .getTags(accountId, photo.ownerId, photo.getObjectId(), photo.accessKey)
                    .fromIOToMain()
                    .subscribe({
                        photo.setPhotoTags(it)
                        showWithUserDialog(context, photo)
                    }) { throwable ->
                        view?.let {
                            showError(
                                it,
                                throwable
                            )
                        }
                    })
        } else {
            showWithUserDialog(context, photo)
        }
    }

    private fun hasPhotos(): Boolean {
        return mPhotos.nonNullNoEmpty()
    }

    fun firePhotoTap() {
        if (!hasPhotos()) return
        mFullScreen = !mFullScreen
        resolveToolbarVisibility()
        resolveButtonsBarVisible()
    }

    fun resolveButtonsBarVisible() {
        view?.setButtonsBarVisible(
            hasPhotos() && !mFullScreen
        )
    }

    fun resolveToolbarVisibility() {
        view?.setToolbarVisible(hasPhotos() && !mFullScreen)
    }

    fun fireLikeLongClick() {
        if (!hasPhotos()) return
        val photo = current
        view?.goToLikesList(
            accountId,
            photo.ownerId,
            photo.getObjectId()
        )
    }

    private class ButtonAdapter(
        private val context: Context,
        private val items: List<FunctionSource>
    ) : RecyclerView.Adapter<ButtonHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonHolder {
            return ButtonHolder(
                LayoutInflater.from(
                    context
                ).inflate(R.layout.item_button, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ButtonHolder, position: Int) {
            val source = items[position]
            holder.button.text = source.getTitle(context)
            holder.button.setIconResource(source.getIcon())
            holder.button.setOnClickListener { source.Do() }
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }

    private class ButtonHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button: MaterialButton = itemView.findViewById(R.id.item_button_function)

    }

    init {
        AssertUtils.requireNonNull(mPhotos, "'mPhotos' not initialized")
    }
}
