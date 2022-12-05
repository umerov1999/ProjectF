package dev.ragnarok.fenrir.fragment.audio.audiosbyartist

import android.Manifest
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.audio.audios.AudioRecyclerAdapter
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.currentAudio
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleURLPhotoPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.DoRequestPermissions
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.DownloadWorkUtils.CheckDirectory
import dev.ragnarok.fenrir.util.DownloadWorkUtils.makeDownloadRequestAudio
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class AudiosByArtistFragment : BaseMvpFragment<AudiosByArtistPresenter, IAudiosByArtistView>(),
    IAudiosByArtistView {
    private val requestWritePermission: DoRequestPermissions = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) { createCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text) }
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAudioRecyclerAdapter: AudioRecyclerAdapter? = null
    private val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
        ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
            viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            mAudioRecyclerAdapter?.notifyItemChanged(viewHolder.bindingAdapterPosition)
            mAudioRecyclerAdapter?.getItemRawPosition(viewHolder.bindingAdapterPosition)?.let {
                presenter?.playAudio(
                    requireActivity(),
                    it
                )
            }
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return true
        }

        override fun isLongPressDragEnabled(): Boolean {
            return false
        }
    }
    private var isSaveMode = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSaveMode = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_music, container, false)
        val toolbar: Toolbar = root.findViewById(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
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
        save_mode.visibility =
            if (Settings.get().other().isAudio_save_mode_button) View.VISIBLE else View.GONE
        save_mode.setOnClickListener {
            isSaveMode = !isSaveMode
            Goto.setImageResource(if (isSaveMode) R.drawable.check else R.drawable.audio_player)
            save_mode.setImageResource(if (isSaveMode) R.drawable.ic_dismiss else R.drawable.save)
            mAudioRecyclerAdapter?.toggleSelectMode(isSaveMode)
            presenter?.fireUpdateSelectMode()
        }
        Goto.setImageResource(R.drawable.audio_player)
        save_mode.setImageResource(R.drawable.save)
        Goto.setOnLongClickListener {
            if (!isSaveMode) {
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
                    var `object` = WorkManager.getInstance(requireActivity()).beginWith(
                        makeDownloadRequestAudio(
                            tracks[0], account_id
                        )
                    )
                    if (tracks.size > 1) {
                        val Requests: MutableList<OneTimeWorkRequest> = ArrayList(tracks.size - 1)
                        var is_first = true
                        for (i in tracks) {
                            if (is_first) {
                                is_first = false
                                continue
                            }
                            Requests.add(makeDownloadRequestAudio(i, account_id))
                        }
                        `object` = `object`.then(Requests)
                    }
                    `object`.enqueue()
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
        mAudioRecyclerAdapter = AudioRecyclerAdapter(
            requireActivity(),
            mutableListOf(),
            presenter?.isMyAudio ?: false,
            false,
            null
        )
        mAudioRecyclerAdapter?.setClickListener(object : AudioRecyclerAdapter.ClickListener {
            override fun onClick(position: Int, audio: Audio) {
                presenter?.playAudio(
                    requireActivity(),
                    position
                )
            }

            override fun onEdit(position: Int, audio: Audio) {}
            override fun onDelete(position: Int) {}
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

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.AUDIOS)
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.artists)
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

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<AudiosByArtistPresenter> {
        return object : IPresenterFactory<AudiosByArtistPresenter> {
            override fun create(): AudiosByArtistPresenter {
                return AudiosByArtistPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getString(Extra.ARTIST)!!,
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

    override fun notifyItemChanged(index: Int) {
        mAudioRecyclerAdapter?.notifyItemBindableChanged(index)
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAudioRecyclerAdapter?.notifyItemBindableRangeInserted(position, count)
    }

    override fun displayRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    companion object {
        fun newInstance(args: Bundle?): Fragment {
            val fragment = AudiosByArtistFragment()
            fragment.arguments = args
            return fragment
        }

        fun buildArgs(accountId: Int, artistId: String?): Bundle {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putString(Extra.ARTIST, artistId)
            return args
        }
    }
}