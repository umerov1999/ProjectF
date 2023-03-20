package dev.ragnarok.fenrir.fragment.groupwall

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.abswall.IWallView
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.CommunityDetails
import dev.ragnarok.fenrir.model.GroupSettings
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.PostFilter

interface IGroupWallView : IWallView {
    fun displayBaseCommunityData(community: Community, details: CommunityDetails)
    fun setupPrimaryButton(@StringRes title: Int?)
    fun setupSecondaryButton(@StringRes title: Int?)
    fun openTopics(accountId: Long, ownerId: Long, owner: Owner?)
    fun openCommunityMembers(accountId: Long, groupId: Long)
    fun openDocuments(accountId: Long, ownerId: Long, owner: Owner?)
    fun openProducts(accountId: Long, ownerId: Long, owner: Owner?)
    fun openProductServices(accountId: Long, ownerId: Long)
    fun displayWallFilters(filters: MutableList<PostFilter>)
    fun notifyWallFiltersChanged()
    fun goToCommunityControl(accountId: Long, community: Community, settings: GroupSettings?)
    fun goToShowCommunityInfo(accountId: Long, community: Community)
    fun goToShowCommunityLinksInfo(accountId: Long, community: Community)
    fun goToShowCommunityAboutInfo(accountId: Long, details: CommunityDetails)
    fun goToGroupChats(accountId: Long, community: Community)
    fun startLoginCommunityActivity(groupId: Long)
    fun openCommunityDialogs(accountId: Long, groupId: Long, subtitle: String?)
    fun displayCounters(
        members: Int,
        topics: Int,
        docs: Int,
        photos: Int,
        audio: Int,
        video: Int,
        articles: Int,
        products: Int,
        chats: Int,
        products_services: Int,
        narratives: Int,
        clips: Int
    )

    fun invalidateOptionsMenu()
    interface IOptionMenuView {
        fun setControlVisible(visible: Boolean)
        fun setIsSubscribed(subscribed: Boolean)
        fun setIsFavorite(favorite: Boolean)
    }

    fun displayWallMenus(menus: MutableList<CommunityDetails.Menu>)
    fun notifyWallMenusChanged(hidden: Boolean)
    fun onSinglePhoto(ava: String, prefix: String?, community: Community)
    fun openVKURL(accountId: Long, link: String)
}