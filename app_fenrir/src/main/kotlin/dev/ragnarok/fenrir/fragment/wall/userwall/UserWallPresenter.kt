package dev.ragnarok.fenrir.fragment.wall.userwall

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.UserAgentTool
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.toRequestBuilder
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.vkHeader
import dev.ragnarok.fenrir.api.ProxyUtil
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.rest.HttpException
import dev.ragnarok.fenrir.domain.IAccountsInteractor
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IPhotosInteractor
import dev.ragnarok.fenrir.domain.IRelationshipInteractor
import dev.ragnarok.fenrir.domain.IStoriesShortVideosInteractor
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.fragment.friends.friendstabs.FriendsTabsFragment
import dev.ragnarok.fenrir.fragment.wall.AbsWallPresenter
import dev.ragnarok.fenrir.fragment.wall.IWallView
import dev.ragnarok.fenrir.ifNonNullNoEmpty
import dev.ragnarok.fenrir.model.FriendsCounters
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PostFilter
import dev.ragnarok.fenrir.model.RegistrationInfoResult
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UserDetails
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ShortcutUtils.createWallShortcutRx
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.singletonArrayList
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.coroutines.cancellation.CancellationException

class UserWallPresenter(
    accountId: Long,
    ownerId: Long,
    owner: User?,
    savedInstanceState: Bundle?
) : AbsWallPresenter<IUserWallView>(accountId, ownerId, savedInstanceState) {
    private val filters: MutableList<PostFilter> = ArrayList()
    private val ownersRepository: IOwnersRepository = owners
    private val storiesInteractor: IStoriesShortVideosInteractor =
        InteractorFactory.createStoriesInteractor()
    private val relationshipInteractor: IRelationshipInteractor =
        InteractorFactory.createRelationshipInteractor()
    private val accountInteractor: IAccountsInteractor = InteractorFactory.createAccountInteractor()
    private val photosInteractor: IPhotosInteractor = InteractorFactory.createPhotosInteractor()
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val wallsRepository: IWallsRepository = walls
    var user: User
        private set
    private var details: UserDetails
    private var loadingAvatarPhotosNow = false
    override fun onRefresh() {
        requestActualFullInfo()
    }

    private fun resolveCoverImage() {
        details.cover?.images.ifNonNullNoEmpty({
            var def = 0
            var url: String? = null
            for (i in it) {
                if (i.width * i.height > def) {
                    def = i.width * i.height
                    url = i.url
                }
            }
            view?.displayUserCover(user.blacklisted, url, true)
        }, {
            view?.displayUserCover(user.blacklisted, user.maxSquareAvatar, false)
        })
    }

    private fun resolveCounters() {
        view?.displayCounters(
            details.friendsCount,
            details.mutualFriendsCount,
            details.followersCount,
            details.groupsCount,
            details.photosCount,
            details.audiosCount,
            details.videosCount,
            details.articlesCount,
            details.productsCount,
            details.giftCount,
            details.productServicesCount,
            details.narrativesCount,
            details.clipsCount
        )
    }

    private fun resolveBaseUserInfoViews() {
        view?.displayBaseUserInfo(user)
    }

    private fun refreshUserDetails() {
        appendJob(
            ownersRepository.getFullUserInfo(
                accountId,
                ownerId,
                IOwnersRepository.MODE_CACHE
            )
                .fromIOToMain({
                    onFullInfoReceived(it.first, it.second)
                    requestActualFullInfo()
                }, { t -> onDetailsGetError(t) })
        )
    }

    private fun requestActualFullInfo() {
        appendJob(ownersRepository.getFullUserInfo(
            accountId,
            ownerId,
            IOwnersRepository.MODE_NET
        )
            .fromIOToMain({
                onFullInfoReceived(
                    it.first,
                    it.second
                )
            }) { t -> onDetailsGetError(t) })
    }

    private fun onFullInfoReceived(user: User?, details: UserDetails?) {
        if (user != null) {
            this.user = user
            onUserInfoUpdated()
        }
        if (details != null) {
            this.details = details
            onUserDetailsUpdated()
        }
        resolveStatusView()
        resolveMenu()
    }

    private fun onUserDetailsUpdated() {
        syncFilterCountersWithDetails()
        view?.notifyWallFiltersChanged()
        resolvePrimaryActionButton()
        resolveCounters()
        resolveCoverImage()
    }

    private fun onUserInfoUpdated() {
        resolveBaseUserInfoViews()
    }

    private fun onDetailsGetError(t: Throwable) {
        showError(getCauseIfRuntime(t))
    }

    private fun syncFiltersWithSelectedMode() {
        for (filter in filters) {
            filter.setActive(filter.getMode() == wallFilter)
        }
    }

    private fun syncFilterCountersWithDetails() {
        for (filter in filters) {
            when (filter.getMode()) {
                WallCriteria.MODE_ALL -> filter.setCount(details.allWallCount)
                WallCriteria.MODE_OWNER -> filter.setCount(details.ownWallCount)
                WallCriteria.MODE_SCHEDULED -> filter.setCount(details.postponedWallCount)
            }
        }
    }

    override fun onGuiCreated(viewHost: IUserWallView) {
        super.onGuiCreated(viewHost)
        viewHost.displayWallFilters(filters)
        resolveCounters()
        resolveBaseUserInfoViews()
        resolvePrimaryActionButton()
        resolveStatusView()
        resolveMenu()
        resolveProgressDialogView()
        resolveCoverImage()
    }

    private fun createPostFilters(): List<PostFilter> {
        val filters: MutableList<PostFilter> = ArrayList()
        filters.add(PostFilter(WallCriteria.MODE_ALL, getString(R.string.all_posts)))
        filters.add(PostFilter(WallCriteria.MODE_OWNER, getString(R.string.owner_s_posts)))
        if (isMyWall) {
            filters.add(PostFilter(WallCriteria.MODE_SCHEDULED, getString(R.string.scheduled)))
        }
        return filters
    }

    fun fireStatusClick() {
        details.statusAudio.requireNonNull {
            view?.playAudioList(
                accountId, 0, singletonArrayList(
                    it
                )
            )
        }
    }

    fun fireMoreInfoClick() {
        view?.openUserDetails(
            accountId,
            user,
            details
        )
    }

    fun fireFilterClick(entry: PostFilter) {
        if (changeWallFilter(entry.getMode())) {
            syncFiltersWithSelectedMode()
            view?.notifyWallFiltersChanged()
        }
    }

    fun fireHeaderPhotosClick() {
        view?.openPhotoAlbums(
            accountId,
            ownerId,
            user
        )
    }

    fun fireHeaderAudiosClick() {
        view?.openAudios(
            accountId,
            ownerId,
            user
        )
    }

    fun fireHeaderArticlesClick() {
        view?.openArticles(
            accountId,
            ownerId,
            user
        )
    }

    fun fireHeaderProductsClick() {
        view?.openProducts(
            accountId,
            ownerId,
            user
        )
    }

    fun fireHeaderProductServicesClick() {
        view?.openProductServices(
            accountId,
            ownerId
        )
    }

    fun fireHeaderGiftsClick() {
        view?.openGifts(
            accountId,
            ownerId,
            user
        )
    }

    fun fireHeaderFriendsClick() {
        view?.openFriends(
            accountId,
            ownerId,
            FriendsTabsFragment.TAB_ALL_FRIENDS,
            friendsCounters
        )
    }

    private val friendsCounters: FriendsCounters
        get() = FriendsCounters(
            details.friendsCount,
            details.onlineFriendsCount,
            details.followersCount,
            details.mutualFriendsCount
        )

    fun fireHeaderGroupsClick() {
        view?.openGroups(
            accountId,
            ownerId,
            user
        )
    }

    fun fireHeaderVideosClick() {
        view?.openVideosLibrary(
            accountId,
            ownerId,
            user
        )
    }

    @SuppressLint("ResourceType")
    private fun resolvePrimaryActionButton() {
        @StringRes var title: Int? = null
        if (accountId == ownerId) {
            title = R.string.edit_status
        } else {
            title = when (user.friendStatus) {
                VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND -> R.string.add_to_friends
                VKApiUser.FRIEND_STATUS_REQUEST_SENT -> R.string.cancel_request
                VKApiUser.FRIEND_STATUS_HAS_INPUT_REQUEST -> R.string.accept_request
                VKApiUser.FRIEND_STATUS_IS_FRIEDND -> R.string.delete_from_friends
                else -> null
            }
            if (user.blacklisted_by_me) {
                title = R.string.is_to_blacklist
            }
        }
        val finalTitle = title
        view?.setupPrimaryActionButton(
            finalTitle
        )
    }

    fun firePrimaryActionsClick() {
        if (accountId == ownerId) {
            view?.showEditStatusDialog(user.status)
            return
        }
        if (user.blacklisted_by_me) {
            view?.showUnbanMessageDialog()
            return
        }
        when (user.friendStatus) {
            VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND -> view?.showAddToFriendsMessageDialog()
            VKApiUser.FRIEND_STATUS_REQUEST_SENT -> fireDeleteFromFriends()
            VKApiUser.FRIEND_STATUS_IS_FRIEDND -> view?.showDeleteFromFriendsMessageDialog()
            VKApiUser.FRIEND_STATUS_HAS_INPUT_REQUEST -> executeAddToFriendsRequest(null, false)
        }
    }

    private fun DisplayUserProfileAlbum(photos: List<Photo>) {
        setLoadingAvatarPhotosNow(false)
        if (photos.isEmpty()) {
            view?.showSnackbar(
                R.string.no_photos_found,
                true
            )
            return
        }
        val currentAvatarPhotoId = details.photoId?.id
        val currentAvatarOwner_id = details.photoId?.ownerId
        var sel = 0
        if (currentAvatarPhotoId != null && currentAvatarOwner_id != null) {
            var ut = 0
            for (i in photos) {
                if (i.ownerId == currentAvatarOwner_id && i.getObjectId() == currentAvatarPhotoId) {
                    sel = ut
                    break
                }
                ut++
            }
        }
        val curr = sel
        view?.openPhotoAlbum(
            accountId,
            ownerId,
            -6,
            ArrayList(photos),
            curr
        )
    }

    private fun onAddFriendResult(resultCode: Int) {
        var strRes: Int? = null
        var newFriendStatus: Int? = null
        when (resultCode) {
            IRelationshipInteractor.FRIEND_ADD_REQUEST_SENT -> {
                strRes = R.string.friend_request_sent
                newFriendStatus = VKApiUser.FRIEND_STATUS_REQUEST_SENT
            }

            IRelationshipInteractor.FRIEND_ADD_REQUEST_FROM_USER_APPROVED -> {
                strRes = R.string.friend_request_from_user_approved
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_FRIEDND
            }

            IRelationshipInteractor.FRIEND_ADD_RESENDING -> {
                strRes = R.string.request_resending
                newFriendStatus = VKApiUser.FRIEND_STATUS_REQUEST_SENT
            }
        }
        if (newFriendStatus != null) {
            user.setFriendStatus(newFriendStatus)
        }
        if (strRes != null) {
            val finalStrRes: Int = strRes
            view?.showSnackbar(
                finalStrRes,
                true
            )
        }
        resolvePrimaryActionButton()
    }

    fun fireDeleteFromFriends() {
        appendJob(relationshipInteractor.deleteFriends(accountId, ownerId)
            .fromIOToMain({ responseCode -> onFriendsDeleteResult(responseCode) }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    fun fireNewStatusEntered(newValue: String?) {
        appendJob(accountInteractor.changeStatus(accountId, newValue)
            .fromIOToMain({ onStatusChanged(newValue) }) { t ->
                showError(
                    getCauseIfRuntime(t)
                )
            })
    }

    private fun onStatusChanged(status: String?) {
        user.setStatus(status)
        view?.showSnackbar(
            R.string.status_was_changed,
            true
        )
        resolveStatusView()
    }

    private fun resolveStatusView() {
        val statusText: String? = if (details.statusAudio != null) {
            details.statusAudio?.artistAndTitle
        } else {
            user.status
        }
        view?.displayUserStatus(
            statusText,
            details.statusAudio != null
        )
    }

    private fun resolveMenu() {
        view?.invalidateOptionsMenu()
    }

    fun fireAddToFrindsClick(message: String?) {
        executeAddToFriendsRequest(message, false)
    }

    fun fireAddToBookmarks() {
        appendJob(faveInteractor.addPage(accountId, ownerId)
            .fromIOToMain({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireRemoveFromBookmarks() {
        appendJob(faveInteractor.removePage(accountId, ownerId, true)
            .fromIOToMain({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireSubscribe() {
        appendJob(wallsRepository.subscribe(accountId, ownerId)
            .fromIOToMain({ onExecuteComplete() }) { t -> onExecuteError(t) })

        appendJob(storiesInteractor.subscribe(accountId, ownerId)
            .fromIOToMain({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireUnSubscribe() {
        appendJob(wallsRepository.unsubscribe(accountId, ownerId)
            .fromIOToMain({ onExecuteComplete() }) { t -> onExecuteError(t) })

        appendJob(storiesInteractor.unsubscribe(accountId, ownerId)
            .fromIOToMain({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    private fun executeAddToFriendsRequest(text: String?, follow: Boolean) {
        appendJob(relationshipInteractor.addFriend(accountId, ownerId, text, follow)
            .fromIOToMain({ resultCode -> onAddFriendResult(resultCode) }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    private fun onFriendsDeleteResult(responseCode: Int) {
        var strRes: Int? = null
        var newFriendStatus: Int? = null
        when (responseCode) {
            IRelationshipInteractor.DeletedCodes.FRIEND_DELETED -> {
                newFriendStatus = VKApiUser.FRIEND_STATUS_HAS_INPUT_REQUEST
                strRes = R.string.friend_deleted
            }

            IRelationshipInteractor.DeletedCodes.OUT_REQUEST_DELETED -> {
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND
                strRes = R.string.out_request_deleted
            }

            IRelationshipInteractor.DeletedCodes.IN_REQUEST_DELETED -> {
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND
                strRes = R.string.in_request_deleted
            }

            IRelationshipInteractor.DeletedCodes.SUGGESTION_DELETED -> {
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND
                strRes = R.string.suggestion_deleted
            }
        }
        if (newFriendStatus != null) {
            user.setFriendStatus(newFriendStatus)
        }
        if (strRes != null) {
            val finalStrRes: Int = strRes
            view?.showSnackbar(
                finalStrRes,
                true
            )
            resolvePrimaryActionButton()
        }
    }

    private fun prepareUserAvatarsAndShow() {
        setLoadingAvatarPhotosNow(true)
        appendJob(photosInteractor[accountId, ownerId, -6, 100, 0, true]
            .fromIOToMain({ photos -> DisplayUserProfileAlbum(photos) }) { t ->
                onAvatarAlbumPrepareFailed(
                    t
                )
            })
    }

    private fun onAvatarAlbumPrepareFailed(t: Throwable) {
        setLoadingAvatarPhotosNow(false)
        showError(getCauseIfRuntime(t))
    }

    private fun resolveProgressDialogView() {
        if (loadingAvatarPhotosNow) {
            view?.displayProgressDialog(
                R.string.please_wait,
                R.string.loading_owner_photo_album,
                false
            )
        } else {
            view?.dismissProgressDialog()
        }
    }

    private fun setLoadingAvatarPhotosNow(loadingAvatarPhotosNow: Boolean) {
        this.loadingAvatarPhotosNow = loadingAvatarPhotosNow
        resolveProgressDialogView()
    }

    fun fireAvatarClick() {
        view?.showAvatarContextMenu(isMyWall)
    }

    fun fireAvatarLongClick() {
        view?.showMention(
            accountId,
            ownerId
        )
    }

    fun fireOpenAvatarsPhotoAlbum() {
        prepareUserAvatarsAndShow()
    }

    fun fireMentions() {
        view?.goMentions(accountId, ownerId)
    }

    override fun fireOptionViewCreated(view: IWallView.IOptionView) {
        super.fireOptionViewCreated(view)
        view.setIsBlacklistedByMe(user.blacklisted_by_me)
        view.setIsFavorite(details.isFavorite)
        view.setIsSubscribed(details.isSubscribed)
    }

    fun renameLocal(name: String?) {
        Settings.get().main().setUserNameChanges(ownerId, name)
        onUserInfoUpdated()
    }

    private fun parseResponse(str: String, pattern: Pattern): String? {
        val matcher = pattern.matcher(str)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }

    private fun getRegistrationDate(owner_id: Long): Flow<RegistrationInfoResult> {
        return flow {
            val builder: OkHttpClient.Builder = OkHttpClient.Builder()
                .readTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
                .callTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                    val request =
                        chain.toRequestBuilder(false).vkHeader(true).addHeader(
                            "User-Agent", UserAgentTool.USER_AGENT_CURRENT_ACCOUNT
                        ).build()
                    chain.proceed(request)
                })
            ProxyUtil.applyProxyConfig(builder, Includes.proxySettings.activeProxy)
            val request: Request = Request.Builder()
                .url("https://vk.com/foaf.php?id=$owner_id").build()

            val call = builder.build().newCall(request)
            try {
                val response = call.execute()
                if (!response.isSuccessful) {
                    throw HttpException(response.code)
                } else {
                    val resp = response.body.string()
                    val locale = Utils.appLocale
                    var registered: String? = null
                    var auth: String? = null
                    var changes: String? = null
                    var tmp =
                        parseResponse(
                            resp,
                            Pattern.compile("ya:created dc:date=\"(.*?)\"")
                        )
                    if (tmp.nonNullNoEmpty()) {
                        registered = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", locale).parse(
                            tmp
                        )?.let {
                            DateFormat.getDateInstance(1).format(
                                it
                            )
                        }
                    }
                    tmp = parseResponse(
                        resp,
                        Pattern.compile("ya:lastLoggedIn dc:date=\"(.*?)\"")
                    )
                    if (tmp.nonNullNoEmpty()) {
                        auth = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", locale).parse(
                            tmp
                        )?.let {
                            DateFormat.getDateInstance(1).format(
                                it
                            )
                        }
                    }
                    tmp = parseResponse(
                        resp,
                        Pattern.compile("ya:modified dc:date=\"(.*?)\"")
                    )
                    if (tmp.nonNullNoEmpty()) {
                        changes = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", locale).parse(
                            tmp
                        )?.let {
                            DateFormat.getDateInstance(1).format(
                                it
                            )
                        }
                    }
                    emit(
                        RegistrationInfoResult().setRegistered(registered).setAuth(auth)
                            .setChanges(changes)
                    )
                }
                response.close()
            } catch (e: CancellationException) {
                call.cancel()
                throw e
            }
        }
    }

    fun fireGetRegistrationDate() {
        appendJob(
            getRegistrationDate(ownerId).fromIOToMain({
                view?.showRegistrationDate(it)
            }, { showError(it) })
        )
    }

    fun fireReport(context: Context) {
        val values = arrayOf<CharSequence>("porn", "spam", "insult", "advertisement")
        val items = arrayOf<CharSequence>(
            "Порнография",
            "Спам, Мошенничество",
            "Оскорбительное поведение",
            "Рекламная страница"
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.report)
            .setItems(items) { dialog, item ->
                val report = values[item].toString()
                appendJob(ownersRepository.report(accountId, ownerId, report, null)
                    .fromIOToMain({ p ->
                        if (p == 1) view?.customToast?.showToast(
                            R.string.success
                        )
                        else view?.customToast?.showToast(
                            R.string.error
                        )
                    }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
                dialog.dismiss()
            }
            .show()
    }

    fun fireChatClick() {
        val peer = Peer(Peer.fromUserId(user.getOwnerObjectId()))
            .setAvaUrl(user.maxSquareAvatar)
            .setTitle(user.fullName)
        view?.openChatWith(
            accountId,
            accountId,
            peer
        )
    }

    override fun fireAddToShortcutClick(context: Context) {
        appendJob(
            createWallShortcutRx(
                context,
                accountId,
                user.ownerId,
                user.fullName,
                user.maxSquareAvatar
            )
                .fromIOToMain({
                    view?.showSnackbar(
                        R.string.success,
                        true
                    )
                }) { t ->
                    view?.showError(t.localizedMessage)
                })
    }

    override fun searchStory(ByName: Boolean) {
        appendJob(storiesInteractor.searchStories(
            accountId,
            if (ByName) user.fullName else null,
            if (ByName) null else ownerId
        )
            .fromIOToMain({
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
        return user
    }

    init {
        filters.addAll(createPostFilters())
        user = owner ?: User(ownerId)
        details = UserDetails()
        syncFiltersWithSelectedMode()
        syncFilterCountersWithDetails()
        refreshUserDetails()
    }
}