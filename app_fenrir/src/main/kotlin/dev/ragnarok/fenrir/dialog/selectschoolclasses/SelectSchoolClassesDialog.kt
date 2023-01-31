package dev.ragnarok.fenrir.dialog.selectschoolclasses

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
import dev.ragnarok.fenrir.model.database.SchoolClazz
import java.util.Collections

class SelectSchoolClassesDialog : AccountDependencyDialogFragment(), SchoolClassesAdapter.Listener {
    private var schoolClassesId = 0
    private var mDatabaseInteractor: IDatabaseInteractor =
        InteractorFactory.createDatabaseInteractor()
    private var mData: ArrayList<SchoolClazz>? = null
    private var mAdapter: SchoolClassesAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        schoolClassesId = requireArguments().getInt(Extra.SCHOOL_CLASS_ID)
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
        mAdapter = SchoolClassesAdapter(requireActivity(), mData ?: Collections.emptyList())
        mAdapter?.setListener(this)
        mRecyclerView.adapter = mAdapter
        if (firstRun) {
            request()
        }
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.school_class)
            .setView(root)
            .setNegativeButton(R.string.button_cancel, null)
            .create()
    }

    private fun request() {
        appendDisposable(
            mDatabaseInteractor.getSchoolClasses(accountId, schoolClassesId)
                .fromIOToMain()
                .subscribe({ clazzes -> onDataReceived(clazzes) }) { })
    }

    private fun onDataReceived(clazzes: List<SchoolClazz>) {
        mData?.clear()
        mData?.addAll(clazzes)
        mAdapter?.notifyDataSetChanged()
    }

    override fun onClick(schoolClazz: SchoolClazz) {
        val intent = Bundle()
        intent.putParcelable(Extra.SCHOOL_CLASS, schoolClazz)
        intent.putInt(Extra.ID, schoolClazz.id)
        intent.putString(Extra.TITLE, schoolClazz.title)
        if (arguments != null) {
            intent.putAll(arguments)
        }
        parentFragmentManager.setFragmentResult(FilterEditFragment.REQUEST_FILTER_OPTION, intent)
        dismiss()
    }

    companion object {
        fun newInstance(
            aid: Long,
            schoolClassesId: Int,
            additional: Bundle?
        ): SelectSchoolClassesDialog {
            val args = additional ?: Bundle()
            args.putInt(Extra.SCHOOL_CLASS_ID, schoolClassesId)
            args.putLong(Extra.ACCOUNT_ID, aid)
            val selectCityDialog = SelectSchoolClassesDialog()
            selectCityDialog.arguments = args
            return selectCityDialog
        }
    }
}