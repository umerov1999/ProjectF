package dev.ragnarok.fenrir.fragment.search.audioplaylistsearch

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.fragment.audio.audioplaylists.AudioPlaylistsAdapter
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.AudioPlaylistSearchCriteria
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.place.PlaceFactory.getAudiosInAlbumPlace
import dev.ragnarok.fenrir.settings.Settings

class AudioPlaylistSearchFragment :
    AbsSearchFragment<AudioPlaylistSearchPresenter, IAudioPlaylistSearchView, AudioPlaylist, AudioPlaylistsAdapter>(),
    AudioPlaylistsAdapter.ClickListener, IAudioPlaylistSearchView {
    private var isSelectMode = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSelectMode = requireArguments().getBoolean(ACTION_SELECT)
    }

    override fun setAdapterData(adapter: AudioPlaylistsAdapter, data: MutableList<AudioPlaylist>) {
        adapter.setData(data)
    }

    override fun postCreate(root: View) {}
    override fun createAdapter(data: MutableList<AudioPlaylist>): AudioPlaylistsAdapter {
        val ret = AudioPlaylistsAdapter(data, requireActivity(), isSelectMode)
        ret.setClickListener(this)
        return ret
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        val columnCount = resources.getInteger(R.integer.photos_albums_column_count)
        return GridLayoutManager(requireActivity(), columnCount)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<AudioPlaylistSearchPresenter> {
        return object : IPresenterFactory<AudioPlaylistSearchPresenter> {
            override fun create(): AudioPlaylistSearchPresenter {
                return AudioPlaylistSearchPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getParcelableCompat(Extra.CRITERIA),
                    saveInstanceState
                )
            }
        }
    }

    override fun onAlbumClick(index: Int, album: AudioPlaylist) {
        if (isSelectMode) {
            val intent = Intent()
            intent.putParcelableArrayListExtra(Extra.ATTACHMENTS, ArrayList(setOf(album)))
            requireActivity().setResult(Activity.RESULT_OK, intent)
            requireActivity().finish()
        } else {
            if (album.getOriginal_access_key()
                    .isNullOrEmpty() || album.getOriginal_id() == 0 || album.getOriginal_owner_id() == 0L
            ) getAudiosInAlbumPlace(
                presenter?.accountId ?: Settings.get().accounts().current,
                album.getOwnerId(),
                album.getId(),
                album.getAccess_key()
            ).tryOpenWith(requireActivity()) else getAudiosInAlbumPlace(
                presenter?.accountId ?: Settings.get().accounts().current,
                album.getOriginal_owner_id(),
                album.getOriginal_id(),
                album.getOriginal_access_key()
            ).tryOpenWith(requireActivity())
        }
    }

    override fun onOpenClick(index: Int, album: AudioPlaylist) {
        if (album.getOriginal_access_key()
                .isNullOrEmpty() || album.getOriginal_id() == 0 || album.getOriginal_owner_id() == 0L
        ) getAudiosInAlbumPlace(
            presenter?.accountId ?: Settings.get().accounts().current,
            album.getOwnerId(),
            album.getId(),
            album.getAccess_key()
        ).tryOpenWith(requireActivity()) else getAudiosInAlbumPlace(
            presenter?.accountId ?: Settings.get().accounts().current,
            album.getOriginal_owner_id(),
            album.getOriginal_id(),
            album.getOriginal_access_key()
        ).tryOpenWith(requireActivity())
    }

    override fun onDelete(index: Int, album: AudioPlaylist) {}
    override fun onShare(index: Int, album: AudioPlaylist) {
        startForSendAttachments(
            requireActivity(),
            presenter?.accountId ?: Settings.get().accounts().current,
            album
        )
    }

    override fun onEdit(index: Int, album: AudioPlaylist) {}
    override fun onAddAudios(index: Int, album: AudioPlaylist) {}
    override fun onAdd(index: Int, album: AudioPlaylist, clone: Boolean) {
        presenter?.onAdd(
            album,
            clone
        )
    }

    companion object {
        const val ACTION_SELECT = "AudioPlaylistSearchFragment.ACTION_SELECT"


        fun newInstance(
            accountId: Long,
            initialCriteria: AudioPlaylistSearchCriteria?
        ): AudioPlaylistSearchFragment {
            val args = Bundle()
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = AudioPlaylistSearchFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstanceSelect(
            accountId: Long,
            initialCriteria: AudioPlaylistSearchCriteria?
        ): AudioPlaylistSearchFragment {
            val args = Bundle()
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putBoolean(ACTION_SELECT, true)
            val fragment = AudioPlaylistSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}