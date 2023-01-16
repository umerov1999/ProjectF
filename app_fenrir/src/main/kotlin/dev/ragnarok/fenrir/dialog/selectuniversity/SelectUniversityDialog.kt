package dev.ragnarok.fenrir.dialog.selectuniversity

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.dialog.base.AccountDependencyDialogFragment
import dev.ragnarok.fenrir.domain.IDatabaseInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.search.filteredit.FilterEditFragment
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.model.database.University
import java.util.*

class SelectUniversityDialog : AccountDependencyDialogFragment(), UniversitiesAdapter.Listener {
    private val mHandler = Handler(Looper.getMainLooper())
    private var universityId = 0
    private val mDatabaseInteractor: IDatabaseInteractor =
        InteractorFactory.createDatabaseInteractor()
    private var mData: ArrayList<University>? = null
    private var mAdapter: UniversitiesAdapter? = null
    private var filter: String? = null
    private val runSearchRunnable = Runnable { request(0) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        universityId = requireArguments().getInt(Extra.UNIVERSITY_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val root = View.inflate(requireActivity(), R.layout.dialog_country_or_city_select, null)
        val input: TextInputEditText = root.findViewById(R.id.input)
        input.setText(filter)
        input.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable?) {
                filter = s.toString()
                mHandler.removeCallbacks(runSearchRunnable)
                mHandler.postDelayed(runSearchRunnable, RUN_SEARCH_DELAY.toLong())
            }
        })
        val mRecyclerView: RecyclerView = root.findViewById(R.id.list)
        mRecyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        var firstRun = false
        if (mData == null) {
            mData = ArrayList()
            firstRun = true
        }
        mAdapter = UniversitiesAdapter(requireActivity(), mData ?: Collections.emptyList())
        mAdapter?.setListener(this)
        mRecyclerView.adapter = mAdapter
        if (firstRun) {
            request(0)
        }
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.college_or_university)
            .setView(root)
            .setNegativeButton(R.string.button_cancel, null)
            .create()
    }

    private fun request(offset: Int) {
        appendDisposable(
            mDatabaseInteractor.getUniversities(
                accountId,
                filter,
                null,
                universityId,
                COUNT_PER_REQUEST,
                offset
            )
                .fromIOToMain()
                .subscribe({ universities ->
                    onDataReceived(
                        offset,
                        universities
                    )
                }) { })
    }

    private fun onDataReceived(offset: Int, universities: List<University>) {
        if (offset == 0) {
            mData?.clear()
        }
        mData?.addAll(universities)
        mAdapter?.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(runSearchRunnable)
    }

    override fun onClick(university: University) {
        val intent = Bundle()
        intent.putParcelable(Extra.UNIVERSITY, university)
        intent.putInt(Extra.ID, university.id)
        intent.putString(Extra.TITLE, university.title)
        if (arguments != null) {
            intent.putAll(arguments)
        }
        parentFragmentManager.setFragmentResult(FilterEditFragment.REQUEST_FILTER_OPTION, intent)
        dismiss()
    }

    companion object {
        private const val COUNT_PER_REQUEST = 1000
        private const val RUN_SEARCH_DELAY = 1000
        fun newInstance(aid: Long, universityId: Int, additional: Bundle?): SelectUniversityDialog {
            val args = additional ?: Bundle()
            args.putInt(Extra.UNIVERSITY_ID, universityId)
            args.putLong(Extra.ACCOUNT_ID, aid)
            val selectCityDialog = SelectUniversityDialog()
            selectCityDialog.arguments = args
            return selectCityDialog
        }
    }
}