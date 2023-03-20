package dev.ragnarok.fenrir.link

import dev.ragnarok.fenrir.link.types.*
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import java.net.URLDecoder
import java.util.LinkedList
import java.util.regex.Pattern
import kotlin.math.abs

object VKLinkParser {
    internal class Patterns {
        val PARSERS: MutableList<IParser> = LinkedList()

        val PATTERN_PHOTOS: Pattern = Pattern.compile("vk\\.com/photos(-?\\d*)") //+
        val PATTERN_PROFILE_ID: Pattern = Pattern.compile("(m\\.)?vk\\.com/id(\\d+)$") //+
        val PATTERN_DOMAIN: Pattern = Pattern.compile("vk\\.com/([\\w.]+)")
        val PATTERN_WALL_POST: Pattern =
            Pattern.compile("vk.com/(?:[\\w.\\d]+\\?(?:[\\w=&]+)?w=)?wall(-?\\d*)_(\\d*)")
        val PATTERN_WALL_POST_NOTIFICATION: Pattern =
            Pattern.compile("vk\\.com/feed\\?\\S*scroll_to=wall(-?\\d*)_(\\d*)")
        val PATTERN_AWAY: Pattern = Pattern.compile("vk\\.com/away(\\.php)?\\?(.*)")
        val PATTERN_DIALOG: Pattern = Pattern.compile("vk\\.com/im\\?sel=(c?)(-?\\d+)")
        val PATTERN_DIALOG2: Pattern = Pattern.compile("vk\\.com/mail\\?\\S*(peer|chat)=(-?\\d+)")
        val PATTERN_ALBUMS: Pattern = Pattern.compile("vk\\.com/albums(-?\\d+)")
        val PATTERN_AUDIOS: Pattern = Pattern.compile("vk\\.com/audios(-?\\d+)")
        val PATTERN_ARTIST: Pattern = Pattern.compile("vk\\.com/artist/([^&]*)")
        val PATTERN_ALBUM: Pattern = Pattern.compile("vk\\.com/album(-?\\d*)_(-?\\d*)")
        val PATTERN_WALL: Pattern = Pattern.compile("vk\\.com/wall(-?\\d*)")
        val PATTERN_POLL: Pattern = Pattern.compile("vk\\.com/poll(-?\\d*)_(\\d*)") //+
        val PATTERN_PHOTO: Pattern =
            Pattern.compile("vk\\.com/(\\w*)(-)?(\\d)*(\\?z=)?photo(-?\\d*)_(\\d*)") //+
        val PATTERN_VIDEO: Pattern = Pattern.compile("vk\\.com/video(-?\\d*)_(\\d*)") //+
        val PATTERN_VIDEO_METHOD_2: Pattern =
            Pattern.compile("vk\\.com/(\\w*)(-)?(\\d)*(\\?z=)?(video|clip)(-?\\d*)_(\\d*)") //+
        val PATTERN_PLAYLIST: Pattern =
            Pattern.compile("vk\\.com/music/album/(-?\\d*)_(\\d*)_([^&]*)") //+
        val PATTERN_PLAYLIST_ALT: Pattern =
            Pattern.compile("vk\\.com/.+(?:act=|z=)audio_playlist(-?\\d*)_(\\d*)(?:&access_hash=(\\w+))?")
        val PATTERN_DOC: Pattern = Pattern.compile("vk\\.com/doc(-?\\d*)_(\\d*)") //+
        val PATTERN_TOPIC: Pattern = Pattern.compile("vk\\.com/topic-(\\d*)_(\\d*)") //+
        val PATTERN_FAVE: Pattern = Pattern.compile("vk\\.com/fave")
        val PATTERN_GROUP_ID: Pattern = Pattern.compile("vk\\.com/(club|event|public)(\\d+)$") //+
        val PATTERN_FAVE_WITH_SECTION: Pattern =
            Pattern.compile("vk\\.com/fave\\?section=([\\w.]+)")
        val PATTERN_ACCESS_KEY: Pattern = Pattern.compile("access_key=(\\w+)")
        val PATTERN_VIDEO_ALBUM: Pattern =
            Pattern.compile("vk\\.com/videos(-?\\d*)[?]section=album_(\\d*)")
        val PATTERN_VIDEOS_OWNER: Pattern =
            Pattern.compile("vk\\.com/videos(-?\\d*)")

