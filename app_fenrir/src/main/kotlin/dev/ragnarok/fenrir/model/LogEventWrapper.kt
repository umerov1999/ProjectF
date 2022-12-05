package dev.ragnarok.fenrir.model

class LogEventWrapper(private val event: LogEvent?) {
    private var expanded = false
    fun getEvent(): LogEvent? {
        return event
    }

    fun isExpanded(): Boolean {
        return expanded
    }

    fun setExpanded(expanded: Boolean) {
        this.expanded = expanded
    }
}