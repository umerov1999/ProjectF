package dev.ragnarok.fenrir.fragment.photos.localimagealbums

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.picasso.Content_Local
import dev.ragnarok.fenrir.place.PlaceFactory.getLocalImageAlbumPlace
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.view.MySearchView

class LocalImageAlbumsFragment :
    BaseMvpFragment<LocalPhotoAlbumsPresenter, ILocalPhotoAlbumsView>(),
    LocalPhotoAlbumsAdapter.ClickListener, SwipeRefreshLayout.OnRefreshListener,
    ILocalPhotoAlbumsView {
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
    private var mEmptyTextView: TextView? = null
    private var mAlbumsAdapter: LocalPhotoAlbumsAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_local_albums_gallery, container, false)
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        if (!hasHideToolbarExtra()) {
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        } else {
            toolbar.visibility = View.GONE
        }
        val mySearchView: MySearchView = view.findViewById(R.id.searchview)
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
        mSwipeRefreshLayout = view.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val columnCount = resources.getInteger(R.integer.photos_albums_column_count)
        val manager: RecyclerView.LayoutManager =
            StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL)
        mRecyclerView = view.findViewById(R.id.list)
        mRecyclerView?.layoutManager = manager
        mRecyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(LocalPhotoAlbumsAdapter.PICASSO_TAG))
        mAlbumsAdapter =
            LocalPhotoAlbumsAdapter(requireActivity(), emptyList(), Content_Local.PHOTO)
        mAlbumsAdapter?.setClickListener(this)
        mRecyclerView?.adapter = mAlbumsAdapter
        mEmptyTextView = view.findViewById(R.id.empty)
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

    override fun setEmptyTextVisible(visible: Boolean) {
        mEmptyTextView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun displayProgress(loading: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = loading }
    }

    override fun openAlbum(album: LocalImageAlbum) {
        getLocalImageAlbumPlace(album).tryOpenWith(requireActivity())
    }

    override fun notifyDataChanged() {
        mAlbumsAdapter?.notifyDataSetChanged()
    }

    override fun requestReadExternalStoragePermission() {
        requestReadPermission.launch()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<LocalPhotoAlbumsPresenter> {
        return object : IPresenterFactory<LocalPhotoAlbumsPresenter> {
            override fun create(): LocalPhotoAlbumsPresenter {
                return LocalPhotoAlbumsPresenter(
                    saveInstanceState
                )
            }
        }
    }
}