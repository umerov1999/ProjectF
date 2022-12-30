package dev.ragnarok.fenrir.api.impl

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.IVkRestProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.*
import dev.ragnarok.fenrir.api.rest.IServiceRest
import dev.ragnarok.fenrir.api.rest.SimplePostHttp
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.core.Single

internal class VkApies private constructor(
    accountId: Int,
    useCustomToken: Boolean,
    customAccessToken: String?,
    provider: IVkRestProvider
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
        private val APIS: MutableMap<Int, VkApies> = HashMap(1)
        fun create(accountId: Int, accessToken: String?, provider: IVkRestProvider): VkApies {
            return VkApies(accountId, true, accessToken, provider)
        }

        @Synchronized
        operator fun get(accountId: Int, provider: IVkRestProvider): VkApies {
            var apies = APIS[accountId]
            if (apies == null) {
                apies = VkApies(accountId, false, null, provider)
                APIS[accountId] = apies
            }
            return apies
        }
    }

    init {
        val restProvider: IServiceProvider = object : IServiceProvider {
            override fun <T : IServiceRest> provideService(
                accountId: Int,
                serviceClass: T,
                vararg tokenTypes: Int
            ): Single<T> {
                return provideRest(
                    accountId,
                    *tokenTypes
                ).map {
                    serviceClass.addon(it)
                    serviceClass
                }
            }

            fun provideRest(aid: Int, vararg tokenPolicy: Int): Single<SimplePostHttp> {
                if (useCustomToken) {
                    return provider.provideCustomRest(aid, customAccessToken!!)
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
                            Single.error(
                                UnsupportedOperationException(
                                    "Unsupported account_id: $aid with token_policy: " + tokenPolicy.contentToString()
                                )
                            )
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
                            Single.error(
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
        otherApi = OtherApi(accountId, provider)
    }
}