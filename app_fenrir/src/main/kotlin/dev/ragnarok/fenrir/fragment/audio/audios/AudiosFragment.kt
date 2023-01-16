package dev.ragnarok.fenrir.fragment.audio.audios

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.base.horizontal.HorizontalPlaylistAdapter
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.currentAudio
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleURLPhotoPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.DownloadWorkUtils.CheckDirectory
import dev.ragnarok.fenrir.util.DownloadWorkUtils.makeDownloadRequestAudio
import dev.ragnarok.fenrir.util.HelperSimple
import dev.ragnarok.fenrir.util.HelperSimple.needHelp
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class AudiosFragment : BaseMvpFragment<AudiosPresenter, IAudiosView>(), IAudiosView,
    HorizontalPlaylistAdapter.Listener {
    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        createCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text)
    }
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAudioRecyclerAdapter: AudioRecyclerAdapter? = null
    private var inTabsContainer = false
    private val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
        ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
        ): Boolean {
            return presenter?.fireItemMoved(
                mAudioRecyclerAdapter?.getItemRawPosition(viewHolder.bindingAdapterPosition) ?: 0,
                mAudioRecyclerAdapter?.getItemRawPosition(target.bindingAdapterPosition) ?: 0
            ) ?: false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
            viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            mAudioRecyclerAdapter?.notifyItemChanged(viewHolder.bindingAdapterPosition)
            presenter?.playAudio(
                requireActivity(),
                mAudioRecyclerAdapter?.getItemRawPosition(viewHolder.bindingAdapterPosition) ?: 0
            )
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return !inTabsContainer
        }

        override fun isLongPressDragEnabled(): Boolean {
            return !Settings.get()
                .main().isUse_long_click_download && presenter?.isMyAudio ?: false && presenter?.isNotSearch ?: false
        }
    }
    private var isSelectMode = false
    private var isSaveMode = false
    private var headerPlaylist: View? = null
    private var mPlaylistAdapter: HorizontalPlaylistAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inTabsContainer = requireArguments().getBoolean(EXTRA_IN_TABS_CONTAINER)
        isSelectMode = requireArguments().getBoolean(ACTION_SELECT)
        isSaveMode = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_music_main, container, false)
        val toolbar: Toolbar = root.findViewById(R.id.toolbar)
        if (!inTabsContainer) {
            toolbar.visibility = View.VISIBLE
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        } else {
            toolbar.visibility = View.GONE
        }
        val searchView: MySearchView = root.findViewById(R.id.searchview)
        searchView.setRightButtonVisibility(false)
        searchView.setLeftIcon(R.drawable.magnify)
        searchView.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                lazyPresenter {
                    fireSearchRequestChanged(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                lazyPresenter {
                    fireSearchRequestChanged(newText)
                }
                return false
            }
        })
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(recyclerView)
        val save_mode: FloatingActionButton = root.findViewById(R.id.save_mode_button)
        val Goto: FloatingActionButton = root.findViewById(R.id.goto_button)
        save_mode.visibility = when {
            isSelectMode -> View.GONE
            Settings.get()
                .other().isAudio_save_mode_button -> View.VISIBLE
            else -> View.GONE
        }
        save_mode.setOnClickListener {
            isSaveMode = !isSaveMode
            Goto.setImageResource(if (isSaveMode) R.drawable.check else R.drawable.audio_player)
            save_mode.setImageResource(if (isSaveMode) R.drawable.ic_dismiss else R.drawable.save)
            mAudioRecyclerAdapter?.toggleSelectMode(isSaveMode)
            presenter?.fireUpdateSelectMode()
        }
        if (isSelectMode) {
            Goto.setImageResource(R.drawable.check)
            save_mode.setImageResource(R.drawable.ic_dismiss)
        } else {
            Goto.setImageResource(R.drawable.audio_player)
            save_mode.setImageResource(R.drawable.save)
        }
        Goto.setOnLongClickListener {
            if (!isSelectMode && !isSaveMode) {
                val curr = currentAudio
                if (curr != null) {
                    getPlayerPlace(Settings.get().accounts().current).tryOpenWith(requireActivity())
                } else {
                    createCustomToast(requireActivity()).showToastError(R.string.null_audio)
                }
            } else {
                presenter?.fireSelectAll()
            }
            true
        }
        Goto.setOnClickListener {
            if (isSelectMode) {
                val intent = Intent()
                intent.putParcelableArrayListExtra(
                    Extra.ATTACHMENTS, presenter?.getSelected(false) ?: ArrayList()
                )
                requireActivity().setResult(Activity.RESULT_OK, intent)
                requireActivity().finish()
            } else {
                if (isSaveMode) {
                    val tracks: List<Audio> = presenter?.getSelected(true) ?: ArrayList()
                    isSaveMode = false
                    Goto.setImageResource(R.drawable.audio_player)
                    save_mode.setImageResource(R.drawable.save)
                    mAudioRecyclerAdapter?.toggleSelectMode(isSaveMode)
                    presenter?.fireUpdateSelectMode()
                    if (tracks.isNotEmpty()) {
                        CheckDirectory(Settings.get().other().musicDir)
                        val account_id = presenter?.accountId ?: Settings.get().accounts().current
                        var obj = WorkManager.getInstance(requireActivity()).beginWith(
                            makeDownloadRequestAudio(
                                tracks[0], account_id
                            )
                        )
                        if (tracks.size > 1) {
                            val Requests: MutableList<OneTimeWorkRequest> =
                                ArrayList(tracks.size - 1)
                            var is_first = true
                            for (i in tracks) {
                                if (is_first) {
                                    is_first = false
                                    continue
                                }
                                Requests.add(makeDownloadRequestAudio(i, account_id))
                            }
                            obj = obj.then(Requests)
                        }
                        obj.enqueue()
                    }
                } else {
                    val curr = currentAudio
                    if (curr != null) {
                        val index =
                            presenter?.getAudioPos(curr) ?: -1
                        if (index >= 0) {
                            recyclerView.scrollToPosition(
                                index + (mAudioRecyclerAdapter?.headersCount ?: 0)
                            )
                        } else createCustomToast(requireActivity()).showToast(R.string.audio_not_found)
                    } else createCustomToast(requireActivity()).showToastError(R.string.null_audio)
                }
            }
        }
        mAudioRecyclerAdapter = AudioRecyclerAdapter(
            requireActivity(),
            mutableListOf(),
            presenter?.isMyAudio ?: false,
            isSelectMode,
            presenter?.playlistId
        )
        headerPlaylist = inflater.inflate(R.layout.header_audio_playlist, recyclerView, false)
        val headerPlaylistRecyclerView: RecyclerView? =
            headerPlaylist?.findViewById(R.id.header_audio_playlist)
        headerPlaylistRecyclerView?.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        mPlaylistAdapter = HorizontalPlaylistAdapter(mutableListOf())
        mPlaylistAdapter?.setListener(this)
        headerPlaylistRecyclerView?.adapter = mPlaylistAdapter
        mAudioRecyclerAdapter?.setClickListener(object : AudioRecyclerAdapter.ClickListener {
            override fun onClick(position: Int, audio: Audio) {
                presenter?.playAudio(
                    requireActivity(),
                    position
                )
            }

            override fun onEdit(position: Int, audio: Audio) {
                presenter?.fireEditTrackIn(
                    requireActivity(),
                    audio
                )
            }

            override fun onDelete(position: Int) {
                presenter?.fireDelete(
                    position
                )
            }

            override fun onUrlPhotoOpen(url: String, prefix: String, photo_prefix: String) {
                getSingleURLPhotoPlace(url, prefix, photo_prefix).tryOpenWith(requireActivity())
            }

            override fun onRequestWritePermissions() {
                requestWritePermission.launch()
            }
        })
        recyclerView.adapter = mAudioRecyclerAdapter
        return root
    }

    override fun updatePlaylists(playlist: MutableList<AudioPlaylist>) {
        mPlaylistAdapter?.setItems(playlist)
        mPlaylistAdapter?.notifyDataSetChanged()
        headerPlaylist?.let { mAudioRecyclerAdapter?.addHeader(it) }
    }

    private fun showSnackbar(@StringRes res: Int, isLong: Boolean) {
        CustomSnackbars.createCustomSnackbars(view)
            ?.setDurationSnack(if (isLong) BaseTransientBottomBar.LENGTH_LONG else BaseTransientBottomBar.LENGTH_SHORT)
            ?.defaultSnack(res)?.show()
    }

    override fun showAudioDeadHelper() {
        if (needHelp(HelperSimple.AUDIO_DEAD, 1)) {
            showSnackbar(R.string.audio_dead_helper, true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!inTabsContainer) {
            Settings.get().ui().notifyPlaceResumed(Place.AUDIOS)
            val actionBar = supportToolbarFor(this)
            if (actionBar != null) {
                actionBar.setTitle(R.string.music)
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

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<AudiosPresenter> {
        val accessKey =
            if (requireArguments().containsKey(Extra.ACCESS_KEY)) requireArguments().getString(
                Extra.ACCESS_KEY
            ) else null
        val albumId = if (requireArguments().containsKey(Extra.ALBUM_ID)) requireArguments().getInt(
            Extra.ALBUM_ID
        ) else null
        return object : IPresenterFactory<AudiosPresenter> {
            override fun create(): AudiosPresenter {
                return AudiosPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.OWNER_ID),
                    albumId,
                    accessKey,
                    requireArguments().getBoolean(ACTION_SELECT),
                    saveInstanceState
                )
            }
        }
    }

    override fun displayList(audios: MutableList<Audio>) {
        mAudioRecyclerAdapter?.setData(audios)
    }

    override fun notifyListChanged() {
        mAudioRecyclerAdapter?.notifyDataSetChanged()
    }

    override fun notifyItemMoved(fromPosition: Int, toPosition: Int) {
        mAudioRecyclerAdapter?.notifyItemBindableMoved(fromPosition, toPosition)
    }

    override fun notifyItemRemoved(index: Int) {
        mAudioRecyclerAdapter?.notifyItemBindableRemoved(index)
    }

    override fun notifyItemChanged(index: Int) {
        mAudioRecyclerAdapter?.notifyItemBindableChanged(index)
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAudioRecyclerAdapter?.notifyItemBindableRangeInserted(position, count)
    }

    override fun displayRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun onPlayListClick(item: AudioPlaylist, pos: Int) {
        if (item.getOwnerId() == Settings.get()
                .accounts().current
        ) presenter?.onDelete(item) else presenter?.onAdd(item)
    }

    override fun onShareClick(item: AudioPlaylist, pos: Int) {
        startForSendAttachments(requireActivity(), Settings.get().accounts().current, item)
    }

    companion object {
        const val EXTRA_IN_TABS_CONTAINER = "in_tabs_container"
        const val ACTION_SELECT = "AudiosFragment.ACTION_SELECT"
        fun buildArgs(accountId: Long, ownerId: Long, albumId: Int?, access_key: String?): Bundle {
            val args = Bundle()
            args.putLong(Extra.OWNER_ID, ownerId)
            args.putLong(Extra.ACCOUNT_ID, accountId)
            if (albumId != null) {
                args.putInt(Extra.ALBUM_ID, albumId)
            }
            if (!access_key.isNullOrEmpty()) {
                args.putString(Extra.ACCESS_KEY, access_key)
            }
            return args
        }

        fun newInstance(args: Bundle): AudiosFragment {
            val fragment = AudiosFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(args: Bundle, isSelect: Boolean): AudiosFragment {
            args.putBoolean(ACTION_SELECT, isSelect)
            val fragment = AudiosFragment()
            fragment.arguments = args
            return fragment
        }
    }
}