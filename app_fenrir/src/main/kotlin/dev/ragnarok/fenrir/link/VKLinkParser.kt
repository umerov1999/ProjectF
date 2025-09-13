package dev.ragnarok.fenrir.link

import dev.ragnarok.fenrir.link.types.AbsLink
import dev.ragnarok.fenrir.link.types.AppLink
import dev.ragnarok.fenrir.link.types.ArticleLink
import dev.ragnarok.fenrir.link.types.ArtistsLink
import dev.ragnarok.fenrir.link.types.AudioPlaylistLink
import dev.ragnarok.fenrir.link.types.AudioTrackLink
import dev.ragnarok.fenrir.link.types.AudiosLink
import dev.ragnarok.fenrir.link.types.AwayLink
import dev.ragnarok.fenrir.link.types.BoardLink
import dev.ragnarok.fenrir.link.types.CatalogV2SectionLink
import dev.ragnarok.fenrir.link.types.DialogLink
import dev.ragnarok.fenrir.link.types.DialogsLink
import dev.ragnarok.fenrir.link.types.DocLink
import dev.ragnarok.fenrir.link.types.DomainLink
import dev.ragnarok.fenrir.link.types.FaveLink
import dev.ragnarok.fenrir.link.types.FeedSearchLink
import dev.ragnarok.fenrir.link.types.MarketLink
import dev.ragnarok.fenrir.link.types.OwnerLink
import dev.ragnarok.fenrir.link.types.PageLink
import dev.ragnarok.fenrir.link.types.PhotoAlbumLink
import dev.ragnarok.fenrir.link.types.PhotoAlbumsLink
import dev.ragnarok.fenrir.link.types.PhotoLink
import dev.ragnarok.fenrir.link.types.PollLink
import dev.ragnarok.fenrir.link.types.StoryLink
import dev.ragnarok.fenrir.link.types.TopicLink
import dev.ragnarok.fenrir.link.types.VideoAlbumLink
import dev.ragnarok.fenrir.link.types.VideoLink
import dev.ragnarok.fenrir.link.types.VideosLink
import dev.ragnarok.fenrir.link.types.WallCommentLink
import dev.ragnarok.fenrir.link.types.WallCommentThreadLink
import dev.ragnarok.fenrir.link.types.WallLink
import dev.ragnarok.fenrir.link.types.WallPostLink
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import java.net.URLDecoder
import java.util.LinkedList
import kotlin.math.abs

object VKLinkParser {
    internal class Patterns {
        val PARSERS: MutableList<IParser> = LinkedList()

