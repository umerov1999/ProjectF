package dev.ragnarok.fenrir.api.impl

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.IVKRestProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IAccountApi
import dev.ragnarok.fenrir.api.interfaces.IAccountApis
import dev.ragnarok.fenrir.api.interfaces.IAudioApi
import dev.ragnarok.fenrir.api.interfaces.IBoardApi
import dev.ragnarok.fenrir.api.interfaces.ICommentsApi
import dev.ragnarok.fenrir.api.interfaces.IDatabaseApi
import dev.ragnarok.fenrir.api.interfaces.IDocsApi
import dev.ragnarok.fenrir.api.interfaces.IFaveApi
import dev.ragnarok.fenrir.api.interfaces.IFriendsApi
import dev.ragnarok.fenrir.api.interfaces.IGroupsApi
import dev.ragnarok.fenrir.api.interfaces.ILikesApi
import dev.ragnarok.fenrir.api.interfaces.IMessagesApi
import dev.ragnarok.fenrir.api.interfaces.INewsfeedApi
import dev.ragnarok.fenrir.api.interfaces.INotificationsApi
import dev.ragnarok.fenrir.api.interfaces.IOtherApi
import dev.ragnarok.fenrir.api.interfaces.IPagesApi
import dev.ragnarok.fenrir.api.interfaces.IPhotosApi
import dev.ragnarok.fenrir.api.interfaces.IPollsApi
import dev.ragnarok.fenrir.api.interfaces.IStatusApi
import dev.ragnarok.fenrir.api.interfaces.IStoreApi
import dev.ragnarok.fenrir.api.interfaces.IStoriesShortVideosApi
import dev.ragnarok.fenrir.api.interfaces.IUsersApi
import dev.ragnarok.fenrir.api.interfaces.IUtilsApi
import dev.ragnarok.fenrir.api.interfaces.IVideoApi
import dev.ragnarok.fenrir.api.interfaces.IWallApi
import dev.ragnarok.fenrir.api.rest.IServiceRest
import dev.ragnarok.fenrir.api.rest.SimplePostHttp
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlowThrowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class VKApies private constructor(
    accountId: Long,
    useCustomToken: Boolean,
    customAccessToken: String?,
    provider: IVKRestProvider
) : IAccountApis {
    private val messagesApi: IMessagesApi
    private val photosApi: IPhotosApi
    private val friendsApi: IFriendsApi
    private val docsApi: IDocsApi
    private val wallApi: IWallApi
    private val newsfeedApi: INewsfeedApi
    private val commentsApi: ICommentsApi
    private val notificationsApi: INotificationsApi
    private val videoApi: IVideoApi
    private val boardApi: IBoardApi
    private val usersApi: IUsersApi
    private val groupsApi: IGroupsApi
    private val accountApi: IAccountApi
    private val databaseApi: IDatabaseApi
    private val audioApi: IAudioApi
    private val statusApi: IStatusApi
    private val likesApi: ILikesApi
    private val pagesApi: IPagesApi
    private val storeApi: IStoreApi
    private val faveApi: IFaveApi
    private val pollsApi: IPollsApi
    private val utilsApi: IUtilsApi
    private val storiesApi: IStoriesShortVideosApi
    private val otherApi: IOtherApi
    override fun messages(): IMessagesApi {
        return messagesApi
    }

    override fun photos(): IPhotosApi {
        return photosApi
    }

    override fun friends(): IFriendsApi {
        return friendsApi
    }

    override fun wall(): IWallApi {
        return wallApi
    }

    override fun docs(): IDocsApi {
        return docsApi
    }

    override fun newsfeed(): INewsfeedApi {
        return newsfeedApi
    }

    override fun comments(): ICommentsApi {
        return commentsApi
    }

    override fun notifications(): INotificationsApi {
        return notificationsApi
    }

    override fun video(): IVideoApi {
        return videoApi
    }

    override fun board(): IBoardApi {
        return boardApi
    }

    override fun users(): IUsersApi {
        return usersApi
    }

    override fun groups(): IGroupsApi {
        return groupsApi
    }

    override fun account(): IAccountApi {
        return accountApi
    }

    override fun database(): IDatabaseApi {
        return databaseApi
    }

    override fun audio(): IAudioApi {
        return audioApi
    }

    override fun status(): IStatusApi {
        return statusApi
    }

    override fun likes(): ILikesApi {
        return likesApi
    }

    override fun stories(): IStoriesShortVideosApi {
        return storiesApi
    }

    override fun pages(): IPagesApi {
        return pagesApi
    }

    override fun store(): IStoreApi {
        return storeApi
    }

    override fun fave(): IFaveApi {
        return faveApi
    }

    override fun polls(): IPollsApi {
        return pollsApi
    }

    override fun utils(): IUtilsApi {
        return utilsApi
    }

    override fun other(): IOtherApi {
        return otherApi
    }

    companion object {
        @SuppressLint("UseSparseArrays")
        private val APIS: MutableMap<Long, VKApies> = HashMap(1)
        fun create(accountId: Long, accessToken: String?, provider: IVKRestProvider): VKApies {
            return VKApies(accountId, true, accessToken, provider)
        }

        @Synchronized
        operator fun get(accountId: Long, provider: IVKRestProvider): VKApies {
            var apies = APIS[accountId]
            if (apies == null) {
                apies = VKApies(accountId, false, null, provider)
                APIS[accountId] = apies
            }
            return apies
        }
    }

    init {
        val restProvider: IServiceProvider = object : IServiceProvider {
            override fun <T : IServiceRest> provideService(
                accountId: Long,
                serviceClass: T,
                vararg tokenTypes: Int
            ): Flow<T> {
                return provideRest(
                    accountId,
                    *tokenTypes
                ).map {
                    serviceClass.addon(it)
                    serviceClass
                }
            }

            fun provideRest(aid: Long, vararg tokenPolicy: Int): Flow<SimplePostHttp> {
                if (aid == ISettings.IAccountsSettings.INVALID_ID) {
                    return toFlowThrowable(UnsupportedOperationException("Please select account!"))
                }
                if (useCustomToken) {
                    customAccessToken?.let {
                        return provider.provideCustomRest(aid, it)
                    }
                }
                val isCommunity = aid < 0
                return if (isCommunity) {
                    when {
                        Utils.intValueIn(TokenType.COMMUNITY, *tokenPolicy) -> {
                            provider.provideNormalRest(aid)
                        }

                        Utils.intValueIn(TokenType.SERVICE, *tokenPolicy) -> {
                            provider.provideServiceRest()
                        }

                        else -> {
                            toFlowThrowable(UnsupportedOperationException("Unsupported account_id: $aid with token_policy: " + tokenPolicy.contentToString()))
                        }
                    }
                } else {
                    when {
                        Utils.intValueIn(TokenType.USER, *tokenPolicy) -> {
                            provider.provideNormalRest(aid)
                        }

                        Utils.intValueIn(TokenType.SERVICE, *tokenPolicy) -> {
                            provider.provideServiceRest()
                        }

                        else -> {
                            toFlowThrowable(
                                UnsupportedOperationException(
                                    "Unsupported account_id: " + aid + " with token_policy: " + tokenPolicy.contentToString()
                                )
                            )
                        }
                    }
                }
            }
        }
        accountApi = AccountApi(accountId, restProvider)
        audioApi = AudioApi(accountId, restProvider)
        boardApi = BoardApi(accountId, restProvider)
        commentsApi = CommentsApi(accountId, restProvider)
        databaseApi = DatabaseApi(accountId, restProvider)
        docsApi = DocsApi(accountId, restProvider)
        faveApi = FaveApi(accountId, restProvider)
        friendsApi = FriendsApi(accountId, restProvider)
        groupsApi = GroupsApi(accountId, restProvider)
        likesApi = LikesApi(accountId, restProvider)
        messagesApi = MessagesApi(accountId, restProvider)
        newsfeedApi = NewsfeedApi(accountId, restProvider)
        notificationsApi = NotificationsApi(accountId, restProvider)
        pagesApi = PagesApi(accountId, restProvider)
        photosApi = PhotosApi(accountId, restProvider)
        pollsApi = PollsApi(accountId, restProvider)
        statusApi = StatusApi(accountId, restProvider)
        storeApi = StoreApi(accountId, restProvider)
        usersApi = UsersApi(accountId, restProvider)
        utilsApi = UtilsApi(accountId, restProvider)
        videoApi = VideoApi(accountId, restProvider)
        wallApi = WallApi(accountId, restProvider)
        storiesApi = StoriesShortVideosApi(accountId, restProvider)
        otherApi = OtherApi(accountId, provider)
    }
}