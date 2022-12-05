package dev.ragnarok.fenrir.api.interfaces

interface IAccountApis {
    fun messages(): IMessagesApi
    fun photos(): IPhotosApi
    fun friends(): IFriendsApi
    fun wall(): IWallApi
    fun docs(): IDocsApi
    fun newsfeed(): INewsfeedApi
    fun comments(): ICommentsApi
    fun notifications(): INotificationsApi
    fun video(): IVideoApi
    fun board(): IBoardApi
    fun users(): IUsersApi
    fun groups(): IGroupsApi
    fun account(): IAccountApi
    fun database(): IDatabaseApi
    fun audio(): IAudioApi
    fun status(): IStatusApi
    fun likes(): ILikesApi
    fun pages(): IPagesApi
    fun store(): IStoreApi
    fun fave(): IFaveApi
    fun polls(): IPollsApi
    fun utils(): IUtilsApi
    fun other(): IOtherApi
}