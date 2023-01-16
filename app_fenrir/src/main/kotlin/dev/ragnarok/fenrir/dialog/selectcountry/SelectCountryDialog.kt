package dev.ragnarok.fenrir.dialog.selectcountry

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpDialogFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.search.filteredit.FilterEditFragment
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.model.database.Country

class SelectCountryDialog : BaseMvpDialogFragment<CountriesPresenter, ICountriesView>(),
    CountriesAdapter.Listener, ICountriesView {
    private var mAdapter: CountriesAdapter? = null
    private var mLoadingView: View? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = View.inflate(requireActivity(), R.layout.dialog_countries, null)
        val dialog: Dialog = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.countries_title)
            .setView(view)
            .setNegativeButton(R.string.button_cancel, null)
            .create()
        val filterView: TextInputEditText = view.findViewById(R.id.input)
        filterView.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireFilterEdit(
                    s
                )
            }
        })
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        mAdapter = CountriesAdapter(requireActivity(), emptyList())
        mAdapter?.setListener(this)
        recyclerView.adapter = mAdapter
        mLoadingView = view.findViewById(R.id.progress_root)
        fireViewCreated()
        return dialog
    }

    override fun onClick(country: Country) {
        presenter?.fireCountryClick(
            country
        )
    }

    override fun displayData(countries: List<Country>) {
        mAdapter?.setData(countries)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun displayLoading(loading: Boolean) {
        mLoadingView?.visibility = if (loading) View.VISIBLE else View.INVISIBLE
    }

    override fun returnSelection(country: Country) {
        val intent = Bundle()
        intent.putParcelable(Extra.COUNTRY, country)
        intent.putInt(Extra.ID, country.id)
        intent.putString(Extra.TITLE, country.title)
        if (arguments != null) {
            intent.putAll(arguments)
        }
        parentFragmentManager.setFragmentResult(FilterEditFragment.REQUEST_FILTER_OPTION, intent)
        dismiss()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CountriesPresenter> {
        return object : IPresenterFactory<CountriesPresenter> {
            override fun create(): CountriesPresenter {
                return CountriesPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    saveInstanceState
                )
            }
        }
    }
}