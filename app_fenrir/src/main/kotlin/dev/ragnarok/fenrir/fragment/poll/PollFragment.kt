package dev.ragnarok.fenrir.fragment.poll

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.poll.PollAnswersAdapter.OnAnswerChangedCallback
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.util.Utils.appLocale
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.view.AspectRatioImageView
import dev.ragnarok.fenrir.view.ProgressButton
import java.text.SimpleDateFormat
import java.util.Date

class PollFragment : BaseMvpFragment<PollPresenter, IPollView>(), IPollView,
    OnAnswerChangedCallback {
    private var mQuestion: TextView? = null
    private var mVotesCount: TextView? = null
    private var mAnswersAdapter: PollAnswersAdapter? = null
    private var mButton: ProgressButton? = null
    private var photo: AspectRatioImageView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_poll, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        mAnswersAdapter = PollAnswersAdapter(requireActivity(), mutableListOf())
        mAnswersAdapter?.setListener(this)
        val header = LayoutInflater.from(requireActivity())
            .inflate(R.layout.header_poll, recyclerView, false)
        mAnswersAdapter?.addHeader(header)
        mQuestion = header.findViewById(R.id.title)
        mVotesCount = header.findViewById(R.id.votes_count)
        photo = root.findViewById(R.id.item_poll_image)
        mButton = root.findViewById(R.id.vote)
        mButton?.onButtonClick {
            presenter?.fireButtonClick()
        }
        recyclerView.adapter = mAnswersAdapter
        return root
    }

    override fun displayQuestion(title: String?) {
        mQuestion?.text = title
    }

    override fun displayPhoto(photo_url: String?) {
        if (photo_url != null) {
            photo?.visibility = View.VISIBLE
            displayAvatar(photo, null, photo_url, Constants.PICASSO_TAG)
        } else {
            photo?.let { with().cancelRequest(it) }
            photo?.visibility = View.GONE
        }
    }

    override fun displayType(anonymous: Boolean) {
        val actionBar = supportToolbarFor(this)
        actionBar?.setTitle(if (anonymous) R.string.anonymous_poll else R.string.open_poll)
    }

    override fun displayCreationTime(unixtime: Long) {
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            if (unixtime <= 0) {
                actionBar.setSubtitle(R.string.unknown_error)
                return
            }
            val formattedDate = SimpleDateFormat("dd.MM.yyyy HH:mm", appLocale)
                .format(Date(unixtime * 1000))
            actionBar.subtitle = formattedDate
        }
    }

    override fun displayVoteCount(count: Int) {
        mVotesCount?.text = getString(R.string.votes_count, count)
    }

    override fun displayVotesList(
        answers: MutableList<Poll.Answer>?,
        canCheck: Boolean,
        multiply: Boolean,
        checked: MutableSet<Long>
    ) {
        mAnswersAdapter?.setData(answers, canCheck, multiply, checked)
    }

    override fun displayLoading(loading: Boolean) {
        if (mButton != null) {
            (mButton ?: return).changeState(loading)
        }
    }

    override fun setupButton(voted: Boolean) {
        if (mButton != null) {
            (mButton
                ?: return).setText(getString(if (voted) R.string.remove_vote else R.string.add_vote))
        }
    }

    override fun openVoters(
        accountId: Long,
        ownerId: Long,
        pollId: Int,
        board: Boolean,
        answer: Long
    ) {
        PlaceFactory.getVotersPlace(accountId, ownerId, pollId, board, answer)
            .tryOpenWith(requireActivity())
    }

    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<PollPresenter> {
        return object : IPresenterFactory<PollPresenter> {
            override fun create(): PollPresenter {
                val aid = requireArguments().getLong(Extra.ACCOUNT_ID)
                val poll: Poll = requireArguments().getParcelableCompat(Extra.POLL)!!
                return PollPresenter(aid, poll, saveInstanceState)
            }
        }
    }

    override fun onAnswerChanged(checked: MutableSet<Long>) {
        presenter?.fireVoteChecked(
            checked
        )
    }

    override fun onAnswerClick(id: Long) {
        presenter?.fireVoteClicked(
            id
        )
    }

    companion object {
        fun buildArgs(aid: Long, poll: Poll?): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(Extra.POLL, poll)
            bundle.putLong(Extra.ACCOUNT_ID, aid)
            return bundle
        }

        fun newInstance(bundle: Bundle?): PollFragment {
            val fragment = PollFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}