        val PATTERN_PHOTOS: Regex = Regex("vk\\.(?:ru|com|me)/photos(-?\\d*)") //+
        val PATTERN_PROFILE_ID: Regex = Regex("vk\\.(?:ru|com|me)/id(\\d+)$") //+
        val PATTERN_DOMAIN: Regex = Regex("vk\\.(?:ru|com|me)/([\\w.]+)")
        val PATTERN_WALL_POST: Regex =
            Regex("vk\\.(?:ru|com|me)/(?:[\\w.\\d]+\\?(?:[\\w=&]+)?w=)?wall(-?\\d*)_(\\d*)")
        val PATTERN_WALL_POST_NOTIFICATION: Regex =
            Regex("vk\\.(?:ru|com|me)/feed\\?\\S*scroll_to=wall(-?\\d*)_(\\d*)")
        val PATTERN_AWAY: Regex = Regex("vk\\.(?:ru|com|me)/away(\\.php)?\\?(.*)")
        val PATTERN_DIALOG: Regex = Regex("vk\\.(?:ru|com|me)/im\\?sel=(c?)(-?\\d+)")
        val PATTERN_DIALOG2: Regex = Regex("vk\\.(?:ru|com|me)/mail\\?\\S*(peer|chat)=(-?\\d+)")
        val PATTERN_ALBUMS: Regex = Regex("vk\\.(?:ru|com|me)/albums(-?\\d+)")
        val PATTERN_AUDIOS: Regex = Regex("vk\\.(?:ru|com|me)/audios(-?\\d+)")
        val PATTERN_ARTIST: Regex = Regex("vk\\.(?:ru|com|me)/artist/([^&]*)")
        val PATTERN_ALBUM: Regex = Regex("vk\\.(?:ru|com|me)/album(-?\\d*)_(-?\\d*)")
        val PATTERN_WALL: Regex = Regex("vk\\.(?:ru|com|me)/wall(-?\\d*)")
        val PATTERN_POLL: Regex = Regex("vk\\.(?:ru|com|me)/poll(-?\\d*)_(\\d*)") //+
        val PATTERN_PHOTO: Regex =
            Regex("vk\\.(?:ru|com|me)/(\\w*)(-)?(\\d)*(\\?z=)?photo(-?\\d*)_(\\d*)") //+
        val PATTERN_VIDEO: Regex = Regex("vk\\.(?:ru|com|me)/video(-?\\d*)_(\\d*)") //+
        val PATTERN_VIDEO_METHOD_2: Regex =
            Regex("vk\\.(?:ru|com|me)/(\\w*)(-)?(\\d)*(\\?z=)?(video|clip)(-?\\d*)_(\\d*)") //+
        val PATTERN_PLAYLIST: Regex =
            Regex("vk\\.(?:ru|com|me)/music/album/(-?\\d*)_(\\d*)_([^&]*)") //+
        val PATTERN_PLAYLIST_ALT: Regex =
            Regex("vk\\.(?:ru|com|me)/.+(?:act=|z=)audio_playlist(-?\\d*)_(\\d*)(?:&access_hash=(\\w+))?")
        val PATTERN_DOC: Regex = Regex("vk\\.(?:ru|com|me)/doc(-?\\d*)_(\\d*)") //+
        val PATTERN_STORY: Regex = Regex("vk\\.(?:ru|com|me)/story(-?\\d*)_(\\d*)") //+
        val PATTERN_TOPIC: Regex = Regex("vk\\.(?:ru|com|me)/topic-(\\d*)_(\\d*)") //+
        val PATTERN_FAVE: Regex = Regex("vk\\.(?:ru|com|me)/fave")
        val PATTERN_GROUP_ID: Regex = Regex("vk\\.(?:ru|com|me)/(club|event|public)(\\d+)$") //+
        val PATTERN_FAVE_WITH_SECTION: Regex = Regex("vk\\.(?:ru|com|me)/fave\\?section=([\\w.]+)")
        val PATTERN_ACCESS_KEY: Regex = Regex("access_key=(\\w+)")
        val PATTERN_VIDEO_ALBUM: Regex =
            Regex("vk\\.(?:ru|com|me)/videos(-?\\d*)[?]section=album_(\\d*)")
        val PATTERN_VIDEOS_OWNER: Regex = Regex("vk\\.(?:ru|com|me)/videos(-?\\d*)")

        //vk.ru/wall-2345345_7834545?reply=15345346
        val PATTERN_WALL_POST_COMMENT: Regex =
            Regex("vk\\.(?:ru|com|me)/wall(-?\\d*)_(\\d*)\\?reply=(\\d*)")
        val PATTERN_WALL_POST_COMMENT_THREAD: Regex =
            Regex("vk\\.(?:ru|com|me)/wall(-?\\d*)_(\\d*)\\?reply=(\\d*)&thread=(\\d*)")
        val PATTERN_BOARD: Regex = Regex("vk\\.(?:ru|com|me)/board(\\d+)")
        val PATTERN_FEED_SEARCH: Regex = Regex("vk\\.(?:ru|com|me)/feed\\?q=([^&]*)&section=search")
        val PATTERN_FENRIR_TRACK: Regex = Regex("vk\\.(?:ru|com|me)/audio/(-?\\d*)_(\\d*)") //+
        val PATTERN_CATALOG_V2_SECTION: Regex =
            Regex("vk\\.(?:ru|com|me)/audio\\?section=([\\w.]+)") //+
        val PATTERN_FENRIR_SERVER_TRACK_HASH: Regex = Regex("hash=([^&]*)")

        val PATTERN_APP: Regex = Regex("vk\\.(?:ru|com|me)/app(-?\\d*)")
        val PATTERN_ARTICLE: Regex = Regex("vk\\.(?:ru|com|me)/@([^&]*)")

        val PATTERN_MARKET: Regex = Regex("vk\\.(?:ru|com|me)/market(-?\\d*)")

