package dev.ragnarok.fenrir.fragment.communitycontrol.communitymanageredit

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IProgressView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.User

interface ICommunityManagerEditView : IMvpView, IErrorView, IProgressView,
    IToastView {
    fun displayUserInfo(user: User)
    fun showUserProfile(accountId: Int, user: User)
    fun checkModerator()
    fun checkEditor()
    fun checkAdmin()
    fun setShowAsContactCheched(cheched: Boolean)
    fun setContactInfoVisible(visible: Boolean)
    fun displayPosition(position: String?)
    fun displayEmail(email: String?)
    fun displayPhone(phone: String?)
    fun configRadioButtons(isCreator: Boolean)
    fun goBack()
    fun setDeleteOptionVisible(visible: Boolean)
}