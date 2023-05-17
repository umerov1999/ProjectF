package dev.ragnarok.fenrir.fragment.poll.createpoll

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.model.Poll

class CreatePollFragment : BaseMvpFragment<CreatePollPresenter, ICreatePollView>(),
    ICreatePollView, MenuProvider {
    private var mQuestion: TextInputEditText? = null
    private var mAnonymous: MaterialSwitch? = null
    private var mMultiply: MaterialSwitch? = null
    private var mDisableUnvote: MaterialSwitch? = null
    private var mOptionsViewGroup: ViewGroup? = null
    private var mPollBackground: RecyclerView? = null
    private var mPollBackgroundAdapter: HorizontalPollBackgroundAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_create_poll, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))

        mPollBackground = root.findViewById(R.id.poll_background_option)
        mPollBackground?.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        mPollBackgroundAdapter = HorizontalPollBackgroundAdapter(emptyList(), requireActivity(), 0)
        mPollBackgroundAdapter?.setClickListener(object :
            HorizontalPollBackgroundAdapter.ClickListener {
            override fun onSelect(position: Int) {
                presenter?.fireSelectedBackgroundPoll(position)
            }
        })
        mPollBackground?.adapter = mPollBackgroundAdapter

        mQuestion = root.findViewById(R.id.dialog_poll_create_question)
        mAnonymous = root.findViewById(R.id.dialog_poll_create_anonymous)
        mMultiply = root.findViewById(R.id.dialog_poll_create_multiply)
        mDisableUnvote = root.findViewById(R.id.disable_unvote)
        mOptionsViewGroup = root.findViewById(R.id.dialog_poll_create_options)
        mOptionsViewGroup?.let {
            for (i in 0 until it.childCount) {
                if (it.getChildAt(i) !is TextInputLayout) {
                    continue
                }
                val editText = (it.getChildAt(i) as TextInputLayout).editText
                editText?.addTextChangedListener(object : TextWatcherAdapter() {
                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        presenter?.fireOptionEdited(
                            i,
                            s
                        )
                    }

                    override fun afterTextChanged(s: Editable?) {
                        if (s.isNullOrEmpty() || i == it.childCount - 1) {
                            return
                        }
                        if (it.getChildAt(i + 1) is TextInputLayout) {
                            val next = it.getChildAt(i + 1) as TextInputLayout
                            if (next.visibility == View.GONE) {
                                next.visibility = View.VISIBLE
                            }
                        }
                    }
                })
            }
        }
        mQuestion?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireQuestionEdited(
                    s
                )
            }
        })
        mAnonymous?.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            presenter?.fireAnonyamousChecked(
                isChecked
            )
        }
        mMultiply?.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            presenter?.fireMultiplyChecked(
                isChecked
            )
        }
        mDisableUnvote?.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            presenter?.fireDisableUnvoteChecked(
                isChecked
            )
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CreatePollPresenter> {
        return object : IPresenterFactory<CreatePollPresenter> {
            override fun create(): CreatePollPresenter {
                return CreatePollPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.OWNER_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.new_poll)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun displayQuestion(question: String?) {
        mQuestion?.setText(question)
    }

    override fun setAnonymous(anonymous: Boolean) {
        mAnonymous?.isChecked = anonymous
    }

    override fun setMultiply(multiply: Boolean) {
        mMultiply?.isChecked = multiply
    }

    override fun setDisableUnvote(disableUnvote: Boolean) {
        mDisableUnvote?.isChecked = disableUnvote
    }

    override fun setBackgroundsPoll(backgroundsPollList: List<Poll.PollBackground>, selected: Int) {
        mPollBackgroundAdapter?.setData(backgroundsPollList, selected)
    }

    override fun displayOptions(options: Array<String?>?) {
        mOptionsViewGroup?.let {
            for (i in 0 until it.childCount) {
                if (it.getChildAt(i) !is TextInputLayout) {
                    continue
                }
                val editText = (it.getChildAt(i) as TextInputLayout).editText
                (it.getChildAt(i) as TextInputLayout).visibility = View.VISIBLE
                editText?.setText(options?.get(i))
            }
            for (u in it.childCount - 2 downTo 0) {
                if (u == 1) {
                    break
                }
                if (options?.get(u).isNullOrEmpty()) {
                    it.getChildAt(u + 1).visibility = View.GONE
                } else {
                    break
                }
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.add_menu) {
            presenter?.fireDoneClick()
            return true
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.add_menu, menu)
    }

    override fun showQuestionError(@StringRes message: Int) {
        mQuestion?.error = getString(message)
        mQuestion?.requestFocus()
    }

    override fun showOptionError(index: Int, @StringRes message: Int) {
        (mOptionsViewGroup?.getChildAt(index) as TextInputLayout?)?.error = getString(message)
        mOptionsViewGroup?.getChildAt(index)?.requestFocus()
    }

    override fun sendResultAndGoBack(poll: Poll) {
        val intent = Bundle()
        intent.putParcelable(Extra.POLL, poll)
        parentFragmentManager.setFragmentResult(REQUEST_CREATE_POLL, intent)
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    companion object {
        const val REQUEST_CREATE_POLL = "request_create_poll"
        fun newInstance(args: Bundle?): CreatePollFragment {
            val fragment = CreatePollFragment()
            fragment.arguments = args
            return fragment
        }

        fun buildArgs(accountId: Long, ownerId: Long): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            return args
        }
    }
}
