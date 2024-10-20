package dev.ragnarok.fenrir.fragment.search

import androidx.fragment.app.Fragment
import dev.ragnarok.fenrir.fragment.search.artistsearch.ArtistSearchFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.search.audioplaylistsearch.AudioPlaylistSearchFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.search.audiossearch.AudiosSearchFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.search.audiossearch.AudiosSearchFragment.Companion.newInstanceSelect
import dev.ragnarok.fenrir.fragment.search.communitiessearch.CommunitiesSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.ArtistSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.AudioPlaylistSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.AudioSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.BaseSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.DialogsSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.DocumentSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.GroupSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.MessageSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.NewsFeedCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.PeopleSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.PhotoSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.VideoSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.WallSearchCriteria
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
        accountId: Long,
        criteria: BaseSearchCriteria?
    ): Fragment {
        return when (type) {
            SearchContentType.PEOPLE -> newInstance(
                accountId,
                criteria as? PeopleSearchCriteria
            )

            SearchContentType.COMMUNITIES -> CommunitiesSearchFragment.newInstance(
                accountId,
                criteria as? GroupSearchCriteria
            )

            SearchContentType.VIDEOS -> VideoSearchFragment.newInstance(
                accountId,
                criteria as? VideoSearchCriteria
            )

            SearchContentType.AUDIOS -> newInstance(
                accountId,
                criteria as? AudioSearchCriteria
            )

            SearchContentType.ARTISTS -> newInstance(
                accountId,
                criteria as? ArtistSearchCriteria
            )

            SearchContentType.AUDIOS_SELECT -> newInstanceSelect(
                accountId,
                criteria as? AudioSearchCriteria
            )

            SearchContentType.AUDIO_PLAYLISTS -> newInstance(
                accountId,
                criteria as? AudioPlaylistSearchCriteria
            )

            SearchContentType.DOCUMENTS -> newInstance(
                accountId,
                criteria as? DocumentSearchCriteria
            )

            SearchContentType.NEWS -> newInstance(
                accountId,
                criteria as? NewsFeedCriteria
            )

            SearchContentType.MESSAGES -> newInstance(
                accountId,
                criteria as? MessageSearchCriteria
            )

            SearchContentType.WALL -> WallSearchFragment.newInstance(
                accountId,
                criteria as? WallSearchCriteria
            )

            SearchContentType.DIALOGS -> newInstance(
                accountId,
                criteria as? DialogsSearchCriteria
            )

            SearchContentType.PHOTOS -> newInstance(
                accountId,
                criteria as? PhotoSearchCriteria
            )

            else -> throw UnsupportedOperationException()
        }
    }
}