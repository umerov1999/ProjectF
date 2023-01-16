package dev.ragnarok.fenrir.fragment.attachments.abspostedit

import android.os.Bundle
import dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit.AbsAttachmentsEditPresenter
import dev.ragnarok.fenrir.util.BooleanValue

abstract class AbsPostEditPresenter<V : IBasePostEditView> internal constructor(
    accountId: Long,
    savedInstanceState: Bundle?
) : AbsAttachmentsEditPresenter<V>(accountId, savedInstanceState) {
    val fromGroup = BooleanValue()
    val friendsOnly = BooleanValue()
    val addSignature = BooleanValue()
    private val friendsOnlyOptionAvailable = BooleanValue()
    private val fromGroupOptionAvailable = BooleanValue()
    private val addSignatureOptionAvailable = BooleanValue()
    fun fireShowAuthorChecked(checked: Boolean) {
        if (addSignature.setValue(checked)) {
            onShowAuthorChecked(checked)
        }
    }

    override fun onGuiCreated(viewHost: V) {
        super.onGuiCreated(viewHost)
        viewHost.setFriendsOnlyOptionVisible(friendsOnlyOptionAvailable.get())
        viewHost.setFromGroupOptionVisible(fromGroupOptionAvailable.get())
        viewHost.setAddSignatureOptionVisible(addSignatureOptionAvailable.get())
        viewHost.setShowAuthorChecked(addSignature.get())
        viewHost.setFriendsOnlyChecked(friendsOnly.get())
        viewHost.setFromGroupChecked(fromGroup.get())
    }

    fun checkFriendsOnly(checked: Boolean) {
        if (friendsOnly.setValue(checked)) {
            view?.setFriendsOnlyChecked(checked)
        }
    }

    open fun onShowAuthorChecked(checked: Boolean) {}
    fun fireFromGroupChecked(checked: Boolean) {
        if (fromGroup.setValue(checked)) {
            onFromGroupChecked(checked)
        }
    }

    open fun onFromGroupChecked(checked: Boolean) {}
    fun setFromGroupOptionAvailable(available: Boolean) {
        if (fromGroupOptionAvailable.setValue(available)) {
            view?.setFromGroupOptionVisible(available)
        }
    }

    fun fireFriendsOnlyChecked(checked: Boolean) {
        if (friendsOnly.setValue(checked)) {
            onFriendsOnlyChecked()
        }
    }

    fun isAddSignatureOptionAvailable(): Boolean {
        return addSignatureOptionAvailable.get()
    }

    fun setAddSignatureOptionAvailable(available: Boolean) {
        if (addSignatureOptionAvailable.setValue(available)) {
            view?.setAddSignatureOptionVisible(available)
        }
    }

    fun isFriendsOnlyOptionAvailable(): Boolean {
        return friendsOnlyOptionAvailable.get()
    }

    fun setFriendsOnlyOptionAvailable(available: Boolean) {
        if (friendsOnlyOptionAvailable.setValue(available)) {
            view?.setFriendsOnlyOptionVisible(available)
        }
    }

    private fun onFriendsOnlyChecked() {}
}