package dev.ragnarok.fenrir.fragment.logs

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.LogEventType
import dev.ragnarok.fenrir.model.LogEventWrapper

interface ILogsView : IMvpView, IErrorView {
    fun displayTypes(types: MutableList<LogEventType>)
    fun displayData(events: MutableList<LogEventWrapper>)
    fun showRefreshing(refreshing: Boolean)
    fun notifyEventDataChanged()
    fun notifyTypesDataChanged()
    fun setEmptyTextVisible(visible: Boolean)
}