package dev.ragnarok.fenrir.fragment.base

import android.os.Bundle
import dev.ragnarok.fenrir.domain.ILikesInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.NewsFeedCriteria
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Article
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioArtist
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Link
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.MarketAlbum
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.WallReply
import dev.ragnarok.fenrir.model.WikiPage
import dev.ragnarok.fenrir.util.rxutils.RxUtils

abstract class PlaceSupportPresenter<V>(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<V>(
        accountId,
        savedInstanceState
    ) where V : IMvpView, V : IAttachmentsPlacesView {
    fun fireLinkClick(link: Link) {
        view?.openLink(accountId, link)
    }

    fun fireUrlClick(url: String) {
        view?.openUrl(accountId, url)
    }

    fun fireWikiPageClick(page: WikiPage) {
        view?.openWikiPage(accountId, page)
    }

    fun fireStoryClick(story: Story) {
        view?.openStory(accountId, story)
    }

    fun firePhotoClick(photos: ArrayList<Photo>, index: Int, refresh: Boolean) {
        view?.openSimplePhotoGallery(
            accountId,
            photos,
            index,
            refresh
        )
    }

    open fun firePostClick(post: Post) {
        view?.openPost(accountId, post)
    }

    fun fireDocClick(document: Document) {
        view?.openDocPreview(accountId, document)
    }

    fun fireOwnerClick(ownerId: Long) {
        view?.openOwnerWall(accountId, ownerId)
    }

    fun fireGoToMessagesLookup(message: Message) {
        view?.goToMessagesLookupFWD(
            accountId,
            message.peerId,
            message.originalId
        )
    }

    fun fireGoToMessagesLookup(peerId: Long, msgId: Int) {
        view?.goToMessagesLookupFWD(accountId, peerId, msgId)
    }

    fun fireForwardMessagesClick(messages: ArrayList<Message>) {
        view?.openForwardMessages(accountId, messages)
    }

    fun fireAudioPlayClick(position: Int, apiAudio: ArrayList<Audio>) {
        view?.playAudioList(accountId, position, apiAudio)
    }

    fun fireVideoClick(apiVideo: Video) {
        view?.openVideo(accountId, apiVideo)
    }

    fun fireAudioPlaylistClick(playlist: AudioPlaylist) {
        view?.openAudioPlaylist(accountId, playlist)
    }

    fun fireWallReplyOpen(reply: WallReply) {
        view?.goWallReplyOpen(accountId, reply)
    }

    fun firePollClick(poll: Poll) {
        view?.openPoll(accountId, poll)
    }

    fun fireHashtagClick(hashTag: String) {
        view?.openSearch(
            accountId,
            SearchContentType.NEWS,
            NewsFeedCriteria(hashTag)
        )
    }

    fun fireShareClick(post: Post?) {
        view?.repostPost(accountId, post ?: return)
    }

    fun fireCommentsClick(post: Post?) {
        view?.openComments(
            accountId,
            Commented.from(post ?: return),
            null
        )
    }

    fun firePhotoAlbumClick(album: PhotoAlbum) {
        view?.openPhotoAlbum(accountId, album)
    }

    fun fireMarketAlbumClick(market_album: MarketAlbum) {
        view?.toMarketAlbumOpen(accountId, market_album)
    }

    fun fireMarketClick(market: Market) {
        view?.toMarketOpen(accountId, market)
    }

    fun fireArtistClick(artist: AudioArtist) {
        view?.toArtistOpen(accountId, artist)
    }

    fun fireFaveArticleClick(article: Article) {
        if (!article.isFavorite) {
            appendDisposable(InteractorFactory.createFaveInteractor()
                .addArticle(accountId, article.uRL)
                .fromIOToMain()
                .subscribe(RxUtils.dummy()) { RxUtils.dummy() })
        } else {
            appendDisposable(InteractorFactory.createFaveInteractor()
                .removeArticle(accountId, article.ownerId, article.id)
                .fromIOToMain()
                .subscribe({ RxUtils.dummy() }) { RxUtils.dummy() })
        }
    }

    fun fireCopiesLikesClick(type: String?, ownerId: Long, itemId: Int, filter: String?) {
        if (ILikesInteractor.FILTER_LIKES == filter) {
            view?.goToLikes(accountId, type, ownerId, itemId)
        } else if (ILikesInteractor.FILTER_COPIES == filter) {
            view?.goToReposts(accountId, type, ownerId, itemId)
        }
    }
}