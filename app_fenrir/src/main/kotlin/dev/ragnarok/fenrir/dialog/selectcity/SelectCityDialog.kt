package dev.ragnarok.fenrir.dialog.selectcity

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
import dev.ragnarok.fenrir.model.City
import java.util.Collections

class SelectCityDialog : AccountDependencyDialogFragment(), CitiesAdapter.Listener {
    private val mHandler = Handler(Looper.getMainLooper())
    private var cityId = 0
    private var mData: ArrayList<City>? = null
    private var mAdapter: CitiesAdapter? = null
    private var filter: String? = null
    private val databaseInteractor: IDatabaseInteractor =
        InteractorFactory.createDatabaseInteractor()
    private val mRunSearchRunnable = Runnable { request(0) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cityId = requireArguments().getInt(Extra.CITY_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val root = View.inflate(requireActivity(), R.layout.dialog_country_or_city_select, null)
        val input: TextInputEditText = root.findViewById(R.id.input)
        input.setText(filter)
        input.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable?) {
                filter = s.toString()
                mHandler.removeCallbacks(mRunSearchRunnable)
                mHandler.postDelayed(mRunSearchRunnable, RUN_SEARCH_DELAY.toLong())
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
        mAdapter = CitiesAdapter(requireActivity(), mData ?: Collections.emptyList())
        mAdapter?.setListener(this)
        mRecyclerView.adapter = mAdapter
        if (firstRun) {
            request(0)
        }
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.city)
            .setView(root)
            .setNegativeButton(R.string.button_cancel, null)
            .create()
    }

    private fun request(offset: Int) {
        appendDisposable(
            databaseInteractor.getCities(
                accountId,
                cityId,
                filter,
                true,
                COUNT_PER_REQUEST,
                offset
            )
                .fromIOToMain()
                .subscribe({ cities ->
                    onRequestFinished(
                        cities,
                        offset
                    )
                }) { })
    }

    private fun onRequestFinished(cities: List<City>, offset: Int) {
        if (offset == 0) {
            mData?.clear()
        }
        mData?.addAll(cities)
        mAdapter?.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(mRunSearchRunnable)
    }

    override fun onClick(city: City) {
        val intent = Bundle()
        intent.putParcelable(Extra.CITY, city)
        intent.putInt(Extra.ID, city.id)
        intent.putString(Extra.TITLE, city.title)
        if (arguments != null) {
            intent.putAll(arguments)
        }
        parentFragmentManager.setFragmentResult(FilterEditFragment.REQUEST_FILTER_OPTION, intent)
        dismiss()
    }

    companion object {
        private const val COUNT_PER_REQUEST = 1000
        private const val RUN_SEARCH_DELAY = 1000
        fun newInstance(aid: Long, cityId: Int, additional: Bundle?): SelectCityDialog {
            val args = additional ?: Bundle()
            args.putInt(Extra.CITY_ID, cityId)
            args.putLong(Extra.ACCOUNT_ID, aid)
            val selectCityDialog = SelectCityDialog()
            selectCityDialog.arguments = args
            return selectCityDialog
        }
    }
}