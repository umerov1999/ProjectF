package dev.ragnarok.fenrir.dialog.selectchairs

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
import dev.ragnarok.fenrir.model.database.Chair
import java.util.Collections

class SelectChairsDialog : AccountDependencyDialogFragment(), ChairsAdapter.Listener {
    private var chairId = 0
    private var mData: ArrayList<Chair>? = null
    private var mAdapter: ChairsAdapter? = null
    private val mDatabaseInteractor: IDatabaseInteractor =
        InteractorFactory.createDatabaseInteractor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chairId = requireArguments().getInt(Extra.CHAIR_ID)
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
        mAdapter = ChairsAdapter(requireActivity(), mData ?: Collections.emptyList())
        mAdapter?.setListener(this)
        mRecyclerView.adapter = mAdapter
        if (firstRun) {
            request(0)
        }
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.chair)
            .setView(root)
            .setNegativeButton(R.string.button_cancel, null)
            .create()
    }

    private fun request(offset: Int) {
        appendDisposable(
            mDatabaseInteractor.getChairs(accountId, chairId, COUNT_PER_REQUEST, offset)
                .fromIOToMain()
                .subscribe({ chairs ->
                    onDataReceived(
                        offset,
                        chairs
                    )
                }) { })
    }

    private fun onDataReceived(offset: Int, chairs: List<Chair>) {
        if (offset == 0) {
            mData?.clear()
        }
        mData?.addAll(chairs)
        mAdapter?.notifyDataSetChanged()
    }

    override fun onClick(chair: Chair) {
        val intent = Bundle()
        intent.putParcelable(Extra.CHAIR, chair)
        intent.putInt(Extra.ID, chair.id)
        intent.putString(Extra.TITLE, chair.title)
        if (arguments != null) {
            intent.putAll(arguments)
        }
        parentFragmentManager.setFragmentResult(FilterEditFragment.REQUEST_FILTER_OPTION, intent)
        dismiss()
    }

    companion object {
        private const val COUNT_PER_REQUEST = 1000
        fun newInstance(aid: Long, chairId: Int, additional: Bundle?): SelectChairsDialog {
            val args = additional ?: Bundle()
            args.putInt(Extra.CHAIR_ID, chairId)
            args.putLong(Extra.ACCOUNT_ID, aid)
            val selectCityDialog = SelectChairsDialog()
            selectCityDialog.arguments = args
            return selectCityDialog
        }
    }
}