        val COMMON_PATTERN_VK_DOMAIN: Regex =
            Regex("(vk\\.(?:ru|com|me))")
        val COMMON_PATTERN_WALL: Regex =
            Regex("(vk\\.(?:ru|com|me)/wall)")
        val COMMON_PATTERN_BAD: Regex =
            Regex("((?:vk\\.(?:ru|com|me)/?$)|(?:vk\\.(?:ru|com|me)/(?:activation|login|images?(?:$|\\?|/)|search?(?:$|\\?|/)|friends?(?:$|\\?|/)|support?(?:$|\\?|/)|restore?(?:$|\\?|/)|page$|bugs?(?:$|\\?|/)|dev?(?:$|\\?|/)|note\\d.*$))|(?:login\\.vk\\.(?:ru|com|me)))")
        val COMMON_PATTERN_FEED: Regex =
            Regex("(vk\\.(?:ru|com|me)/feed)")
        val COMMON_PATTERN_MAIL: Regex =
            Regex("(vk\\.(?:ru|com|me)/(?:im|mail))")
        val COMMON_PATTERN_PAGE: Regex =
            Regex("(vk\\.(?:ru|com|me)/(?:pages|page))")

        init {
            PARSERS.add(object : IParser {
                override fun parse(string: String?): Optional<AbsLink> {
                    string ?: return empty()
                    return try {
                        patterns.PATTERN_FEED_SEARCH.find(string)?.let {
                            val q = URLDecoder.decode(it.groupValues.getOrNull(1), "UTF-8")
                            return wrap(FeedSearchLink(q))
                        } ?: empty()
                    } catch (_: Exception) {
                        empty()
                    }
                }
            })
        }
    }

    internal val patterns: Patterns by lazy {
        Patterns()
    }

