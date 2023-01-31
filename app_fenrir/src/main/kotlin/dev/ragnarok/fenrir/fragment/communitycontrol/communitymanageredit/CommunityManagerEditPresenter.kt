package dev.ragnarok.fenrir.fragment.communitycontrol.communitymanageredit

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.domain.IGroupSettingsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime

class CommunityManagerEditPresenter : AccountDependencyPresenter<ICommunityManagerEditView> {
    private val users: List<User>
    private val groupId: Long
    private val interactor: IGroupSettingsInteractor
    private val isCreator: Boolean
    private val adding: Boolean
    private var currentUserIndex = 0
    private var adminLevel = 0
    private var showAsContact: Boolean
    private var position: String? = null
    private var email: String? = null
    private var phone: String? = null
    private var savingNow = false

    constructor(
        accountId: Long,
        groupId: Long,
        manager: Manager,
        savedInstanceState: Bundle?
    ) : super(accountId, savedInstanceState) {
        val user = manager.user
        users = user?.let { listOf(user) } ?: emptyList()
        this.groupId = groupId
        isCreator = "creator".equals(manager.role, ignoreCase = true)
        if (!isCreator) {
            adminLevel = convertRoleToAdminLevel(manager.role.orEmpty())
        }
        showAsContact = manager.isDisplayAsContact
        interactor = InteractorFactory.createGroupSettingsInteractor()
        adding = false
        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            val info = manager.contactInfo
            if (info != null) {
                position = info.getDescription()
                email = info.getEmail()
                phone = info.getPhone()
            }
        }
    }

    constructor(
        accountId: Long,
        groupId: Long,
        users: List<User>,
        savedInstanceState: Bundle?
    ) : super(accountId, savedInstanceState) {
        isCreator = false
        this.users = users
        this.groupId = groupId
        adminLevel = VKApiCommunity.AdminLevel.MODERATOR
        showAsContact = false
        interactor = InteractorFactory.createGroupSettingsInteractor()
        adding = true
        savedInstanceState?.let { restoreState(it) }
    }

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putInt("currentUserIndex", currentUserIndex)
        outState.putString("position", position)
        outState.putString("email", email)
        outState.putString("phone", phone)
    }

    private fun restoreState(state: Bundle) {
        currentUserIndex = state.getInt("currentUserIndex")
        position = state.getString("position")
        email = state.getString("email")
        phone = state.getString("phone")
    }

    private val currentUser: User
        get() = users[currentUserIndex]

    private fun canDelete(): Boolean {
        return !isCreator && !adding
    }

    override fun onGuiCreated(viewHost: ICommunityManagerEditView) {
        super.onGuiCreated(viewHost)
        resolveRadioButtonsCheckState()
        resolveDeleteOptionVisibility()
        resolveRadioButtonsVisibility()
        resolveProgressView()
        resolveContactBlock()
        resolveUserInfoViews()
    }

    private fun resolveRadioButtonsCheckState() {
        if (!isCreator) {
            when (adminLevel) {
                VKApiCommunity.AdminLevel.MODERATOR -> view?.checkModerator()
                VKApiCommunity.AdminLevel.EDITOR -> view?.checkEditor()
                VKApiCommunity.AdminLevel.ADMIN -> view?.checkAdmin()
            }
        }
    }

    private fun resolveDeleteOptionVisibility() {
        view?.setDeleteOptionVisible(
            canDelete()
        )
    }

    private fun resolveRadioButtonsVisibility() {
        view?.configRadioButtons(
            isCreator
        )
    }

    private fun setSavingNow(savingNow: Boolean) {
        this.savingNow = savingNow
        resolveProgressView()
    }

    private fun resolveProgressView() {
        if (savingNow) {
            view?.displayProgressDialog(
                R.string.please_wait,
                R.string.saving,
                false
            )
        } else {
            view?.dismissProgressDialog()
        }
    }

    private val selectedRole: String
        get() = if (isCreator) {
            "creator"
        } else convertAdminLevelToRole(
            adminLevel
        )

    fun fireButtonSaveClick() {
        val role = selectedRole
        val user = currentUser
        setSavingNow(true)
        appendDisposable(interactor.editManager(
            accountId,
            groupId,
            user,
            role,
            showAsContact,
            position,
            email,
            phone
        )
            .fromIOToMain()
            .subscribe({ onSavingComplete() }) { throwable ->
                onSavingError(
                    getCauseIfRuntime(throwable)
                )
            })
    }

    fun fireDeleteClick() {
        if (isCreator) {
            return
        }
        val user = currentUser
        setSavingNow(true)
        appendDisposable(interactor.editManager(
            accountId,
            groupId,
            user,
            null,
            false,
            null,
            null,
            null
        )
            .fromIOToMain()
            .subscribe({ onSavingComplete() }) { throwable ->
                onSavingError(
                    getCauseIfRuntime(throwable)
                )
            })
    }

    private fun onSavingComplete() {
        setSavingNow(false)
        view?.customToast?.showToastSuccessBottom(
            R.string.success
        )
        if (currentUserIndex == users.size - 1) {
            view?.goBack()
        } else {
            // switch to next user
            currentUserIndex++
            resolveUserInfoViews()
            adminLevel = VKApiCommunity.AdminLevel.MODERATOR
            showAsContact = false
            position = null
            email = null
            phone = null
            resolveContactBlock()
            resolveRadioButtonsVisibility()
        }
    }

    private fun resolveContactBlock() {
        view?.let {
            it.setShowAsContactCheched(showAsContact)
            it.setContactInfoVisible(showAsContact)
            it.displayPosition(position)
            it.displayEmail(email)
            it.displayPhone(phone)
        }
    }

    private fun resolveUserInfoViews() {
        view?.displayUserInfo(
            currentUser
        )
    }

    private fun onSavingError(throwable: Throwable) {
        throwable.printStackTrace()
        setSavingNow(false)
        showError(throwable)
    }

    fun fireAvatarClick() {
        view?.showUserProfile(
            accountId,
            currentUser
        )
    }

    fun fireModeratorChecked() {
        adminLevel = VKApiCommunity.AdminLevel.MODERATOR
    }

    fun fireEditorChecked() {
        adminLevel = VKApiCommunity.AdminLevel.EDITOR
    }

    fun fireAdminChecked() {
        adminLevel = VKApiCommunity.AdminLevel.ADMIN
    }

    fun fireShowAsContactChecked(checked: Boolean) {
        if (checked != showAsContact) {
            showAsContact = checked
            view?.setContactInfoVisible(
                checked
            )
        }
    }

    fun firePositionEdit(s: CharSequence?) {
        position = s.toString()
    }

    fun fireEmailEdit(s: CharSequence?) {
        email = s.toString()
    }

    fun firePhoneEdit(s: CharSequence?) {
        phone = s.toString()
    }

    companion object {
        internal fun convertRoleToAdminLevel(role: String): Int {
            return when {
                "moderator".equals(role, ignoreCase = true) -> {
                    VKApiCommunity.AdminLevel.MODERATOR
                }

                "editor".equals(role, ignoreCase = true) -> {
                    VKApiCommunity.AdminLevel.EDITOR
                }

                "administrator".equals(role, ignoreCase = true) -> {
                    VKApiCommunity.AdminLevel.ADMIN
                }

                else -> 0
            }
        }

        internal fun convertAdminLevelToRole(adminLevel: Int): String {
            when (adminLevel) {
                VKApiCommunity.AdminLevel.MODERATOR -> return "moderator"
                VKApiCommunity.AdminLevel.EDITOR -> return "editor"
                VKApiCommunity.AdminLevel.ADMIN -> return "administrator"
            }
            throw IllegalArgumentException("Invalid adminLevel")
        }
    }
}