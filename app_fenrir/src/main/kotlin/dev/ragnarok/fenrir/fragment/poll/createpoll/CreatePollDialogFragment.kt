package dev.ragnarok.fenrir.fragment.poll.createpoll

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpBottomSheetDialogFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.model.Poll

class CreatePollDialogFragment :
    BaseMvpBottomSheetDialogFragment<CreatePollPresenter, ICreatePollView>(),
    ICreatePollView {
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
        val root = inflater.inflate(R.layout.fragment_create_poll_dialog, container, false)
        root.findViewById<FloatingActionButton>(R.id.selected_button).setOnClickListener {
            presenter?.fireDoneClick()
        }

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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity(), theme)
        val behavior: BottomSheetBehavior<*> = dialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        return dialog
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
        if (requireArguments().getBoolean(Extra.IS_EDIT)) {
            parentFragmentManager.setFragmentResult(REQUEST_CREATE_POLL_EDIT, intent)
        } else {
            parentFragmentManager.setFragmentResult(REQUEST_CREATE_POLL, intent)
        }
        dismiss()
    }

    companion object {
        const val REQUEST_CREATE_POLL = "request_create_poll_dialog"
        const val REQUEST_CREATE_POLL_EDIT = "request_create_poll_dialog_edit"
        fun newInstance(accountId: Long, ownerId: Long, edit: Boolean): CreatePollDialogFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            args.putBoolean(Extra.IS_EDIT, edit)
            val fragment = CreatePollDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
