package dev.ragnarok.fenrir.fragment.videos.localvideos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.photos.localphotos.LocalPhotosAdapter
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.InternalVideoSize
import dev.ragnarok.fenrir.model.LocalVideo
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.place.PlaceFactory.getVkInternalPlayerPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.MySearchView

class LocalVideosFragment : BaseMvpFragment<LocalVideosPresenter, ILocalVideosView>(),
    ILocalVideosView, LocalVideosAdapter.ClickListener, SwipeRefreshLayout.OnRefreshListener {
    private var mRecyclerView: RecyclerView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: LocalVideosAdapter? = null
    private var mEmptyTextView: TextView? = null
    private var fabAttach: FloatingActionButton? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_video_gallery, container, false)
        view.findViewById<View>(R.id.toolbar).visibility = View.GONE
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
        val columnCount = resources.getInteger(R.integer.local_gallery_column_count)
        val manager: RecyclerView.LayoutManager = GridLayoutManager(requireActivity(), columnCount)
        mRecyclerView = view.findViewById(R.id.list)
        mRecyclerView?.layoutManager = manager
        mRecyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(LocalPhotosAdapter.TAG))
        mEmptyTextView = view.findViewById(R.id.empty)
        fabAttach = view.findViewById(R.id.fr_video_gallery_attach)
        fabAttach?.setOnClickListener {
            presenter?.fireFabClick()
        }
        return view
    }

    override fun onVideoClick(holder: LocalVideosAdapter.ViewHolder, video: LocalVideo) {
        presenter?.fireVideoClick(
            video
        )
    }

    override fun onVideoLongClick(holder: LocalVideosAdapter.ViewHolder, video: LocalVideo) {
        val target = Video().setOwnerId(Settings.get().accounts().current).setId(
            video.getId().toInt()
        )
            .setMp4link1080(video.getData().toString()).setTitle(video.getTitle())
        getVkInternalPlayerPlace(target, InternalVideoSize.SIZE_1080, true).tryOpenWith(
            requireActivity()
        )
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    override fun displayData(data: List<LocalVideo>) {
        mAdapter = LocalVideosAdapter(requireActivity(), data)
        mAdapter?.setClickListener(this)
        mRecyclerView?.adapter = mAdapter
    }

    override fun setEmptyTextVisible(visible: Boolean) {
        mEmptyTextView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun displayProgress(loading: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = loading }
    }

    override fun returnResultToParent(videos: ArrayList<LocalVideo>) {
        val intent = Intent()
        intent.putExtra(Extra.VIDEO, videos[0])
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

    override fun showError(errorText: String?) {
        if (isAdded) CustomToast.createCustomToast(requireActivity()).setDuration(Toast.LENGTH_LONG)
            .showToastError(errorText)
    }

    override fun showError(@StringRes titleTes: Int, vararg params: Any?) {
        if (isAdded) showError(getString(titleTes, *params))
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<LocalVideosPresenter> {
        return object : IPresenterFactory<LocalVideosPresenter> {
            override fun create(): LocalVideosPresenter {
                return LocalVideosPresenter(saveInstanceState)
            }
        }
    }

    companion object {
        fun newInstance(): LocalVideosFragment {
            return LocalVideosFragment()
        }
    }
}