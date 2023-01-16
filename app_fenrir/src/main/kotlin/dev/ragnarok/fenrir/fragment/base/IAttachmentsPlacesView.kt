package dev.ragnarok.fenrir.fragment.base

import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.BaseSearchCriteria
import dev.ragnarok.fenrir.model.*

interface IAttachmentsPlacesView {
    fun openChatWith(accountId: Long, messagesOwnerId: Long, peer: Peer)
    fun openLink(accountId: Long, link: Link)
    fun openUrl(accountId: Long, url: String)
    fun openWikiPage(accountId: Long, page: WikiPage)
    fun openSimplePhotoGallery(
        accountId: Long,
        photos: ArrayList<Photo>,
        index: Int,
        needUpdate: Boolean
    )

    fun openPost(accountId: Long, post: Post)
    fun goToMessagesLookupFWD(accountId: Long, peerId: Long, messageId: Int)
    fun goWallReplyOpen(accountId: Long, reply: WallReply)
    fun openDocPreview(accountId: Long, document: Document)
    fun openOwnerWall(accountId: Long, ownerId: Long)
    fun openForwardMessages(accountId: Long, messages: ArrayList<Message>)
    fun playAudioList(accountId: Long, position: Int, apiAudio: ArrayList<Audio>)
    fun openVideo(accountId: Long, apiVideo: Video)
    fun openHistoryVideo(accountId: Long, stories: ArrayList<Story>, index: Int)
    fun openPoll(accountId: Long, apiPoll: Poll)
    fun openSearch(accountId: Long, @SearchContentType type: Int, criteria: BaseSearchCriteria?)
    fun openComments(accountId: Long, commented: Commented, focusToCommentId: Int?)
    fun goToLikes(accountId: Long, type: String?, ownerId: Long, id: Int)
    fun goToReposts(accountId: Long, type: String?, ownerId: Long, id: Int)
    fun repostPost(accountId: Long, post: Post)
    fun openStory(accountId: Long, story: Story)
    fun openAudioPlaylist(accountId: Long, playlist: AudioPlaylist)
    fun openPhotoAlbum(accountId: Long, album: PhotoAlbum)
    fun toMarketAlbumOpen(accountId: Long, market_album: MarketAlbum)
    fun toMarketOpen(accountId: Long, market: Market)
    fun toArtistOpen(accountId: Long, artist: AudioArtist)
}