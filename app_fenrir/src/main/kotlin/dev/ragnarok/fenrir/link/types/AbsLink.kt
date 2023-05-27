package dev.ragnarok.fenrir.link.types

abstract class AbsLink(val type: Int) {
    open val isValid: Boolean
        get() = true

    companion object {
        const val PHOTO = 0
        const val PHOTO_ALBUM = 1
        const val PROFILE = 2
        const val GROUP = 3
        const val TOPIC = 4
        const val DOMAIN = 5
        const val WALL_POST = 6
        const val PAGE = 7
        const val ALBUMS = 8
        const val DIALOG = 9
        const val EXTERNAL_LINK = 10
        const val WALL = 11
        const val DIALOGS = 12
        const val VIDEO = 13
        const val DOC = 14
        const val AUDIOS = 15
        const val FAVE = 16
        const val WALL_COMMENT = 17
        const val WALL_COMMENT_THREAD = 18
        const val BOARD = 19
        const val FEED_SEARCH = 20
        const val PLAYLIST = 21
        const val POLL = 22
        const val AUDIO_TRACK = 23
        const val ARTISTS = 24
        const val VIDEO_ALBUM = 25
        const val VIDEOS = 26
        const val APP_LINK = 27
        const val ARTICLE_LINK = 28
        const val STORY = 29
        const val CATALOG_V2_SECTION_LINK = 30
    }
}