        //vk.com/wall-2345345_7834545?reply=15345346
        val PATTERN_WALL_POST_COMMENT: Pattern =
            Pattern.compile("vk\\.com/wall(-?\\d*)_(\\d*)\\?reply=(\\d*)")
        val PATTERN_WALL_POST_COMMENT_THREAD: Pattern =
            Pattern.compile("vk\\.com/wall(-?\\d*)_(\\d*)\\?reply=(\\d*)&thread=(\\d*)")
        val PATTERN_BOARD: Pattern = Pattern.compile("vk\\.com/board(\\d+)")
        val PATTERN_FEED_SEARCH: Pattern =
            Pattern.compile("vk\\.com/feed\\?q=([^&]*)&section=search")
        val PATTERN_FENRIR_TRACK: Pattern = Pattern.compile("vk\\.com/audio/(-?\\d*)_(\\d*)") //+
        val PATTERN_CATALOG_V2_SECTION: Pattern =
            Pattern.compile("vk\\.com/audio\\?section=([\\w.]+)") //+
        val PATTERN_FENRIR_SERVER_TRACK_HASH: Pattern = Pattern.compile("hash=([^&]*)")

        val PATTERN_APP: Pattern =
            Pattern.compile("vk\\.com/app(-?\\d*)")
        val PATTERN_ARTICLE: Pattern =
            Pattern.compile("vk\\.com/@([^&]*)")

        init {
            PARSERS.add(object : IParser {
                override fun parse(string: String?): Optional<AbsLink> {
                    val matcher = string?.let { patterns.PATTERN_FEED_SEARCH.matcher(it) }
                    if (matcher?.find() == true) {
                        val q = URLDecoder.decode(matcher.group(1), "UTF-8")
                        return wrap(FeedSearchLink(q))
                    }
                    return empty()
                }
            })
        }
    }

    internal val patterns: Patterns by lazy {
        Patterns()
    }

