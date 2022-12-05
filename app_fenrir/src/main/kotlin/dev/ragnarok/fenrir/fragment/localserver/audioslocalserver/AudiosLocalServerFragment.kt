package dev.ragnarok.fenrir.fragment.localserver.audioslocalserver

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.dialog.DialogLocalServerOptionDialog.Companion.newInstance
import dev.ragnarok.fenrir.dialog.DialogLocalServerOptionDialog.DialogLocalServerOptionListener
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.currentAudio
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleURLPhotoPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.DownloadWorkUtils.doSyncRemoteAudio
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.MySearchView.OnAdditionalButtonClickListener
import java.io.IOException

class AudiosLocalServerFragment :
    BaseMvpFragment<AudiosLocalServerPresenter, IAudiosLocalServerView>(),
    MySearchView.OnQueryTextListener, AudioLocalServerRecyclerAdapter.ClickListener,
    IAudiosLocalServerView {
    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        createCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text)
    }
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAudioRecyclerAdapter: AudioLocalServerRecyclerAdapter? = null
    private var searchView: MySearchView? = null
    internal fun clearSearch() {
        searchView?.setOnQueryTextListener(null)
        searchView?.clear()
        searchView?.setOnQueryTextListener(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_local_server_music, container, false)
        searchView = root.findViewById(R.id.searchview)
        searchView?.setOnQueryTextListener(this)
        searchView?.setRightButtonVisibility(true)
        searchView?.setLeftIcon(R.drawable.magnify)
        searchView?.setRightIcon(R.drawable.dots_vertical)
        searchView?.setQuery("", true)
        searchView?.setOnAdditionalButtonClickListener(object : OnAdditionalButtonClickListener {
            override fun onAdditionalButtonClick() {
                presenter?.fireOptions()
            }
        })
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh(
                false
            )
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
        val Goto: FloatingActionButton = root.findViewById(R.id.goto_button)
        Goto.setImageResource(R.drawable.audio_player)
        Goto.setOnLongClickListener {
            val curr = currentAudio
            if (curr != null) {
                getPlayerPlace(Settings.get().accounts().current).tryOpenWith(requireActivity())
            } else createCustomToast(requireActivity()).showToastError(R.string.null_audio)
            false
        }
        Goto.setOnClickListener {
            val curr = currentAudio
            if (curr != null) {
                val index =
                    presenter?.getAudioPos(curr) ?: -1
                if (index >= 0) {
                    recyclerView.scrollToPosition(index)
                } else createCustomToast(requireActivity()).showToast(R.string.audio_not_found)
            } else createCustomToast(requireActivity()).showToastError(R.string.null_audio)
        }
        mAudioRecyclerAdapter = AudioLocalServerRecyclerAdapter(requireActivity(), emptyList())
        mAudioRecyclerAdapter?.setClickListener(this)
        recyclerView.adapter = mAudioRecyclerAdapter
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<AudiosLocalServerPresenter> {
        return object : IPresenterFactory<AudiosLocalServerPresenter> {
            override fun create(): AudiosLocalServerPresenter {
                return AudiosLocalServerPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun displayList(audios: List<Audio>) {
        mAudioRecyclerAdapter?.setItems(audios)
    }

    override fun notifyListChanged() {
        mAudioRecyclerAdapter?.notifyDataSetChanged()
    }

    override fun displayLoading(loading: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = loading
    }

    override fun displayOptionsDialog(isReverse: Boolean, isDiscography: Boolean) {
        newInstance(isDiscography, isReverse, object : DialogLocalServerOptionListener {
            override fun onReverse(reverse: Boolean) {
                presenter?.updateReverse(
                    reverse
                )
            }

            override fun onDiscography(discography: Boolean) {
                clearSearch()
                presenter?.updateDiscography(
                    discography
                )
            }

            override fun onRemoteSync() {
                try {
                    MusicPlaybackController.tracksExist.findRemoteAudios(requireActivity())
                    createCustomToast(requireActivity()).showToastSuccessBottom(R.string.success)
                } catch (e: IOException) {
                    createCustomToast(requireActivity()).showToastThrowable(e)
                }
            }

            override fun onRemoteGet() {
                doSyncRemoteAudio(requireActivity())
            }
        }).show(childFragmentManager, "dialog-local-server-options")
    }

    override fun notifyItemChanged(index: Int) {
        mAudioRecyclerAdapter?.notifyItemChanged(index)
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAudioRecyclerAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        presenter?.fireSearchRequestChanged(
            query
        )
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        presenter?.fireSearchRequestChanged(
            newText
        )
        return false
    }

    override fun onClick(position: Int, audio: Audio) {
        presenter?.playAudio(
            requireActivity(),
            position
        )
    }

    override fun onUrlPhotoOpen(url: String, prefix: String, photo_prefix: String) {
        getSingleURLPhotoPlace(url, prefix, photo_prefix).tryOpenWith(requireActivity())
    }

    override fun onRequestWritePermissions() {
        requestWritePermission.launch()
    }

    companion object {
        fun newInstance(accountId: Int): AudiosLocalServerFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            val fragment = AudiosLocalServerFragment()
            fragment.arguments = args
            return fragment
        }
    }
}