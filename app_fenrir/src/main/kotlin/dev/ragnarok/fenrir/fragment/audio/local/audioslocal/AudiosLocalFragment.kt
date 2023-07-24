package dev.ragnarok.fenrir.fragment.audio.local.audioslocal

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.audio.local.localaudioalbums.LocalAudioAlbumsFragment
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.docs.DocsUploadAdapter
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.currentAudio
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.util.AppPerms.DoRequestPermissions
import dev.ragnarok.fenrir.util.AppPerms.hasReadStoragePermission
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsResultAbs
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.MySearchView.OnAdditionalButtonClickListener

class AudiosLocalFragment : BaseMvpFragment<AudiosLocalPresenter, IAudiosLocalView>(),
    MySearchView.OnQueryTextListener, DocsUploadAdapter.ActionListener,
    AudioLocalRecyclerAdapter.ClickListener, IAudiosLocalView {
    private val requestReadPermission: DoRequestPermissions = requestPermissionsResultAbs(arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    ),
        {
            lazyPresenter {
                firePrepared()
            }
        },
        {
            lazyPresenter {
                firePermissionsCanceled()
            }
        }
    )
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAudioRecyclerAdapter: AudioLocalRecyclerAdapter? = null
    private var mUploadAdapter: DocsUploadAdapter? = null
    private var mUploadRoot: View? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_local_music, container, false)
        val searchView: MySearchView = root.findViewById(R.id.searchview)
        searchView.setOnQueryTextListener(this)
        searchView.setRightButtonVisibility(true)
        searchView.setRightIcon(R.drawable.ic_menu_24_white)
        searchView.setLeftIcon(R.drawable.magnify)
        searchView.setQuery("", true)
        searchView.setOnAdditionalButtonClickListener(object : OnAdditionalButtonClickListener {
            override fun onAdditionalButtonClick() {
                presenter?.fireLocalAudioAlbums()
            }

        })
        val uploadRecyclerView: RecyclerView = root.findViewById(R.id.uploads_recycler_view)
        uploadRecyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        mUploadAdapter = DocsUploadAdapter(emptyList(), this)
        uploadRecyclerView.adapter = mUploadAdapter
        mUploadRoot = root.findViewById(R.id.uploads_root)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        PicassoPauseOnScrollListener.addListener(recyclerView)
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
        mAudioRecyclerAdapter = AudioLocalRecyclerAdapter(requireActivity(), emptyList())
        mAudioRecyclerAdapter?.setClickListener(this)
        recyclerView.adapter = mAudioRecyclerAdapter
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<AudiosLocalPresenter> {
        return object : IPresenterFactory<AudiosLocalPresenter> {
            override fun create(): AudiosLocalPresenter {
                return AudiosLocalPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
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

    override fun displayRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun checkPermission() {
        if (!hasReadStoragePermission(requireActivity())) {
            requestReadPermission.launch()
        } else {
            presenter?.firePrepared()
        }
    }

    override fun goToLocalAudioAlbums(selectedId: Int) {
        LocalAudioAlbumsFragment.newInstance(selectedId,
            object : LocalAudioAlbumsFragment.Listener {
                override fun onSelected(bucket_id: Int) {
                    if (Settings.get().other().isRememberLocalAudioAlbum) {
                        Settings.get().other().currentLocalAudioAlbum = bucket_id
                    }
                    presenter?.fireBucketSelected(
                        bucket_id
                    )
                }

            }).show(
            childFragmentManager, "audio_albums_local"
        )
    }

    override fun setUploadDataVisible(visible: Boolean) {
        mUploadRoot?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun displayUploads(data: List<Upload>) {
        mUploadAdapter?.setData(data)
    }

    override fun notifyItemChanged(index: Int) {
        mAudioRecyclerAdapter?.notifyItemChanged(index)
    }

    override fun notifyItemRemoved(index: Int) {
        mAudioRecyclerAdapter?.notifyItemRemoved(index)
    }

    override fun notifyUploadItemsAdded(position: Int, count: Int) {
        mUploadAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun notifyUploadItemChanged(position: Int) {
        mUploadAdapter?.notifyItemChanged(position)
    }

    override fun notifyUploadItemRemoved(position: Int) {
        mUploadAdapter?.notifyItemRemoved(position)
    }

    override fun notifyUploadProgressChanged(position: Int, progress: Int, smoothly: Boolean) {
        mUploadAdapter?.changeUploadProgress(position, progress, smoothly)
    }

    override fun onRemoveClick(upload: Upload) {
        presenter?.fireRemoveClick(
            upload
        )
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        presenter?.fireQuery(
            query
        )
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        presenter?.fireQuery(
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

    override fun onDelete(position: Int) {
        presenter?.fireDelete(
            position
        )
    }

    override fun onUpload(position: Int, audio: Audio) {
        presenter?.fireFileForUploadSelected(
            audio.url
        )
    }

    override fun onRemotePlay(position: Int, audio: Audio) {
        presenter?.fireFileForRemotePlaySelected(
            audio.url
        )
    }

    companion object {
        fun newInstance(accountId: Long): AudiosLocalFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = AudiosLocalFragment()
            fragment.arguments = args
            return fragment
        }
    }
}