package dev.ragnarok.fenrir.fragment.search

import androidx.annotation.IntDef

@IntDef(
    SearchContentType.PEOPLE,
    SearchContentType.COMMUNITIES,
    SearchContentType.NEWS,
    SearchContentType.AUDIOS,
    SearchContentType.VIDEOS,
    SearchContentType.MESSAGES,
    SearchContentType.DOCUMENTS,
    SearchContentType.WALL,
    SearchContentType.DIALOGS,
    SearchContentType.PHOTOS,
    SearchContentType.AUDIOS_SELECT,
    SearchContentType.AUDIO_PLAYLISTS,
    SearchContentType.ARTISTS
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class SearchContentType {
    companion object {
        const val PEOPLE = 0
        const val COMMUNITIES = 1
        const val NEWS = 2
        const val AUDIOS = 3
        const val VIDEOS = 4
        const val MESSAGES = 5
        const val DOCUMENTS = 6
        const val WALL = 7
        const val DIALOGS = 8
        const val PHOTOS = 9
        const val AUDIOS_SELECT = 10
        const val AUDIO_PLAYLISTS = 11
        const val ARTISTS = 12
    }
}