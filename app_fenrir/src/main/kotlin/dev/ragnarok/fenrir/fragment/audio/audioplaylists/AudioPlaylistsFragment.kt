package dev.ragnarok.fenrir.fragment.audio.audioplaylists

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.AudioSelectActivity.Companion.createIntent
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.getParcelableArrayListExtraCompat
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getAudiosInAlbumPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.HelperSimple
import dev.ragnarok.fenrir.util.HelperSimple.needHelp
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class AudioPlaylistsFragment : BaseMvpFragment<AudioPlaylistsPresenter, IAudioPlaylistsView>(),
    IAudioPlaylistsView, AudioPlaylistsAdapter.ClickListener {
    private val requestAudioSelect = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val audios: ArrayList<Audio>? =
                result.data?.getParcelableArrayListExtraCompat("attachments")
            lazyPresenter {
                if (audios != null) {
                    fireAudiosSelected(audios)
                }
            }
        }
    }
    private var mEmpty: TextView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: AudioPlaylistsAdapter? = null
    private var inTabsContainer = false
    private var isSelectMode = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inTabsContainer = requireArguments().getBoolean(EXTRA_IN_TABS_CONTAINER)
        isSelectMode = requireArguments().getBoolean(ACTION_SELECT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_audio_playlist, container, false)
        val toolbar: Toolbar = root.findViewById(R.id.toolbar)
        if (!inTabsContainer) {
            toolbar.visibility = View.VISIBLE
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        } else {
            toolbar.visibility = View.GONE
        }
        mEmpty = root.findViewById(R.id.fragment_audio_playlist_empty_text)
        val mAdd: FloatingActionButton = root.findViewById(R.id.add_button)
        if (presenter?.accountId != (presenter?.owner_id ?: true))
            mAdd.visibility = View.GONE else {
            mAdd.visibility = View.VISIBLE
            mAdd.setOnClickListener {
                presenter?.fireCreatePlaylist(
                    requireActivity()
                )
            }
        }
        val recyclerView: RecyclerView = root.findViewById(R.id.recycleView)
        val columnCount = resources.getInteger(R.integer.photos_albums_column_count)
        recyclerView.layoutManager = GridLayoutManager(requireActivity(), columnCount)
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        val mySearchView: MySearchView = root.findViewById(R.id.searchview)
        mySearchView.setRightButtonVisibility(false)
        mySearchView.setLeftIcon(R.drawable.magnify)
        mySearchView.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                presenter?.fireSearchRequestChanged(
                    query
                )
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                presenter?.fireSearchRequestChanged(
                    newText
                )
                return false
            }
        })
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = AudioPlaylistsAdapter(emptyList(), requireActivity(), isSelectMode)
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter
        resolveEmptyText()
        return root
    }

    private fun resolveEmptyText() {
        if (mEmpty != null && mAdapter != null) {
            mEmpty?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (!inTabsContainer) {
            Settings.get().ui().notifyPlaceResumed(Place.AUDIOS)
            val actionBar = supportToolbarFor(this)
            if (actionBar != null) {
                actionBar.setTitle(R.string.playlists)
                actionBar.subtitle = null
            }
            if (requireActivity() is OnSectionResumeCallback) {
                (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_AUDIOS)
            }
            ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity())
        }
    }

    override fun displayData(pages: List<AudioPlaylist>) {
        if (mAdapter != null) {
            mAdapter?.setData(pages)
            resolveEmptyText()
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyText()
        }
    }

    override fun notifyItemRemoved(position: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRemoved(position)
            resolveEmptyText()
        }
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position, count)
            resolveEmptyText()
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<AudioPlaylistsPresenter> {
        return object : IPresenterFactory<AudioPlaylistsPresenter> {
            override fun create(): AudioPlaylistsPresenter {
                return AudioPlaylistsPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.OWNER_ID),
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
            getAudiosInAlbumPlace(
                presenter?.accountId ?: Settings.get().accounts().current,
                album.getOwnerId(),
                album.getId(),
                album.getAccess_key()
            ).tryOpenWith(requireActivity())
        }
    }

    override fun onOpenClick(index: Int, album: AudioPlaylist) {
        getAudiosInAlbumPlace(
            presenter?.accountId ?: Settings.get().accounts().current,
            album.getOwnerId(),
            album.getId(),
            album.getAccess_key()
        ).tryOpenWith(requireActivity())
    }

    override fun onDelete(index: Int, album: AudioPlaylist) {
        presenter?.onDelete(
            index,
            album
        )
    }

    override fun onShare(index: Int, album: AudioPlaylist) {
        startForSendAttachments(
            requireActivity(),
            presenter?.accountId ?: Settings.get().accounts().current,
            album
        )
    }

    override fun onEdit(index: Int, album: AudioPlaylist) {
        presenter?.onEdit(
            requireActivity(),
            album
        )
    }

    override fun onAddAudios(index: Int, album: AudioPlaylist) {
        presenter?.onPlaceToPending(
            album
        )
    }

    override fun doAddAudios(accountId: Long) {
        requestAudioSelect.launch(createIntent(requireActivity(), accountId))
    }

    override fun showHelper() {
        if (needHelp(HelperSimple.PLAYLIST_HELPER, 2)) {
            showSnackbar(R.string.playlist_helper, true)
        }
    }

    private fun showSnackbar(@StringRes res: Int, isLong: Boolean) {
        CustomSnackbars.createCustomSnackbars(view)
            ?.setDurationSnack(if (isLong) BaseTransientBottomBar.LENGTH_LONG else BaseTransientBottomBar.LENGTH_SHORT)
            ?.defaultSnack(res)?.show()
    }

    override fun onAdd(index: Int, album: AudioPlaylist, clone: Boolean) {
        presenter?.onAdd(
            album,
            clone
        )
    }

    companion object {
        const val EXTRA_IN_TABS_CONTAINER = "in_tabs_container"
        const val ACTION_SELECT = "AudioPlaylistsFragment.ACTION_SELECT"
        fun newInstance(accountId: Long, ownerId: Long): AudioPlaylistsFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            val fragment = AudioPlaylistsFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstanceSelect(accountId: Long): AudioPlaylistsFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, accountId)
            args.putBoolean(ACTION_SELECT, true)
            val fragment = AudioPlaylistsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}