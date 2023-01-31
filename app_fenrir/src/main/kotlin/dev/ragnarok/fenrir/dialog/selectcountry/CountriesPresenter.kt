package dev.ragnarok.fenrir.dialog.selectcountry

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IDatabaseInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.database.Country
import dev.ragnarok.fenrir.util.Objects.safeEquals
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import java.util.Locale

class CountriesPresenter(private val accountId: Long, savedInstanceState: Bundle?) :
    RxSupportPresenter<ICountriesView>(savedInstanceState) {
    private val databaseInteractor: IDatabaseInteractor
    private val filtered: MutableList<Country>
    private var countries: List<Country>
    private var filter: String? = null
    private var loadingNow = false
    override fun onGuiCreated(viewHost: ICountriesView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(filtered)
        resolveLoadingView()
    }

    private fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveLoadingView()
    }

    private fun resolveLoadingView() {
        view?.displayLoading(loadingNow)
    }

    private fun onDataReceived(countries: List<Country>) {
        setLoadingNow(false)
        this.countries = countries
        reFillFilteredData()
        view?.notifyDataSetChanged()
    }

    fun fireFilterEdit(text: CharSequence?) {
        if (safeEquals(text.toString(), filter)) {
            return
        }
        filter = text.toString()
        reFillFilteredData()
        view?.notifyDataSetChanged()
    }

    private fun reFillFilteredData() {
        filtered.clear()
        val pFilter = filter
        if (pFilter.isNullOrEmpty()) {
            filtered.addAll(countries)
            return
        }
        val lowerFilter = pFilter.lowercase(Locale.getDefault())
        for (country in countries) {
            if (country.title?.lowercase(Locale.getDefault())?.contains(lowerFilter) == true) {
                filtered.add(country)
            }
        }
    }

    private fun onDataGetError(t: Throwable) {
        setLoadingNow(false)
        showError(getCauseIfRuntime(t))
    }

    private fun requestData() {
        setLoadingNow(true)
        appendDisposable(databaseInteractor.getCountries(accountId, false)
            .fromIOToMain()
            .subscribe({ countries -> onDataReceived(countries) }) { t ->
                onDataGetError(
                    t
                )
            })
    }

    fun fireCountryClick(country: Country) {
        view?.returnSelection(country)
    }

    init {
        countries = ArrayList()
        filtered = ArrayList()
        databaseInteractor = InteractorFactory.createDatabaseInteractor()
        requestData()
    }
}