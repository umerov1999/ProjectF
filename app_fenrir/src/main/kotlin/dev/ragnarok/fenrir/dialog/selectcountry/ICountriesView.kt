package dev.ragnarok.fenrir.dialog.selectcountry

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.database.Country

interface ICountriesView : IMvpView, IErrorView {
    fun displayData(countries: List<Country>)
    fun notifyDataSetChanged()
    fun displayLoading(loading: Boolean)
    fun returnSelection(country: Country)
}