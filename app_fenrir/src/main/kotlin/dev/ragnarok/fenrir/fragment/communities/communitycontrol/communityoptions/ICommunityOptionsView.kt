package dev.ragnarok.fenrir.fragment.communities.communitycontrol.communityoptions

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IProgressView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.Day
import dev.ragnarok.fenrir.model.IdOption

interface ICommunityOptionsView : IMvpView, IErrorView, IProgressView,
    IToastView {
    fun displayName(name: String?)
    fun displayDescription(description: String?)
    fun setCommunityTypeVisible(visible: Boolean)
    fun displayAddress(address: String?)
    fun setCategoryVisible(visible: Boolean)
    fun displayCategory(categoryText: String?)
    fun showSelectOptionDialog(requestCode: Int, data: List<IdOption>)
    fun displayWebsite(website: String?)
    fun setPublicDateVisible(visible: Boolean)
    fun dislayPublicDate(day: Day)
    fun setFeedbackCommentsRootVisible(visible: Boolean)
    fun setFeedbackCommentsChecked(checked: Boolean)
    fun setObsceneFilterChecked(checked: Boolean)
    fun setObsceneStopWordsChecked(checked: Boolean)
    fun setObsceneStopWordsVisible(visible: Boolean)
    fun displayObsceneStopWords(words: String?)
    fun setGroupType(type: Int)
    fun resolveEdge(age: Int)
}