package dev.ragnarok.fenrir.fragment.createphotoalbum

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.VKApiPhotoAlbum
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fragment.vkphotos.IVKPhotosView
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoAlbumEditor
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.place.PlaceFactory.getVKPhotosAlbumPlace
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStepsHost
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStepsHost.PhotoAlbumState
import kotlin.math.abs

class EditPhotoAlbumPresenter : AccountDependencyPresenter<IEditPhotoAlbumView> {
    private val networker: INetworker
    private val editing: Boolean
    private val ownerId: Long
    private val context: Context
    private val editor: PhotoAlbumEditor
    private var album: PhotoAlbum? = null
    private var stepsHost: CreatePhotoAlbumStepsHost? = null

    constructor(
        accountId: Long,
        ownerId: Long,
        context: Context,
        savedInstanceState: Bundle?
    ) : super(accountId, savedInstanceState) {
        networker = networkInterfaces
        this.ownerId = ownerId
        editor = PhotoAlbumEditor.create()
        editing = false
        this.context = context
        init(savedInstanceState)
    }

    constructor(
        accountId: Long,
        album: PhotoAlbum,
        editor: PhotoAlbumEditor,
        context: Context,
        savedInstanceState: Bundle?
    ) : super(accountId, savedInstanceState) {
        networker = networkInterfaces
        this.album = album
        ownerId = album.ownerId
        this.editor = editor
        editing = true
        this.context = context
        init(savedInstanceState)
    }

    private fun init(savedInstanceState: Bundle?) {
        stepsHost = CreatePhotoAlbumStepsHost()
        (stepsHost ?: return).isAdditionalOptionsEnable = ownerId < 0 // только в группе
        (stepsHost ?: return).setPrivacySettingsEnable(ownerId > 0) // только у пользователя
        if (savedInstanceState != null) {
            (stepsHost ?: return).restoreState(savedInstanceState)
        } else {
            (stepsHost ?: return).state = createInitialState()
        }
    }

    override fun onGuiCreated(viewHost: IEditPhotoAlbumView) {
        super.onGuiCreated(viewHost)
        viewHost.attachSteppersHost(stepsHost ?: return)
    }

    private fun createInitialState(): PhotoAlbumState {
        return PhotoAlbumState()
            .setPrivacyComment(editor.getPrivacyComment())
            .setPrivacyView(editor.getPrivacyView())
            .setCommentsDisabled(editor.isCommentsDisabled())
            .setUploadByAdminsOnly(editor.isUploadByAdminsOnly())
            .setDescription(editor.getDescription())
            .setTitle(editor.getTitle())
    }

    fun fireStepNegativeButtonClick(clickAtStep: Int) {
        if (clickAtStep > 0) {
            stepsHost?.currentStep = clickAtStep - 1
            view?.moveSteppers(
                clickAtStep,
                clickAtStep - 1
            )
        } else {
            onBackOnFirstStepClick()
        }
    }

    private fun onBackOnFirstStepClick() {
        view?.goBack()
    }

    fun fireStepPositiveButtonClick(clickAtStep: Int) {
        val last = clickAtStep == (stepsHost?.stepsCount ?: 0) - 1
        if (!last) {
            val targetStep = clickAtStep + 1
            stepsHost?.currentStep = targetStep
            view?.moveSteppers(
                clickAtStep,
                targetStep
            )
        } else {
            view?.hideKeyboard()
            onFinalButtonClick()
        }
    }

    private fun onFinalButtonClick() {
        val api = networker.vkDefault(accountId).photos()
        val title = state()?.title
        val description = state()?.description
        val uploadsByAdminsOnly = state()?.isUploadByAdminsOnly
        val commentsDisabled = state()?.isCommentsDisabled
        if (editing) {
            album?.getObjectId()?.let {
                api.editAlbum(
                    it, title, description, ownerId, null,
                    null, uploadsByAdminsOnly, commentsDisabled
                )
                    .fromIOToMain()
                    .subscribe({ t: Boolean? -> goToEditedAlbum(album, t) }) { l ->
                        showError(
                            getCauseIfRuntime(l)
                        )
                    }
            }?.let { appendDisposable(it) }
        } else {
            val groupId = if (ownerId < 0) abs(ownerId) else null
            appendDisposable(api.createAlbum(
                title,
                groupId,
                description,
                null,
                null,
                uploadsByAdminsOnly,
                commentsDisabled
            )
                .fromIOToMain()
                .subscribe({ album -> goToAlbum(album) }) { t ->
                    showError(getCauseIfRuntime(t))
                })
        }
    }

    private fun goToAlbum(album: VKApiPhotoAlbum) {
        getVKPhotosAlbumPlace(
            accountId, album.owner_id, album.id,
            IVKPhotosView.ACTION_SHOW_PHOTOS
        )
            .withParcelableExtra(Extra.ALBUM, PhotoAlbum(album.id, album.owner_id))
            .tryOpenWith(context)
    }

    private fun goToEditedAlbum(album: PhotoAlbum?, ret: Boolean?) {
        if (ret == null || !ret) return
        getVKPhotosAlbumPlace(
            accountId, (album ?: return).ownerId, album.getObjectId(),
            IVKPhotosView.ACTION_SHOW_PHOTOS
        )
            .withParcelableExtra(Extra.ALBUM, album)
            .tryOpenWith(context)
    }

    fun fireBackButtonClick(): Boolean {
        val currentStep = stepsHost?.currentStep.orZero()
        return if (currentStep > 0) {
            fireStepNegativeButtonClick(currentStep)
            false
        } else {
            true
        }
    }

    fun firePrivacyCommentClick() {}
    fun firePrivacyViewClick() {}
    fun fireUploadByAdminsOnlyChecked(checked: Boolean) {
        state()?.setUploadByAdminsOnly(checked)
    }

    fun fireDisableCommentsClick(checked: Boolean) {
        state()?.setCommentsDisabled(checked)
    }

    private fun state(): PhotoAlbumState? {
        return stepsHost?.state
    }

    fun fireTitleEdit(text: CharSequence?) {
        state()?.setTitle(text.toString())
        view?.updateStepButtonsAvailability(
            CreatePhotoAlbumStepsHost.STEP_TITLE_AND_DESCRIPTION
        )
    }
}