package dev.ragnarok.fenrir.model

class LogEventWrapper(val event: LogEvent?) {
    var expanded = false
        private set

    fun setExpanded(expanded: Boolean) {
        this.expanded = expanded
    }
}