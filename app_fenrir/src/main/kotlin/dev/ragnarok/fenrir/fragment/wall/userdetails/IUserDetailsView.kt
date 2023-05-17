package dev.ragnarok.fenrir.fragment.wall.userdetails

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.menu.AdvancedItem

interface IUserDetailsView : IMvpView, IErrorView {
    fun displayData(items: List<AdvancedItem>)
    fun displayToolbarTitle(user: User?)
    fun openOwnerProfile(accountId: Long, ownerId: Long, owner: Owner?)
    fun onPhotosLoaded(photo: Photo)
    fun openPhotoAlbum(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        photos: ArrayList<Photo>,
        position: Int
    )

    fun openChatWith(accountId: Long, messagesOwnerId: Long, peer: Peer)
    fun openPhotoUser(user: User)
}