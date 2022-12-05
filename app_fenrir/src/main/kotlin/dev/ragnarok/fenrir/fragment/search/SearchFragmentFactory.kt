package dev.ragnarok.fenrir.fragment.search

import androidx.fragment.app.Fragment
import dev.ragnarok.fenrir.fragment.search.artistsearch.ArtistSearchFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.search.audioplaylistsearch.AudioPlaylistSearchFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.search.audiossearch.AudiosSearchFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.search.audiossearch.AudiosSearchFragment.Companion.newInstanceSelect
import dev.ragnarok.fenrir.fragment.search.communitiessearch.CommunitiesSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.*
import dev.ragnarok.fenrir.fragment.search.dialogssearch.DialogsSearchFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.search.docssearch.DocsSearchFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.search.messagessearch.MessagesSearchFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.search.newsfeedsearch.NewsFeedSearchFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.search.peoplesearch.PeopleSearchFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.search.photosearch.PhotoSearchFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.search.videosearch.VideoSearchFragment
import dev.ragnarok.fenrir.fragment.search.wallsearch.WallSearchFragment

object SearchFragmentFactory {

    fun create(
        @SearchContentType type: Int,
        accountId: Int,
        criteria: BaseSearchCriteria?
    ): Fragment {
        return when (type) {
            SearchContentType.PEOPLE -> newInstance(
                accountId,
                if (criteria is PeopleSearchCriteria) criteria else null
            )
            SearchContentType.COMMUNITIES -> CommunitiesSearchFragment.newInstance(
                accountId,
                if (criteria is GroupSearchCriteria) criteria else null
            )
            SearchContentType.VIDEOS -> VideoSearchFragment.newInstance(
                accountId,
                if (criteria is VideoSearchCriteria) criteria else null
            )
            SearchContentType.AUDIOS -> newInstance(
                accountId,
                if (criteria is AudioSearchCriteria) criteria else null
            )
            SearchContentType.ARTISTS -> newInstance(
                accountId,
                if (criteria is ArtistSearchCriteria) criteria else null
            )
            SearchContentType.AUDIOS_SELECT -> newInstanceSelect(
                accountId,
                if (criteria is AudioSearchCriteria) criteria else null
            )
            SearchContentType.AUDIO_PLAYLISTS -> newInstance(
                accountId,
                if (criteria is AudioPlaylistSearchCriteria) criteria else null
            )
            SearchContentType.DOCUMENTS -> newInstance(
                accountId,
                if (criteria is DocumentSearchCriteria) criteria else null
            )
            SearchContentType.NEWS -> newInstance(
                accountId,
                if (criteria is NewsFeedCriteria) criteria else null
            )
            SearchContentType.MESSAGES -> newInstance(
                accountId,
                if (criteria is MessageSearchCriteria) criteria else null
            )
            SearchContentType.WALL -> WallSearchFragment.newInstance(
                accountId,
                if (criteria is WallSearchCriteria) criteria else null
            )
            SearchContentType.DIALOGS -> newInstance(
                accountId,
                if (criteria is DialogsSearchCriteria) criteria else null
            )
            SearchContentType.PHOTOS -> newInstance(
                accountId,
                if (criteria is PhotoSearchCriteria) criteria else null
            )
            else -> throw UnsupportedOperationException()
        }
    }
}