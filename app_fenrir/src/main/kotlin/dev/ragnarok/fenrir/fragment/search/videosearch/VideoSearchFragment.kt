package dev.ragnarok.fenrir.fragment.search.videosearch

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.VideoSearchCriteria
import dev.ragnarok.fenrir.fragment.videos.VideosAdapter
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.util.Utils

class VideoSearchFragment :
    AbsSearchFragment<VideosSearchPresenter, IVideosSearchView, Video, VideosAdapter>(),
    VideosAdapter.VideoOnClickListener, IVideosSearchView {
    override fun setAdapterData(adapter: VideosAdapter, data: MutableList<Video>) {
        adapter.setData(data)
    }

    override fun postCreate(root: View) {}
    override fun createAdapter(data: MutableList<Video>): VideosAdapter {
        val adapter = VideosAdapter(requireActivity(), data)
        adapter.setVideoOnClickListener(this)
        return adapter
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        val columns = resources.getInteger(R.integer.videos_column_count)
        return StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
    }

    override fun returnSelectionToParent(video: Video) {
        val intent = Intent()
        intent.putParcelableArrayListExtra(Extra.ATTACHMENTS, Utils.singletonArrayList(video))
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    override fun onVideoClick(position: Int, video: Video) {
        presenter?.fireVideoClicked(
            video
        )
    }

    override fun onVideoLongClick(position: Int, video: Video): Boolean {
        return false
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<VideosSearchPresenter> {
        return object : IPresenterFactory<VideosSearchPresenter> {
            override fun create(): VideosSearchPresenter {
                return VideosSearchPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getParcelableCompat(Extra.CRITERIA),
                    requireArguments().getString(Extra.ACTION),
                    saveInstanceState
                )
            }
        }
    }

    companion object {
        fun newInstance(
            accountId: Int,
            initialCriteria: VideoSearchCriteria?
        ): VideoSearchFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            val fragment = VideoSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}