    fun parse(string: String): AbsLink? {
        if (!patterns.COMMON_PATTERN_VK_DOMAIN.containsMatchIn(string) || patterns.COMMON_PATTERN_BAD.containsMatchIn(
                string
            )
        ) {
            return null
        }
        if (patterns.COMMON_PATTERN_FEED.containsMatchIn(string) && !string.contains("z=photo") && !string.contains(
                "w=wall"
            ) && !string.contains(
                "w=page"
            ) && !string.contains("q=") && !string.contains("scroll_to=wall")
        ) {
            return null
        }
        var vkLink = parseWallCommentThreadLink(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseWallCommentLink(string)
        if (vkLink != null) {
            return vkLink
        }

        for (parser in patterns.PARSERS) {
            try {
                val link = parser.parse(string).get()
                if (link != null) {
                    return link
                }
            } catch (_: Exception) {
            }
        }
        vkLink = parseBoard(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parsePage(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parsePhoto(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseAlbum(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseProfileById(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseGroupById(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseTopic(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseWallPost(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseWallPostNotif(string)
        if (vkLink != null) {
            return vkLink
        }

        /*    vkLink = VkLinkParser.parseAway(string);
        if (vkLink != null) {
            return vkLink;
        }
    */if (string.contains("/im?sel")) {
            vkLink = parseDialog(string)
            if (vkLink != null) {
                return vkLink
            }
        }
        vkLink = parseAlbums(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseAway(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseWall(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parsePoll(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseVideoAlbum(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseVideo(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseAudioPlaylistAlt(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parsePlaylist(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseAudios(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseArtists(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseAudioTrack(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseDoc(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseStory(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parsePhotos(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseApps(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseMarkets(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseArticles(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseFave(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseCatalogV2(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseDomain(string)
        return vkLink
    }

    private fun parseBoard(string: String): AbsLink? {
        return try {
            patterns.PATTERN_BOARD.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { r ->
                    BoardLink(r.toLong())
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAlbum(string: String): AbsLink? {
        return try {
            patterns.PATTERN_ALBUM.find(string)?.let {
                val ownerId = it.groupValues.getOrNull(1)
                var albumId = it.groupValues.getOrNull(2)
                if (albumId == "0") {
                    albumId = (-6).toString()
                }
                if (albumId == "00") {
                    albumId = (-7).toString()
                }
                if (albumId == "000") {
                    albumId = (-15).toString()
                }
                ownerId?.let { o ->
                    albumId?.let { a ->
                        PhotoAlbumLink(o.toLong(), a.toInt())
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAlbums(string: String): AbsLink? {
        return try {
            patterns.PATTERN_ALBUMS.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { r ->
                    PhotoAlbumsLink(r.toLong())
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAway(string: String): AbsLink? {
        return if (!patterns.PATTERN_AWAY.containsMatchIn(string)) {
            null
        } else AwayLink(string)
    }

    private fun parseDialog(string: String): AbsLink? {
        try {
            patterns.PATTERN_DIALOG.find(string)?.let {
                val chat = it.groupValues.getOrNull(1)
                val id = it.groupValues.getOrNull(2)?.toLong()
                val isChat = chat.nonNullNoEmpty()
                return if (id == null) {
                    DialogsLink()
                } else {
                    DialogLink(if (isChat) Peer.fromChatId(id) else Peer.fromOwnerId(id))
                }
            }

            patterns.PATTERN_DIALOG2.find(string)?.let {
                val chat = it.groupValues.getOrNull(1)
                val id = it.groupValues.getOrNull(2)?.toLong()
                val isChat = "chat" == chat
                return if (id == null) {
                    DialogsLink()
                } else {
                    DialogLink(if (isChat) Peer.fromChatId(id) else Peer.fromOwnerId(id))
                }
            }

            if (patterns.COMMON_PATTERN_MAIL.containsMatchIn(string)) {
                return DialogsLink()
            }
        } catch (_: Exception) {
            return null
        }
        return null
    }

    private fun parseDomain(string: String): AbsLink? {
        return try {
            patterns.PATTERN_DOMAIN.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { r ->
                    DomainLink(string, r)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseGroupById(string: String): AbsLink? {
        return try {
            patterns.PATTERN_GROUP_ID.find(string)?.let {
                it.groupValues.getOrNull(2)?.let { r ->
                    OwnerLink(-abs(r.toLong()))
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePage(string: String): AbsLink? {
        return if (patterns.COMMON_PATTERN_PAGE.containsMatchIn(string) && string.contains("w=page")) {
            PageLink(string.replace("m.vk.com/", "vk.com/").replace("m.vk.ru/", "vk.ru/"))
        } else {
            null
        }
    }

    private fun parseAccessKey(string: String): String? {
        return try {
            patterns.PATTERN_ACCESS_KEY.find(string)?.groupValues?.getOrNull(1)
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePhoto(string: String): AbsLink? {
        return try {
            patterns.PATTERN_PHOTO.find(string)?.let {
                it.groupValues.getOrNull(5)?.let { r ->
                    it.groupValues.getOrNull(6)?.let { n ->
                        PhotoLink(n.toInt(), r.toLong(), parseAccessKey(string))
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePhotos(string: String): AbsLink? {
        return try {
            patterns.PATTERN_PHOTOS.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { r ->
                    PhotoAlbumsLink(r.toLong())
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseApps(string: String): AbsLink? {
        return try {
            patterns.PATTERN_APP.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { r ->
                    AppLink(string, r.toInt())
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseMarkets(string: String): AbsLink? {
        return try {
            patterns.PATTERN_MARKET.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { r ->
                    MarketLink(r.toLong())
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseArticles(string: String): AbsLink? {
        return try {
            patterns.PATTERN_ARTICLE.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { r ->
                    ArticleLink(string, r)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseProfileById(string: String): AbsLink? {
        return try {
            patterns.PATTERN_PROFILE_ID.find(string)?.let {
                it.groupValues.getOrNull(2)?.let { r ->
                    OwnerLink(r.toLong())
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTopic(string: String): AbsLink? {
        return try {
            patterns.PATTERN_TOPIC.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { i ->
                        TopicLink(i.toInt(), o.toLong())
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseVideoAlbum(string: String): AbsLink? {
        try {
            patterns.PATTERN_VIDEO_ALBUM.find(string)?.let {
                return it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { i ->
                        VideoAlbumLink(o.toLong(), i.toInt())
                    }
                }
            }

            patterns.PATTERN_VIDEOS_OWNER.find(string)?.let {
                return it.groupValues.getOrNull(1)?.let { r ->
                    VideosLink(r.toLong())
                }
            }
        } catch (_: Exception) {
            return null
        }
        return null
    }

    private fun parseVideo(string: String): AbsLink? {
        try {
            patterns.PATTERN_VIDEO.find(string)?.let {
                return it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { i ->
                        VideoLink(o.toLong(), i.toInt(), parseAccessKey(string))
                    }
                }
            }

            patterns.PATTERN_VIDEO_METHOD_2.find(string)?.let {
                return it.groupValues.getOrNull(6)?.let { o ->
                    it.groupValues.getOrNull(7)?.let { i ->
                        VideoLink(o.toLong(), i.toInt(), parseAccessKey(string))
                    }
                }
            }
        } catch (_: Exception) {
            return null
        }
        return null
    }

    fun parseLocalServerURL(string: String?): String? {
        string ?: return null
        return try {
            patterns.PATTERN_FENRIR_SERVER_TRACK_HASH.find(string)?.groupValues?.getOrNull(1)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAudioTrack(string: String): AbsLink? {
        return try {
            patterns.PATTERN_FENRIR_TRACK.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { i ->
                        AudioTrackLink(o.toLong(), i.toInt())
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAudioPlaylistAlt(string: String): AbsLink? {
        return try {
            patterns.PATTERN_PLAYLIST_ALT.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { i ->
                        AudioPlaylistLink(o.toLong(), i.toInt(), it.groupValues.getOrNull(3))
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePlaylist(string: String): AbsLink? {
        return try {
            patterns.PATTERN_PLAYLIST.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { i ->
                        AudioPlaylistLink(o.toLong(), i.toInt(), it.groupValues.getOrNull(3))
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseCatalogV2(string: String): AbsLink? {
        return if (patterns.PATTERN_CATALOG_V2_SECTION.containsMatchIn(string)) {
            return CatalogV2SectionLink(string)
        } else null
    }

    private fun parseFave(string: String): AbsLink? {
        val matcherWithSection = patterns.PATTERN_FAVE_WITH_SECTION.find(string)
        if (matcherWithSection != null) {
            return FaveLink(matcherWithSection.groupValues.getOrNull(1))
        }
        return if (patterns.PATTERN_FAVE.containsMatchIn(string)) {
            FaveLink()
        } else null
    }

    private fun parseAudios(string: String): AbsLink? {
        return try {
            patterns.PATTERN_AUDIOS.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { r ->
                    AudiosLink(r.toLong())
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseArtists(string: String): AbsLink? {
        return try {
            patterns.PATTERN_ARTIST.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { r ->
                    ArtistsLink(r)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDoc(string: String): AbsLink? {
        return try {
            patterns.PATTERN_DOC.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { i ->
                        DocLink(o.toLong(), i.toInt(), parseAccessKey(string))
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseStory(string: String): AbsLink? {
        return try {
            patterns.PATTERN_STORY.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { i ->
                        StoryLink(o.toLong(), i.toInt(), parseAccessKey(string))
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseWall(string: String): AbsLink? {
        return try {
            patterns.PATTERN_WALL.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { r ->
                    WallLink(r.toLong())
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePoll(string: String): AbsLink? {
        return try {
            patterns.PATTERN_POLL.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { i ->
                        PollLink(o.toLong(), i.toInt())
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseWallCommentThreadLink(string: String): AbsLink? {
        return try {
            patterns.PATTERN_WALL_POST_COMMENT_THREAD.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { p ->
                        it.groupValues.getOrNull(3)?.let { c ->
                            it.groupValues.getOrNull(4)?.let { t ->
                                WallCommentThreadLink(
                                    o.toLong(),
                                    p.toInt(),
                                    c.toInt(),
                                    t.toInt()
                                ).validOrNull
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseWallCommentLink(string: String): AbsLink? {
        return try {
            patterns.PATTERN_WALL_POST_COMMENT.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { p ->
                        it.groupValues.getOrNull(3)?.let { c ->
                            WallCommentLink(
                                o.toLong(),
                                p.toInt(),
                                c.toInt()
                            ).validOrNull
                        }
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseWallPost(string: String): AbsLink? {
        return try {
            patterns.PATTERN_WALL_POST.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { i ->
                        WallPostLink(o.toLong(), i.toInt())
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseWallPostNotif(string: String): AbsLink? {
        return try {
            patterns.PATTERN_WALL_POST_NOTIFICATION.find(string)?.let {
                it.groupValues.getOrNull(1)?.let { o ->
                    it.groupValues.getOrNull(2)?.let { i ->
                        WallPostLink(o.toLong(), i.toInt())
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    interface IParser {
        @Throws(Exception::class)
        fun parse(string: String?): Optional<AbsLink>
    }
}
