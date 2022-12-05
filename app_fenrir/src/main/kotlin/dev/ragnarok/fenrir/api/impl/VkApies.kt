package dev.ragnarok.fenrir.api.impl

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.IVkRetrofitProvider
import dev.ragnarok.fenrir.api.RetrofitWrapper
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.*
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.core.Single

internal class VkApies private constructor(
    accountId: Int,
    useCustomToken: Boolean,
    customAccessToken: String?,
    provider: IVkRetrofitProvider
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
        fun create(accountId: Int, accessToken: String?, provider: IVkRetrofitProvider): VkApies {
            return VkApies(accountId, true, accessToken, provider)
        }

        @Synchronized
        operator fun get(accountId: Int, provider: IVkRetrofitProvider): VkApies {
            var apies = APIS[accountId]
            if (apies == null) {
                apies = VkApies(accountId, false, null, provider)
                APIS[accountId] = apies
            }
            return apies
        }
    }

    init {
        val retrofitProvider: IServiceProvider = object : IServiceProvider {
            override fun <T : Any> provideService(
                accountId: Int,
                serviceClass: Class<T>,
                vararg tokenTypes: Int
            ): Single<T> {
                return provideRetrofit(
                    accountId,
                    *tokenTypes
                ).map { retrofit -> retrofit.create(serviceClass) }
            }

            fun provideRetrofit(aid: Int, vararg tokenPolicy: Int): Single<RetrofitWrapper> {
                if (useCustomToken) {
                    return provider.provideCustomRetrofit(aid, customAccessToken!!)
                }
                val isCommunity = aid < 0
                return if (isCommunity) {
                    when {
                        Utils.intValueIn(TokenType.COMMUNITY, *tokenPolicy) -> {
                            provider.provideNormalRetrofit(aid)
                        }
                        Utils.intValueIn(TokenType.SERVICE, *tokenPolicy) -> {
                            provider.provideServiceRetrofit()
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
                            provider.provideNormalRetrofit(aid)
                        }
                        Utils.intValueIn(TokenType.SERVICE, *tokenPolicy) -> {
                            provider.provideServiceRetrofit()
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
        accountApi = AccountApi(accountId, retrofitProvider)
        audioApi = AudioApi(accountId, retrofitProvider)
        boardApi = BoardApi(accountId, retrofitProvider)
        commentsApi = CommentsApi(accountId, retrofitProvider)
        databaseApi = DatabaseApi(accountId, retrofitProvider)
        docsApi = DocsApi(accountId, retrofitProvider)
        faveApi = FaveApi(accountId, retrofitProvider)
        friendsApi = FriendsApi(accountId, retrofitProvider)
        groupsApi = GroupsApi(accountId, retrofitProvider)
        likesApi = LikesApi(accountId, retrofitProvider)
        messagesApi = MessagesApi(accountId, retrofitProvider)
        newsfeedApi = NewsfeedApi(accountId, retrofitProvider)
        notificationsApi = NotificationsApi(accountId, retrofitProvider)
        pagesApi = PagesApi(accountId, retrofitProvider)
        photosApi = PhotosApi(accountId, retrofitProvider)
        pollsApi = PollsApi(accountId, retrofitProvider)
        statusApi = StatusApi(accountId, retrofitProvider)
        storeApi = StoreApi(accountId, retrofitProvider)
        usersApi = UsersApi(accountId, retrofitProvider)
        utilsApi = UtilsApi(accountId, retrofitProvider)
        videoApi = VideoApi(accountId, retrofitProvider)
        wallApi = WallApi(accountId, retrofitProvider)
        otherApi = OtherApi(accountId, provider)
    }
}