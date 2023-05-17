package dev.ragnarok.fenrir.fragment.wall.groupwall

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.domain.ICommunitiesInteractor
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IStoriesShortVideosInteractor
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.domain.impl.GroupSettingsInteractor
import dev.ragnarok.fenrir.fragment.wall.AbsWallPresenter
import dev.ragnarok.fenrir.fragment.wall.groupwall.IGroupWallView.IOptionMenuView
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.CommunityDetails
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.PostFilter
import dev.ragnarok.fenrir.model.Token
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.place.PlaceFactory.getMentionsPlace
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.ISettings.IAccountsSettings
import dev.ragnarok.fenrir.util.ShortcutUtils.createWallShortcutRx
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.singletonArrayList
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import kotlin.math.abs

class GroupWallPresenter(
    accountId: Long,
    ownerId: Long,
    pCommunity: Community?,
    private val context: Context,
    savedInstanceState: Bundle?
) : AbsWallPresenter<IGroupWallView>(accountId, ownerId, savedInstanceState) {
    private var community: Community
    private val settings: IAccountsSettings
    private val faveInteractor: IFaveInteractor
    private val ownersRepository: IOwnersRepository
    private val communitiesInteractor: ICommunitiesInteractor
    private val storiesInteractor: IStoriesShortVideosInteractor
    private val wallsRepository: IWallsRepository
    private val filters: MutableList<PostFilter>
    private val menus: MutableList<CommunityDetails.Menu>
    private var details: CommunityDetails
    private fun resolveBaseCommunityViews() {
        view?.displayBaseCommunityData(
            community, details
        )
    }

    private fun resolveMenu() {
        view?.invalidateOptionsMenu()
    }

    private fun resolveCounters() {
        view?.displayCounters(
            community.membersCount,
            details.getTopicsCount(),
            details.getDocsCount(),
            details.getPhotosCount(),
            details.getAudiosCount(),
            details.getVideosCount(),
            details.getArticlesCount(),
            details.getProductsCount(),
            details.getChatsCount(),
            details.getProductServicesCount(),
            details.getNarrativesCount(),
            details.getClipsCount()
        )
    }

    private fun refreshInfo() {
        appendDisposable(
            ownersRepository.getFullCommunityInfo(
                accountId,
                abs(ownerId),
                IOwnersRepository.MODE_CACHE
            )
                .fromIOToMain()
                .subscribe({
                    onFullInfoReceived(it.first, it.second)
                    requestActualFullInfo()
                }, ignore())
        )
    }

    private fun requestActualFullInfo() {
        appendDisposable(ownersRepository.getFullCommunityInfo(
            accountId,
            abs(ownerId),
            IOwnersRepository.MODE_NET
        )
            .fromIOToMain()
            .subscribe({
                onFullInfoReceived(
                    it.first,
                    it.second
                )
            }) { t -> onDetailsGetError(t) })
    }

    private fun onFullInfoReceived(community: Community?, details: CommunityDetails?) {
        if (community != null) {
            this.community = community
        }
        if (details != null) {
            this.details = details
        }
        menus.clear()
        menus.addAll(details?.getMenu().orEmpty())
        view?.notifyWallMenusChanged(menus.isEmpty())
        filters.clear()
        filters.addAll(createPostFilters())
        syncFiltersWithSelectedMode()
        syncFilterCounters()
        view?.notifyWallFiltersChanged()
        resolveActionButtons()
        resolveCounters()
        resolveBaseCommunityViews()
        resolveMenu()
    }

    private fun onDetailsGetError(t: Throwable) {
        showError(getCauseIfRuntime(t))
    }

    private fun createPostFilters(): List<PostFilter> {
        val filters: MutableList<PostFilter> = ArrayList()
        filters.add(PostFilter(WallCriteria.MODE_ALL, getString(R.string.all_posts)))
        filters.add(PostFilter(WallCriteria.MODE_OWNER, getString(R.string.owner_s_posts)))
        filters.add(PostFilter(WallCriteria.MODE_SUGGEST, getString(R.string.suggests)))
        filters.add(PostFilter(WallCriteria.MODE_DONUT, getString(R.string.donut)))
        if (isAdmin) {
            filters.add(PostFilter(WallCriteria.MODE_SCHEDULED, getString(R.string.scheduled)))
        }
        return filters
    }

    private val isAdmin: Boolean
        get() = community.isAdmin

    private fun syncFiltersWithSelectedMode() {
        for (filter in filters) {
            filter.setActive(filter.getMode() == wallFilter)
        }
    }

    private fun syncFilterCounters() {
        for (filter in filters) {
            when (filter.getMode()) {
                WallCriteria.MODE_ALL -> filter.setCount(details.getAllWallCount())
                WallCriteria.MODE_OWNER -> filter.setCount(details.getOwnerWallCount())
                WallCriteria.MODE_SCHEDULED -> filter.setCount(details.getPostponedWallCount())
                WallCriteria.MODE_SUGGEST -> filter.setCount(details.getSuggestedWallCount())
                WallCriteria.MODE_DONUT -> filter.setCount(details.getDonutWallCount())
            }
        }
    }

    override fun onGuiCreated(viewHost: IGroupWallView) {
        super.onGuiCreated(viewHost)
        viewHost.displayWallFilters(filters)
        viewHost.displayWallMenus(menus)
        resolveBaseCommunityViews()
        resolveMenu()
        resolveCounters()
        resolveActionButtons()
    }

    fun fireMenuClick(menu: CommunityDetails.Menu) {
        menu.url?.let { view?.openVKURL(accountId, it) }
    }

    fun fireAvatarPhotoClick(url: String?, prefix: String?) {
        view?.onSinglePhoto(url ?: (community.originalAvatar ?: return), prefix, community)
    }

    fun firePrimaryButtonClick() {
        if (community.memberStatus == VKApiCommunity.MemberStatus.IS_MEMBER || community.memberStatus == VKApiCommunity.MemberStatus.SENT_REQUEST) {
            leaveCommunity()
        } else {
            joinCommunity()
        }
    }

    fun fireSecondaryButtonClick() {
        if (community.memberStatus == VKApiCommunity.MemberStatus.INVITED) {
            leaveCommunity()
        }
    }

    private fun leaveCommunity() {
        val groupId = abs(ownerId)
        appendDisposable(communitiesInteractor.leave(accountId, groupId)
            .fromIOToMain()
            .subscribe({ onLeaveResult() }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    private fun joinCommunity() {
        val groupId = abs(ownerId)
        appendDisposable(communitiesInteractor.join(accountId, groupId)
            .fromIOToMain()
            .subscribe({ onJoinResult() }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    fun fireHeaderPhotosClick() {
        view?.openPhotoAlbums(
            accountId,
            ownerId,
            community
        )
    }

    fun fireHeaderAudiosClick() {
        view?.openAudios(
            accountId,
            ownerId,
            community
        )
    }

    fun fireHeaderArticlesClick() {
        view?.openArticles(
            accountId,
            ownerId,
            community
        )
    }

    fun fireHeaderProductsClick() {
        view?.openProducts(
            accountId,
            ownerId,
            community
        )
    }

    fun fireHeaderProductServicesClick() {
        view?.openProductServices(
            accountId,
            ownerId
        )
    }

    fun fireHeaderVideosClick() {
        view?.openVideosLibrary(
            accountId,
            ownerId,
            community
        )
    }

    fun fireHeaderMembersClick() {
        view?.openCommunityMembers(
            accountId,
            abs(ownerId)
        )
    }

    fun fireHeaderTopicsClick() {
        view?.openTopics(
            accountId,
            ownerId,
            community
        )
    }

    fun fireHeaderDocsClick() {
        view?.openDocuments(
            accountId,
            ownerId,
            community
        )
    }

    fun fireShowCommunityInfoClick() {
        view?.goToShowCommunityInfo(
            accountId,
            community
        )
    }

    fun fireShowCommunityLinksInfoClick() {
        view?.goToShowCommunityLinksInfo(
            accountId,
            community
        )
    }

    fun fireShowCommunityAboutInfoClick() {
        view?.goToShowCommunityAboutInfo(
            accountId,
            details
        )
    }

    fun fireGroupChatsClick() {
        view?.goToGroupChats(
            accountId,
            community
        )
    }

    fun fireHeaderStatusClick() {
        details.getStatusAudio().requireNonNull {
            view?.playAudioList(
                accountId, 0, singletonArrayList(
                    it
                )
            )
        }
    }

    private fun resolveActionButtons() {
        @StringRes var primaryText: Int? = null
        @StringRes var secondaryText: Int? = null
        when (community.memberStatus) {
            VKApiCommunity.MemberStatus.IS_NOT_MEMBER -> when (community.communityType) {
                VKApiCommunity.Type.GROUP -> when (community.closed) {
                    VKApiCommunity.Status.CLOSED -> primaryText = R.string.community_send_request
                    VKApiCommunity.Status.OPEN -> primaryText = R.string.community_join
                }

                VKApiCommunity.Type.PAGE -> primaryText = R.string.community_follow
                VKApiCommunity.Type.EVENT -> primaryText = R.string.community_to_go
            }

            VKApiCommunity.MemberStatus.IS_MEMBER -> when (community.communityType) {
                VKApiCommunity.Type.GROUP -> primaryText = R.string.community_leave
                VKApiCommunity.Type.PAGE -> primaryText = R.string.community_unsubscribe_from_news
                VKApiCommunity.Type.EVENT -> primaryText = R.string.community_not_to_go
            }

            VKApiCommunity.MemberStatus.NOT_SURE -> primaryText = R.string.community_leave
            VKApiCommunity.MemberStatus.DECLINED_INVITATION -> primaryText =
                R.string.community_send_request

            VKApiCommunity.MemberStatus.SENT_REQUEST -> primaryText = R.string.cancel_request
            VKApiCommunity.MemberStatus.INVITED -> {
                primaryText = R.string.community_join
                secondaryText = R.string.cancel_invitation
            }
        }
        val finalPrimaryText = primaryText
        val finalSecondaryText = secondaryText
        view?.let {
            it.setupPrimaryButton(finalPrimaryText)
            it.setupSecondaryButton(finalSecondaryText)
        }
    }

    private fun onLeaveResult() {
        var resultMessage: Int? = null
        when (community.memberStatus) {
            VKApiCommunity.MemberStatus.IS_MEMBER -> {
                community.setMemberStatus(VKApiCommunity.MemberStatus.IS_NOT_MEMBER)
                community.setMember(false)
                when (community.communityType) {
                    VKApiCommunity.Type.GROUP, VKApiCommunity.Type.EVENT -> resultMessage =
                        R.string.community_leave_success

                    VKApiCommunity.Type.PAGE -> resultMessage =
                        R.string.community_unsubscribe_from_news_success
                }
            }

            VKApiCommunity.MemberStatus.SENT_REQUEST -> if (community.communityType == VKApiCommunity.Type.GROUP) {
                community.setMemberStatus(VKApiCommunity.MemberStatus.IS_NOT_MEMBER)
                community.setMember(false)
                resultMessage = R.string.request_canceled
            }

            VKApiCommunity.MemberStatus.INVITED -> if (community.communityType == VKApiCommunity.Type.GROUP) {
                community.setMember(false)
                community.setMemberStatus(VKApiCommunity.MemberStatus.IS_NOT_MEMBER)
                resultMessage = R.string.invitation_has_been_declined
            }
        }
        resolveActionButtons()
        if (resultMessage != null) {
            val finalResultMessage: Int = resultMessage
            view?.showSnackbar(
                finalResultMessage,
                true
            )
        }
    }

    private fun onJoinResult() {
        var resultMessage: Int? = null
        when (community.memberStatus) {
            VKApiCommunity.MemberStatus.IS_NOT_MEMBER -> when (community.communityType) {
                VKApiCommunity.Type.GROUP -> when (community.closed) {
                    VKApiCommunity.Status.CLOSED -> {
                        community.setMember(false)
                        community.setMemberStatus(VKApiCommunity.MemberStatus.SENT_REQUEST)
                        resultMessage = R.string.community_send_request_success
                    }

                    VKApiCommunity.Status.OPEN -> {
                        community.setMember(true)
                        community.setMemberStatus(VKApiCommunity.MemberStatus.IS_MEMBER)
                        resultMessage = R.string.community_join_success
                    }
                }

                VKApiCommunity.Type.PAGE, VKApiCommunity.Type.EVENT -> {
                    community.setMember(true)
                    community.setMemberStatus(VKApiCommunity.MemberStatus.IS_MEMBER)
                    resultMessage = R.string.community_follow_success
                }
            }

            VKApiCommunity.MemberStatus.DECLINED_INVITATION -> if (community.communityType == VKApiCommunity.Type.GROUP) {
                community.setMember(false)
                community.setMemberStatus(VKApiCommunity.MemberStatus.SENT_REQUEST)
                resultMessage = R.string.community_send_request_success
            }

            VKApiCommunity.MemberStatus.INVITED -> if (community.communityType == VKApiCommunity.Type.GROUP) {
                community.setMember(true)
                community.setMemberStatus(VKApiCommunity.MemberStatus.IS_MEMBER)
                resultMessage = R.string.community_join_success
            }
        }
        resolveActionButtons()
        if (resultMessage != null) {
            val finalResultMessage: Int = resultMessage
            view?.showSnackbar(
                finalResultMessage,
                true
            )
        }
    }

    fun fireFilterEntryClick(entry: PostFilter) {
        if (changeWallFilter(entry.getMode())) {
            syncFiltersWithSelectedMode()
            view?.notifyWallFiltersChanged()
        }
    }

    fun fireCommunityControlClick() {
        val groupId = abs(ownerId)
        val interactor =
            GroupSettingsInteractor(Includes.networkInterfaces, Includes.stores.owners(), owners)
        appendDisposable(
            interactor.getGroupSettings(accountId, groupId)
                .fromIOToMain()
                .subscribe({
                    view?.goToCommunityControl(accountId, community, it)
                }, {
                    view?.goToCommunityControl(
                        accountId,
                        community,
                        null
                    )
                })
        )
    }

    fun fireCommunityMessagesClick() {
        if (settings.getAccessToken(ownerId).nonNullNoEmpty()) {
            openCommunityMessages()
        } else {
            val groupId = abs(ownerId)
            view?.startLoginCommunityActivity(
                groupId
            )
        }
    }

    private fun openCommunityMessages() {
        val groupId = abs(ownerId)
        val subtitle = community.fullName
        view?.openCommunityDialogs(
            accountId,
            groupId,
            subtitle
        )
    }

    fun fireGroupTokensReceived(tokens: ArrayList<Token>) {
        for (token in tokens) {
            settings.registerAccountId(token.ownerId, false)
            settings.storeAccessToken(token.ownerId, token.accessToken)
        }
        if (tokens.size == 1) {
            openCommunityMessages()
        }
    }

    fun fireSubscribe() {
        appendDisposable(wallsRepository.subscribe(accountId, ownerId)
            .fromIOToMain()
            .subscribe({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireUnSubscribe() {
        appendDisposable(wallsRepository.unsubscribe(accountId, ownerId)
            .fromIOToMain()
            .subscribe({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireAddToBookmarksClick() {
        appendDisposable(faveInteractor.addPage(accountId, ownerId)
            .fromIOToMain()
            .subscribe({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireRemoveFromBookmarks() {
        appendDisposable(faveInteractor.removePage(accountId, ownerId, false)
            .fromIOToMain()
            .subscribe({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireMentions() {
        getMentionsPlace(accountId, ownerId).tryOpenWith(context)
    }

    override fun onRefresh() {
        requestActualFullInfo()
    }

    fun fireOptionMenuViewCreated(view: IOptionMenuView) {
        view.setControlVisible(isAdmin)
        view.setIsFavorite(details.isSetFavorite())
        view.setIsSubscribed(details.isSetSubscribed())
    }

    fun fireChatClick() {
        val peer = Peer(ownerId).setTitle(
            community.fullName
        ).setAvaUrl(community.maxSquareAvatar)
        view?.openChatWith(
            accountId,
            accountId,
            peer
        )
    }

    override fun fireAddToShortcutClick() {
        appendDisposable(
            createWallShortcutRx(
                context,
                accountId,
                community.ownerId,
                community.fullName,
                community.maxSquareAvatar
            )
                .fromIOToMain().subscribe({
                    view?.showSnackbar(
                        R.string.success,
                        true
                    )
                }) { t ->
                    view?.showError(t.localizedMessage)
                })
    }

    override fun searchStory(ByName: Boolean) {
        appendDisposable(storiesInteractor.searchStory(
            accountId,
            if (ByName) community.fullName else null,
            if (ByName) null else ownerId
        )
            .fromIOToMain()
            .subscribe({
                if (it.nonNullNoEmpty()) {
                    stories.clear()
                    stories.addAll(it)
                    view?.updateStory(
                        stories
                    )
                }
            }) { })
    }

    override fun getOwner(): Owner {
        return community
    }

    init {
        community = pCommunity ?: Community(abs(ownerId))
        details = CommunityDetails()
        ownersRepository = owners
        storiesInteractor = InteractorFactory.createStoriesInteractor()
        faveInteractor = InteractorFactory.createFaveInteractor()
        communitiesInteractor = InteractorFactory.createCommunitiesInteractor()
        settings = Includes.settings.accounts()
        wallsRepository = walls
        filters = ArrayList()
        menus = ArrayList()
        filters.addAll(createPostFilters())
        syncFiltersWithSelectedMode()
        syncFilterCounters()
        refreshInfo()
    }
}