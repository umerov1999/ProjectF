package dev.ragnarok.fenrir.dialog.selectschools

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
import dev.ragnarok.fenrir.model.database.School
import java.util.*

class SelectSchoolsDialog : AccountDependencyDialogFragment(), SchoolsAdapter.Listener {
    private val mHandler = Handler(Looper.getMainLooper())
    private var schoolId = 0
    private var mDatabaseInteractor: IDatabaseInteractor =
        InteractorFactory.createDatabaseInteractor()
    private var mData: ArrayList<School>? = null
    private var mAdapter: SchoolsAdapter? = null
    private var filter: String? = null
    private val runSearchRunnable = Runnable { request(0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        schoolId = requireArguments().getInt(Extra.SCHOOL_ID)
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
        mAdapter = SchoolsAdapter(requireActivity(), mData ?: Collections.emptyList())
        mAdapter?.setListener(this)
        mRecyclerView.adapter = mAdapter
        if (firstRun) {
            request(0)
        }
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.school)
            .setView(root)
            .setNegativeButton(R.string.button_cancel, null)
            .create()
    }

    private fun request(offset: Int) {
        appendDisposable(
            mDatabaseInteractor.getSchools(accountId, schoolId, filter, COUNT_PER_REQUEST, offset)
                .fromIOToMain()
                .subscribe({ schools ->
                    onDataReceived(
                        offset,
                        schools
                    )
                }) { })
    }

    private fun onDataReceived(offset: Int, schools: List<School>) {
        if (offset == 0) {
            mData?.clear()
        }
        mData?.addAll(schools)
        mAdapter?.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(runSearchRunnable)
    }

    override fun onClick(school: School) {
        val intent = Bundle()
        intent.putParcelable(Extra.SCHOOL, school)
        intent.putInt(Extra.ID, school.id)
        intent.putString(Extra.TITLE, school.title)
        if (arguments != null) {
            intent.putAll(arguments)
        }
        parentFragmentManager.setFragmentResult(FilterEditFragment.REQUEST_FILTER_OPTION, intent)
        dismiss()
    }

    companion object {
        private const val COUNT_PER_REQUEST = 1000
        private const val RUN_SEARCH_DELAY = 1000
        fun newInstance(aid: Long, schoolId: Int, additional: Bundle?): SelectSchoolsDialog {
            val args = additional ?: Bundle()
            args.putInt(Extra.SCHOOL_ID, schoolId)
            args.putLong(Extra.ACCOUNT_ID, aid)
            val selectCityDialog = SelectSchoolsDialog()
            selectCityDialog.arguments = args
            return selectCityDialog
        }
    }
}