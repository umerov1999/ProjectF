package dev.ragnarok.filegallery.fragment.localserver.audioslocalserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.dialog.DialogLocalServerOptionDialog.Companion.newInstance
import dev.ragnarok.filegallery.dialog.DialogLocalServerOptionDialog.DialogLocalServerOptionListener
import dev.ragnarok.filegallery.fragment.base.BaseMvpFragment
import dev.ragnarok.filegallery.fragment.base.core.IPresenterFactory
import dev.ragnarok.filegallery.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.filegallery.listener.PicassoPauseOnScrollListener
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.filegallery.util.ViewUtils
import dev.ragnarok.filegallery.view.MySearchView
import dev.ragnarok.filegallery.view.MySearchView.OnAdditionalButtonClickListener

class AudiosLocalServerFragment :
    BaseMvpFragment<AudiosLocalServerPresenter, IAudiosLocalServerView>(),
    MySearchView.OnQueryTextListener, AudioLocalServerRecyclerAdapter.ClickListener,
    IAudiosLocalServerView {
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
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        val Goto: FloatingActionButton = root.findViewById(R.id.goto_button)
        Goto.setOnLongClickListener {
            val curr = MusicPlaybackController.currentAudio
            if (curr != null) {
                getPlayerPlace().tryOpenWith(requireActivity())
            } else customToast?.showToastError(R.string.null_audio)
            false
        }
        Goto.setOnClickListener {
            val curr = MusicPlaybackController.currentAudio
            if (curr != null) {
                val index =
                    presenter?.getAudioPos(curr) ?: -1
                if (index >= 0) {
                    recyclerView.scrollToPosition(index)
                } else customToast?.showToast(R.string.audio_not_found)
            } else customToast?.showToastError(R.string.null_audio)
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
}