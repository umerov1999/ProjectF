package dev.ragnarok.fenrir.fragment.photos.localphotos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleURLPhotoPlace
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomToast

class LocalPhotosFragment : BaseMvpFragment<LocalPhotosPresenter, ILocalPhotosView>(),
    ILocalPhotosView, LocalPhotosAdapter.ClickListener, SwipeRefreshLayout.OnRefreshListener {
    private val requestReadPermission =
        requestPermissionsAbs(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            lazyPresenter {
                fireReadExternalStoregePermissionResolved()
            }
        }
    private var mRecyclerView: RecyclerView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: LocalPhotosAdapter? = null
    private var mEmptyTextView: TextView? = null
    private var fabAttach: FloatingActionButton? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo_gallery, container, false)
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        if (!hasHideToolbarExtra()) {
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        } else {
            toolbar.visibility = View.GONE
        }
        mSwipeRefreshLayout = view.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val columnCount = resources.getInteger(R.integer.local_gallery_column_count)
        val manager: RecyclerView.LayoutManager = GridLayoutManager(requireActivity(), columnCount)
        mRecyclerView = view.findViewById(R.id.list)
        mRecyclerView?.layoutManager = manager
        mRecyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(LocalPhotosAdapter.TAG))
        mEmptyTextView = view.findViewById(R.id.empty)
        fabAttach = view.findViewById(R.id.fr_photo_gallery_attach)
        fabAttach?.setOnClickListener {
            presenter?.fireFabClick()
        }
        return view
    }

    override fun onPhotoClick(holder: LocalPhotosAdapter.ViewHolder, photo: LocalPhoto) {
        presenter?.firePhotoClick(
            photo
        )
    }

    override fun onLongPhotoClick(holder: LocalPhotosAdapter.ViewHolder, photo: LocalPhoto) {
        getSingleURLPhotoPlace(
            "file://" + photo.getFullImageUri()?.path,
            "Preview",
            "Temp"
        ).tryOpenWith(
            requireActivity()
        )
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    override fun displayData(data: List<LocalPhoto>) {
        mAdapter = LocalPhotosAdapter(requireActivity(), data)
        mAdapter?.setClickListener(this)
        mRecyclerView?.adapter = mAdapter
    }

    override fun setEmptyTextVisible(visible: Boolean) {
        mEmptyTextView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun displayProgress(loading: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = loading }
    }

    override fun returnResultToParent(photos: ArrayList<LocalPhoto>) {
        photos.sort()
        val intent = Intent()
        intent.putParcelableArrayListExtra(Extra.PHOTOS, photos)
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    override fun updateSelectionAndIndexes() {
        mAdapter?.updateHoldersSelectionAndIndexes()
    }

    override fun setFabVisible(visible: Boolean, anim: Boolean) {
        if (visible && fabAttach?.isShown == false) {
            fabAttach?.show()
        }
        if (!visible && fabAttach?.isShown == true) {
            fabAttach?.hide()
        }
    }

    override fun requestReadExternalStoragePermission() {
        requestReadPermission.launch()
    }

    override fun showError(errorText: String?) {
        if (isAdded) CustomToast.createCustomToast(requireActivity()).setDuration(Toast.LENGTH_LONG)
            .showToastError(errorText)
    }

    override fun showError(@StringRes titleTes: Int, vararg params: Any?) {
        if (isAdded) showError(getString(titleTes, *params))
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<LocalPhotosPresenter> {
        return object : IPresenterFactory<LocalPhotosPresenter> {
            override fun create(): LocalPhotosPresenter {
                val maxSelectionItemCount1 =
                    requireArguments().getInt(EXTRA_MAX_SELECTION_COUNT, 10)
                val album: LocalImageAlbum? = requireArguments().getParcelableCompat(Extra.ALBUM)
                return LocalPhotosPresenter(album, maxSelectionItemCount1, saveInstanceState)
            }
        }
    }

    companion object {
        const val EXTRA_MAX_SELECTION_COUNT = "max_selection_count"
        fun newInstance(
            maxSelectionItemCount: Int,
            album: LocalImageAlbum?,
            hide_toolbar: Boolean
        ): LocalPhotosFragment {
            val args = Bundle()
            args.putInt(EXTRA_MAX_SELECTION_COUNT, maxSelectionItemCount)
            args.putParcelable(Extra.ALBUM, album)
            if (hide_toolbar) args.putBoolean(EXTRA_HIDE_TOOLBAR, true)
            val fragment = LocalPhotosFragment()
            fragment.arguments = args
            return fragment
        }
    }
}