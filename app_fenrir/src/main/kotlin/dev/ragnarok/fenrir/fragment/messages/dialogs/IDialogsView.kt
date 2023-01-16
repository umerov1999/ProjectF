package dev.ragnarok.fenrir.fragment.messages.dialogs

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.Dialog
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User

interface IDialogsView : IMvpView, IErrorView, IToastView {
    fun displayData(data: List<Dialog>, accountId: Long)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun goToChat(
        accountId: Long,
        messagesOwnerId: Long,
        peerId: Long,
        title: String?,
        ava_url: String?
    )

    fun goToSearch(accountId: Long)
    fun goToImportant(accountId: Long)
    fun showSnackbar(@StringRes res: Int, isLong: Boolean)
    fun askToReload()
    fun showEnterNewGroupChatTitle(users: List<User>)
    fun showNotificationSettings(accountId: Long, peerId: Long)
    fun goToOwnerWall(accountId: Long, ownerId: Long, owner: Owner?)
    fun setCreateGroupChatButtonVisible(visible: Boolean)
    fun notifyHasAttachments(has: Boolean)
    fun updateAccountIdNoRefresh(accountId: Long)

    interface IContextView {
        fun setCanDelete(can: Boolean)
        fun setCanAddToHomescreen(can: Boolean)
        fun setCanConfigNotifications(can: Boolean)
        fun setCanAddToShortcuts(can: Boolean)
        fun setIsHidden(can: Boolean)
        fun setCanRead(can: Boolean)
        fun setPinned(pinned: Boolean)
    }

    interface IOptionView {
        fun setCanSearch(can: Boolean)
    }
}