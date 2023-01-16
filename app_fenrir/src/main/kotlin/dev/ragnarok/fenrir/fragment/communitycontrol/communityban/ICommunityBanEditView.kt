package dev.ragnarok.fenrir.fragment.communitycontrol.communityban

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IProgressView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.IdOption
import dev.ragnarok.fenrir.model.Owner

interface ICommunityBanEditView : IMvpView, IErrorView, IProgressView,
    IToastView {
    fun displayUserInfo(user: Owner)
    fun displayBanStatus(adminId: Long, adminName: String?, endDate: Long)
    fun displayBlockFor(blockFor: String?)
    fun displayReason(reason: String?)
    fun diplayComment(comment: String?)
    fun setShowCommentChecked(checked: Boolean)
    fun goBack()
    fun displaySelectOptionDialog(requestCode: Int, options: List<IdOption>)
    fun openProfile(accountId: Long, owner: Owner)
}