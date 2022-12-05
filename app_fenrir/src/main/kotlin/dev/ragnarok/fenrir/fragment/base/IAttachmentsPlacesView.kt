package dev.ragnarok.fenrir.fragment.base

import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.BaseSearchCriteria
import dev.ragnarok.fenrir.model.*

interface IAttachmentsPlacesView {
    fun openChatWith(accountId: Int, messagesOwnerId: Int, peer: Peer)
    fun openLink(accountId: Int, link: Link)
    fun openUrl(accountId: Int, url: String)
    fun openWikiPage(accountId: Int, page: WikiPage)
    fun openSimplePhotoGallery(
        accountId: Int,
        photos: ArrayList<Photo>,
        index: Int,
        needUpdate: Boolean
    )

    fun openPost(accountId: Int, post: Post)
    fun goToMessagesLookupFWD(accountId: Int, peerId: Int, messageId: Int)
    fun goWallReplyOpen(accountId: Int, reply: WallReply)
    fun openDocPreview(accountId: Int, document: Document)
    fun openOwnerWall(accountId: Int, ownerId: Int)
    fun openForwardMessages(accountId: Int, messages: ArrayList<Message>)
    fun playAudioList(accountId: Int, position: Int, apiAudio: ArrayList<Audio>)
    fun openVideo(accountId: Int, apiVideo: Video)
    fun openHistoryVideo(accountId: Int, stories: ArrayList<Story>, index: Int)
    fun openPoll(accountId: Int, apiPoll: Poll)
    fun openSearch(accountId: Int, @SearchContentType type: Int, criteria: BaseSearchCriteria?)
    fun openComments(accountId: Int, commented: Commented, focusToCommentId: Int?)
    fun goToLikes(accountId: Int, type: String?, ownerId: Int, id: Int)
    fun goToReposts(accountId: Int, type: String?, ownerId: Int, id: Int)
    fun repostPost(accountId: Int, post: Post)
    fun openStory(accountId: Int, story: Story)
    fun openAudioPlaylist(accountId: Int, playlist: AudioPlaylist)
    fun openPhotoAlbum(accountId: Int, album: PhotoAlbum)
    fun toMarketAlbumOpen(accountId: Int, market_album: MarketAlbum)
    fun toMarketOpen(accountId: Int, market: Market)
    fun toArtistOpen(accountId: Int, artist: AudioArtist)
}