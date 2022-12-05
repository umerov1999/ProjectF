package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.crypt.KeyLocationPolicy

interface IStorages {
    fun tempStore(): ITempDataStorage
    fun videoAlbums(): IVideoAlbumsStorage
    fun videos(): IVideoStorage
    fun attachments(): IAttachmentsStorage
    fun keys(@KeyLocationPolicy policy: Int): IKeysStorage
    fun localMedia(): ILocalMediaStorage
    fun notifications(): IFeedbackStorage
    fun dialogs(): IDialogsStorage
    fun messages(): IMessagesStorage
    fun wall(): IWallStorage
    fun fave(): IFaveStorage
    fun photos(): IPhotosStorage
    fun relativeship(): IRelativeshipStorage
    fun feed(): IFeedStorage
    fun owners(): IOwnersStorage
    fun comments(): ICommentsStorage
    fun photoAlbums(): IPhotoAlbumsStorage
    fun topics(): ITopicsStore
    fun docs(): IDocsStorage
    fun stickers(): IStickersStorage
    fun database(): IDatabaseStore
}