    fun parse(string: String): AbsLink? {
        if (!string.contains("vk.com")) {
            return null
        }
        if (string == "vk.com" || string == "m.vk.com" || string.contains("login.vk.com") || string.contains(
                "vk.com/login"
            )
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
        if (string.contains("vk.com/images")) {
            return null
        }
        if (string.contains("vk.com/search")) {
            return null
        }
        if (string.contains("vk.com/feed") && !string.contains("?z=photo") && !string.contains("w=wall") && !string.contains(
                "?w=page"
            ) && !string.contains("?q=") && !string.contains("scroll_to=wall")
        ) {
            return null
        }
        if (string.contains("vk.com/friends")) {
            return null
        }
        if (string.endsWith("vk.com/support")) {
            return null
        }
        if (string.endsWith("vk.com/restore")) {
            return null
        }
        if (string.contains("vk.com/restore?")) {
            return null
        }
        if (string.endsWith("vk.com/page")) {
            return null
        }
        if (string.contains("vk.com/login")) {
            return null
        }
        if (string.contains("vk.com/bugs")) {
            return null
        }
        if (Pattern.matches(".*vk.com/note\\d.*", string)) {
            return null
        }
        if (string.contains("vk.com/dev/")) {
            return null
        }
        if (string.contains("vk.com/wall") && string.contains("?reply=")) {
            return null
        }
        for (parser in patterns.PARSERS) {
            try {
                val link = parser.parse(string).get()
                if (link != null) {
                    return link
                }
            } catch (ignored: Exception) {
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
        vkLink = parsePhotos(string)
        if (vkLink != null) {
            return vkLink
        }
        vkLink = parseApps(string)
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
        val matcher = patterns.PATTERN_BOARD.matcher(string)
        try {
            if (matcher.find()) {
                val groupId = matcher.group(1)
                if (groupId != null) {
                    return BoardLink(groupId.toLong())
                }
            }
        } catch (ignored: Exception) {
        }
        return null
    }

    private fun parseAlbum(string: String): AbsLink? {
        val matcher = patterns.PATTERN_ALBUM.matcher(string)
        if (!matcher.find()) {
            return null
        }
        val ownerId = matcher.group(1)
        var albumId = matcher.group(2)
        if (albumId == "0") {
            albumId = (-6).toString()
        }
        if (albumId == "00") {
            albumId = (-7).toString()
        }
        if (albumId == "000") {
            albumId = (-15).toString()
        }
        if (ownerId != null) {
            if (albumId != null) {
                return PhotoAlbumLink(ownerId.toLong(), albumId.toInt())
            }
        }
        return null
    }

    private fun parseAlbums(string: String): AbsLink? {
        val matcher = patterns.PATTERN_ALBUMS.matcher(string)
        if (!matcher.find()) {
            return null
        }
        val ownerId = matcher.group(1)
        if (ownerId != null) {
            return PhotoAlbumsLink(ownerId.toLong())
        }
        return null
    }

    private fun parseAway(string: String): AbsLink? {
        return if (!patterns.PATTERN_AWAY.matcher(string).find()) {
            null
        } else AwayLink(string)
    }

    private fun parseDialog(string: String): AbsLink? {
        var matcher = patterns.PATTERN_DIALOG.matcher(string)
        try {
            if (matcher.find()) {
                val chat = matcher.group(1)
                val id = matcher.group(2)?.toLong()
                val isChat = chat.nonNullNoEmpty()
                return (if (isChat) id?.let { Peer.fromChatId(it) } else id?.let {
                    Peer.fromOwnerId(
                        it
                    )
                })?.let {
                    DialogLink(
                        it
                    )
                }
            } else {
                matcher = patterns.PATTERN_DIALOG2.matcher(string)
                if (matcher.find()) {
                    val chat = matcher.group(1)
                    val id = matcher.group(2)?.toLong()
                    val isChat = "chat" == chat
                    return (if (isChat) id?.let { Peer.fromChatId(it) } else id?.let {
                        Peer.fromOwnerId(
                            it
                        )
                    })?.let {
                        DialogLink(
                            it
                        )
                    }
                }
            }
        } catch (ignored: Exception) {
            if (string.contains("vk.com/im") || string.contains("vk.com/mail")) {
                return DialogsLink()
            }
        }
        if (string.contains("vk.com/im") || string.contains("vk.com/mail")) {
            return DialogsLink()
        }
        return null
    }

    private fun parseDomain(string: String): AbsLink? {
        val matcher = patterns.PATTERN_DOMAIN.matcher(string)
        return if (!matcher.find()) {
            null
        } else matcher.group(1)?.let { DomainLink(string, it) }
    }

    private fun parseGroupById(string: String): AbsLink? {
        val matcher = patterns.PATTERN_GROUP_ID.matcher(string)
        try {
            if (matcher.find()) {
                matcher.group(2)?.let {
                    return OwnerLink(-abs(it.toLong()))
                }
            }
        } catch (ignored: Exception) {
        }
        return null
    }

    private fun parsePage(string: String): AbsLink? {
        return if (string.contains("vk.com/pages")
            || string.contains("vk.com/page")
            || string.contains("vk.com") && string.contains("w=page")
        ) {
            PageLink(string.replace("m.vk.com/", "vk.com/"))
        } else null
    }

    private fun parseAccessKey(string: String): String? {
        val matcher = patterns.PATTERN_ACCESS_KEY.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(1)
            }
        } catch (ignored: Exception) {
        }
        return null
    }

    private fun parsePhoto(string: String): AbsLink? {
        val matcher = patterns.PATTERN_PHOTO.matcher(string)
        try {
            if (matcher.find()) {
                val ownerId = matcher.group(5)?.toLong()
                val photoId = matcher.group(6)?.toInt()
                photoId?.let {
                    if (ownerId != null) {
                        return PhotoLink(it, ownerId, parseAccessKey(string))
                    }
                    return null
                }
            }
        } catch (ignored: Exception) {
        }
        return null
    }

    private fun parsePhotos(string: String): AbsLink? {
        val matcher = patterns.PATTERN_PHOTOS.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(1)?.let { PhotoAlbumsLink(it.toLong()) }
            }
        } catch (ignored: Exception) {
        }
        return null
    }

    private fun parseApps(string: String): AbsLink? {
        val matcher = patterns.PATTERN_APP.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(1)?.let { AppLink(string, it.toInt()) }
            }
        } catch (ignored: Exception) {
        }
        return null
    }

    private fun parseArticles(string: String): AbsLink? {
        val matcher = patterns.PATTERN_ARTICLE.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(1)?.let { ArticleLink(string, it) }
            }
        } catch (ignored: Exception) {
        }
        return null
    }

    private fun parseProfileById(string: String): AbsLink? {
        val matcher = patterns.PATTERN_PROFILE_ID.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(2)?.let { OwnerLink(it.toLong()) }
            }
        } catch (ignored: Exception) {
        }
        return null
    }

    private fun parseTopic(string: String): AbsLink? {
        val matcher = patterns.PATTERN_TOPIC.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(2)?.let {
                    matcher.group(1)
                        ?.let { it1 -> TopicLink(it.toInt(), it1.toLong()) }
                }
            }
        } catch (ignored: Exception) {
        }
        return null
    }

    private fun parseVideoAlbum(string: String): AbsLink? {
        var matcher = patterns.PATTERN_VIDEO_ALBUM.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(1)
                    ?.let {
                        matcher.group(2)?.let { it1 -> VideoAlbumLink(it.toLong(), it1.toInt()) }
                    }
            }
        } catch (ignored: NumberFormatException) {
        }
        matcher = patterns.PATTERN_VIDEOS_OWNER.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(1)
                    ?.let {
                        VideosLink(it.toLong())
                    }
            }
        } catch (ignored: NumberFormatException) {
        }
        return null
    }

    private fun parseVideo(string: String): AbsLink? {
        var matcher = patterns.PATTERN_VIDEO.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(1)?.let {
                    matcher.group(2)?.let { it1 ->
                        VideoLink(
                            it.toLong(),
                            it1.toInt(),
                            parseAccessKey(string)
                        )
                    }
                }
            }
        } catch (ignored: NumberFormatException) {
        }
        matcher = patterns.PATTERN_VIDEO_METHOD_2.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(6)?.let {
                    matcher.group(7)?.let { it1 ->
                        VideoLink(
                            it.toLong(),
                            it1.toInt(),
                            parseAccessKey(string)
                        )
                    }
                }
            }
        } catch (ignored: NumberFormatException) {
        }
        return null
    }

    fun parseLocalServerURL(string: String?): String? {
        val matcher = string?.let { patterns.PATTERN_FENRIR_SERVER_TRACK_HASH.matcher(it) }
        try {
            if (matcher?.find() == true) {
                return matcher.group(1)
            }
        } catch (ignored: NumberFormatException) {
        }
        return null
    }

    private fun parseAudioTrack(string: String): AbsLink? {
        val matcher = patterns.PATTERN_FENRIR_TRACK.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(1)
                    ?.let {
                        matcher.group(2)?.let { it1 -> AudioTrackLink(it.toLong(), it1.toInt()) }
                    }
            }
        } catch (ignored: NumberFormatException) {
        }
        return null
    }

    private fun parseAudioPlaylistAlt(string: String): AbsLink? {
        val matcher = patterns.PATTERN_PLAYLIST_ALT.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(1)?.let {
                    matcher.group(2)?.let { it1 ->
                        AudioPlaylistLink(
                            it.toLong(),
                            it1.toInt(),
                            matcher.group(3)
                        )
                    }
                }
            }
        } catch (ignored: NumberFormatException) {
        }
        return null
    }

    private fun parsePlaylist(string: String): AbsLink? {
        val matcher = patterns.PATTERN_PLAYLIST.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(1)?.let {
                    matcher.group(2)?.let { it1 ->
                        AudioPlaylistLink(
                            it.toLong(),
                            it1.toInt(),
                            matcher.group(3)
                        )
                    }
                }
            }
        } catch (ignored: NumberFormatException) {
        }
        return null
    }

    private fun parseCatalogV2(string: String): AbsLink? {
        val matcherWithSection = patterns.PATTERN_CATALOG_V2_SECTION.matcher(string)
        return if (matcherWithSection.find()) {
            return CatalogV2SectionLink(string)
        } else null
    }

    private fun parseFave(string: String): AbsLink? {
        val matcherWithSection = patterns.PATTERN_FAVE_WITH_SECTION.matcher(string)
        val matcher = patterns.PATTERN_FAVE.matcher(string)
        if (matcherWithSection.find()) {
            return FaveLink(matcherWithSection.group(1))
        }
        return if (matcher.find()) {
            FaveLink()
        } else null
    }

    private fun parseAudios(string: String): AbsLink? {
        val matcher = patterns.PATTERN_AUDIOS.matcher(string)
        return if (!matcher.find()) {
            null
        } else matcher.group(1)?.let { AudiosLink(it.toLong()) }
    }

    private fun parseArtists(string: String): AbsLink? {
        val matcher = patterns.PATTERN_ARTIST.matcher(string)
        return if (!matcher.find()) {
            null
        } else matcher.group(1)?.let { ArtistsLink(it) }
    }

    private fun parseDoc(string: String): AbsLink? {
        val matcher = patterns.PATTERN_DOC.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(1)?.let {
                    matcher.group(2)?.let { it1 ->
                        DocLink(
                            it.toLong(),
                            it1.toInt(),
                            parseAccessKey(string)
                        )
                    }
                }
            }
        } catch (ignored: Exception) {
        }
        return null
    }

    private fun parseWall(string: String): AbsLink? {
        val matcher = patterns.PATTERN_WALL.matcher(string)
        return if (!matcher.find()) {
            null
        } else matcher.group(1)?.let { WallLink(it.toLong()) }
    }

    private fun parsePoll(string: String): AbsLink? {
        val matcher = patterns.PATTERN_POLL.matcher(string)
        try {
            if (matcher.find()) {
                return matcher.group(1)?.let {
                    matcher.group(2)
                        ?.let { it1 -> PollLink(it.toLong(), it1.toInt()) }
                }
            }
        } catch (ignored: Exception) {
        }
        return null
    }

    private fun parseWallCommentThreadLink(string: String): AbsLink? {
        val matcher = patterns.PATTERN_WALL_POST_COMMENT_THREAD.matcher(string)
        if (!matcher.find()) {
            return null
        }
        val link = matcher.group(1)?.let {
            matcher.group(2)?.let { it1 ->
                matcher.group(3)?.let { it2 ->
                    matcher.group(4)?.let { it3 ->
                        WallCommentThreadLink(
                            it.toLong(),
                            it1.toInt(),
                            it2.toInt(),
                            it3.toInt()
                        )
                    }
                }
            }
        }
        return if (link?.isValid == true) link else null
    }

    private fun parseWallCommentLink(string: String): AbsLink? {
        val matcher = patterns.PATTERN_WALL_POST_COMMENT.matcher(string)
        if (!matcher.find()) {
            return null
        }
        val link = matcher.group(1)?.let {
            matcher.group(2)?.let { it1 ->
                matcher.group(3)?.let { it2 ->
                    WallCommentLink(
                        it.toLong(),
                        it1.toInt(),
                        it2.toInt()
                    )
                }
            }
        }
        return if (link?.isValid == true) link else null
    }

    private fun parseWallPost(string: String): AbsLink? {
        val matcher = patterns.PATTERN_WALL_POST.matcher(string)
        return if (!matcher.find()) {
            null
        } else matcher.group(1)?.let {
            matcher.group(2)
                ?.let { it1 -> WallPostLink(it.toLong(), it1.toInt()) }
        }
    }

    private fun parseWallPostNotif(string: String): AbsLink? {
        val matcher = patterns.PATTERN_WALL_POST_NOTIFICATION.matcher(string)
        return if (!matcher.find()) {
            null
        } else matcher.group(1)?.let {
            matcher.group(2)
                ?.let { it1 -> WallPostLink(it.toLong(), it1.toInt()) }
        }
    }

    interface IParser {
        @Throws(Exception::class)
        fun parse(string: String?): Optional<AbsLink>
    }
}