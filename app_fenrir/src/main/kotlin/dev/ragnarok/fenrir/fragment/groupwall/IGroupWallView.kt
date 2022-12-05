package dev.ragnarok.fenrir.fragment.groupwall

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.abswall.IWallView
import dev.ragnarok.fenrir.model.*

interface IGroupWallView : IWallView {
    fun displayBaseCommunityData(community: Community, details: CommunityDetails)
    fun setupPrimaryButton(@StringRes title: Int?)
    fun setupSecondaryButton(@StringRes title: Int?)
    fun openTopics(accountId: Int, ownerId: Int, owner: Owner?)
    fun openCommunityMembers(accountId: Int, groupId: Int)
    fun openDocuments(accountId: Int, ownerId: Int, owner: Owner?)
    fun openProducts(accountId: Int, ownerId: Int, owner: Owner?)
    fun openProductServices(accountId: Int, ownerId: Int)
    fun displayWallFilters(filters: MutableList<PostFilter>)
    fun notifyWallFiltersChanged()
    fun goToCommunityControl(accountId: Int, community: Community, settings: GroupSettings?)
    fun goToShowCommunityInfo(accountId: Int, community: Community)
    fun goToShowCommunityLinksInfo(accountId: Int, community: Community)
    fun goToShowCommunityAboutInfo(accountId: Int, details: CommunityDetails)
    fun goToGroupChats(accountId: Int, community: Community)
    fun startLoginCommunityActivity(groupId: Int)
    fun openCommunityDialogs(accountId: Int, groupId: Int, subtitle: String?)
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
        narratives: Int
    )

    fun InvalidateOptionsMenu()
    interface IOptionMenuView {
        fun setControlVisible(visible: Boolean)
        fun setIsSubscribed(subscribed: Boolean)
        fun setIsFavorite(favorite: Boolean)
    }

    fun displayWallMenus(menus: MutableList<CommunityDetails.Menu>)
    fun notifyWallMenusChanged(hidden: Boolean)
    fun onSinglePhoto(ava: String, prefix: String?, community: Community)
    fun openVKURL(accountId: Int, link: String)
}