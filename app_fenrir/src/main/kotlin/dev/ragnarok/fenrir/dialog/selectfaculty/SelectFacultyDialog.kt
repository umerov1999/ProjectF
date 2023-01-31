package dev.ragnarok.fenrir.dialog.selectfaculty

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.dialog.base.AccountDependencyDialogFragment
import dev.ragnarok.fenrir.domain.IDatabaseInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.search.filteredit.FilterEditFragment
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.database.Faculty
import java.util.Collections

class SelectFacultyDialog : AccountDependencyDialogFragment(), FacultiesAdapter.Listener {
    private var facultyId = 0
    private var mDatabaseInteractor: IDatabaseInteractor =
        InteractorFactory.createDatabaseInteractor()
    private var mData: ArrayList<Faculty>? = null
    private var mAdapter: FacultiesAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        facultyId = requireArguments().getInt(Extra.FACULTY_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val root = View.inflate(requireActivity(), R.layout.dialog_simple_recycler_view, null)
        val mRecyclerView: RecyclerView = root.findViewById(R.id.list)
        mRecyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        var firstRun = false
        if (mData == null) {
            mData = ArrayList()
            firstRun = true
        }
        mAdapter = FacultiesAdapter(requireActivity(), mData ?: Collections.emptyList())
        mAdapter?.setListener(this)
        mRecyclerView.adapter = mAdapter
        if (firstRun) {
            request(0)
        }
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.faculty)
            .setView(root)
            .setNegativeButton(R.string.button_cancel, null)
            .create()
    }

    private fun request(offset: Int) {
        appendDisposable(
            mDatabaseInteractor.getFaculties(accountId, facultyId, COUNT_PER_REQUEST, offset)
                .fromIOToMain()
                .subscribe({ faculties ->
                    onDataReceived(
                        offset,
                        faculties
                    )
                }) { })
    }

    private fun onDataReceived(offset: Int, faculties: List<Faculty>) {
        if (offset == 0) {
            mData?.clear()
        }
        mData?.addAll(faculties)
        mAdapter?.notifyDataSetChanged()
    }

    override fun onClick(faculty: Faculty) {
        val intent = Bundle()
        intent.putParcelable(Extra.FACULTY, faculty)
        intent.putInt(Extra.ID, faculty.id)
        intent.putString(Extra.TITLE, faculty.title)
        if (arguments != null) {
            intent.putAll(arguments)
        }
        parentFragmentManager.setFragmentResult(FilterEditFragment.REQUEST_FILTER_OPTION, intent)
        dismiss()
    }

    companion object {
        private const val COUNT_PER_REQUEST = 1000
        fun newInstance(aid: Long, facultyId: Int, additional: Bundle?): SelectFacultyDialog {
            val args = additional ?: Bundle()
            args.putInt(Extra.FACULTY_ID, facultyId)
            args.putLong(Extra.ACCOUNT_ID, aid)
            val selectCityDialog = SelectFacultyDialog()
            selectCityDialog.arguments = args
            return selectCityDialog
        }
    }
}