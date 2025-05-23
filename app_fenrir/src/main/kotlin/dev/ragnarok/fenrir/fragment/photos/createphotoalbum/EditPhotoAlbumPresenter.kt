package dev.ragnarok.fenrir.fragment.photos.createphotoalbum

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoAlbumEditor
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStepsHost
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStepsHost.PhotoAlbumState
import kotlin.math.abs

class EditPhotoAlbumPresenter : AccountDependencyPresenter<IEditPhotoAlbumView> {
    private val networker: INetworker
    private val editing: Boolean
    private val ownerId: Long
    private val editor: PhotoAlbumEditor
    private var album: PhotoAlbum? = null
    private var stepsHost: CreatePhotoAlbumStepsHost? = null

    constructor(
        accountId: Long,
        ownerId: Long,
        savedInstanceState: Bundle?
    ) : super(accountId, savedInstanceState) {
        networker = networkInterfaces
        this.ownerId = ownerId
        editor = PhotoAlbumEditor.create()
        editing = false
        init(savedInstanceState)
    }

    constructor(
        accountId: Long,
        album: PhotoAlbum,
        editor: PhotoAlbumEditor,
        savedInstanceState: Bundle?
    ) : super(accountId, savedInstanceState) {
        networker = networkInterfaces
        this.album = album
        ownerId = album.ownerId
        this.editor = editor
        editing = true
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
            .setPrivacyComment(editor.privacyComment)
            .setPrivacyView(editor.privacyView)
            .setCommentsDisabled(editor.commentsDisabled)
            .setUploadByAdminsOnly(editor.uploadByAdminsOnly)
            .setDescription(editor.description)
            .setTitle(editor.title)
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
                    .fromIOToMain({ t -> view?.goToEditedAlbum(accountId, album, t) }) { l ->
                        showError(
                            getCauseIfRuntime(l)
                        )
                    }
            }?.let { appendJob(it) }
        } else {
            val groupId = if (ownerId < 0) abs(ownerId) else null
            appendJob(
                api.createAlbum(
                    title,
                    groupId,
                    description,
                    null,
                    null,
                    uploadsByAdminsOnly,
                    commentsDisabled
                )
                    .fromIOToMain({ album -> view?.goToAlbum(accountId, album) }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
        }
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