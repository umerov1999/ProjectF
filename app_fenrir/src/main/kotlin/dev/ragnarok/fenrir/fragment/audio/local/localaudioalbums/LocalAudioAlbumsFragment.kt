package dev.ragnarok.fenrir.fragment.audio.local.localaudioalbums

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.materialswitch.MaterialSwitch
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpBottomSheetDialogFragment
import dev.ragnarok.fenrir.fragment.photos.localimagealbums.LocalPhotoAlbumsAdapter
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.view.MySearchView

class LocalAudioAlbumsFragment :
    BaseMvpBottomSheetDialogFragment<LocalAudioAlbumsPresenter, ILocalAudioAlbumsView>(),
    LocalAudioAlbumsAdapter.ClickListener, SwipeRefreshLayout.OnRefreshListener,
    ILocalAudioAlbumsView {
    private val requestReadPermission =
        requestPermissionsAbs(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            lazyPresenter {
                fireReadExternalStoragePermissionResolved()
            }
        }
    private var mRecyclerView: RecyclerView? = null
    private var mAlbumsAdapter: LocalAudioAlbumsAdapter? = null
    private var listener: Listener? = null
    private var manager: RecyclerView.LayoutManager? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val dialog = BottomSheetDialog(requireActivity(), theme)
        val behavior = dialog.behavior
        //behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        behavior.skipCollapsed = true
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_local_albums_audio, container, false)
        val mySearchView: MySearchView = view.findViewById(R.id.searchview)
        val mRememberAudioAlbum: MaterialSwitch = view.findViewById(R.id.remember_audio_album)
        mRememberAudioAlbum.isChecked = Settings.get().main().isRememberLocalAudioAlbum
        mRememberAudioAlbum.setOnClickListener {
            Settings.get().main().isRememberLocalAudioAlbum = mRememberAudioAlbum.isChecked
        }
        mySearchView.setRightButtonVisibility(false)
        mySearchView.setLeftIcon(R.drawable.magnify)
        mySearchView.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                presenter?.fireSearchRequestChanged(
                    query,
                    false
                )
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                presenter?.fireSearchRequestChanged(
                    newText,
                    false
                )
                return false
            }
        })
        val columnCount = resources.getInteger(R.integer.photos_albums_column_count)
        manager = StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL)
        mRecyclerView = view.findViewById(R.id.recycler_view)
        mRecyclerView?.layoutManager = manager
        PicassoPauseOnScrollListener.addListener(mRecyclerView, LocalPhotoAlbumsAdapter.PICASSO_TAG)
        mAlbumsAdapter =
            LocalAudioAlbumsAdapter(requireActivity(), emptyList())
        mAlbumsAdapter?.setClickListener(this)
        mRecyclerView?.adapter = mAlbumsAdapter
        return view
    }

    override fun onClick(album: LocalImageAlbum) {
        presenter?.fireAlbumClick(
            album
        )
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    override fun displayData(data: List<LocalImageAlbum>) {
        mAlbumsAdapter?.setData(data)
    }

    override fun setEmptyTextVisible(visible: Boolean) {}
    override fun displayProgress(loading: Boolean) {}
    override fun openAlbum(album: LocalImageAlbum) {
        listener?.onSelected(album.getId())
        dismiss()
    }

    override fun notifyDataChanged() {
        mAlbumsAdapter?.notifyDataSetChanged()
        val pos = mAlbumsAdapter?.getFirstWithCurrentId().orZero()
        manager?.scrollToPosition(pos)
    }

    override fun requestReadExternalStoragePermission() {
        requestReadPermission.launch()
    }

    override fun updateCurrentId(currentId: Int) {
        mAlbumsAdapter?.updateCurrentId(currentId)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): LocalAudioAlbumsPresenter {
        val selectedId =
            requireArguments().getInt(Extra.ALBUM_ID, 0)
        return LocalAudioAlbumsPresenter(
            selectedId,
            saveInstanceState
        )
    }

    interface Listener {
        fun onSelected(bucket_id: Int)
    }

    companion object {
        fun newInstance(selectedId: Int, listener: Listener?): LocalAudioAlbumsFragment {
            val args = Bundle()
            args.putInt(Extra.ALBUM_ID, selectedId)

            val fragment = LocalAudioAlbumsFragment()
            fragment.arguments = args
            fragment.listener = listener
            return fragment
        }
    }
}
