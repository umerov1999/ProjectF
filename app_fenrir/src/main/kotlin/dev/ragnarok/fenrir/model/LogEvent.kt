package dev.ragnarok.fenrir.model

class LogEvent(val id: Int) {
    var date: Long = 0
        private set
    var type = 0
        private set
    var tag: String? = null
        private set
    var body: String? = null
        private set

    fun setDate(date: Long): LogEvent {
        this.date = date
        return this
    }

    fun setType(type: Int): LogEvent {
        this.type = type
        return this
    }

    fun setTag(tag: String?): LogEvent {
        this.tag = tag
        return this
    }

    fun setBody(body: String?): LogEvent {
        this.body = body
        return this
    }

    object Type {
        const val ERROR = 1
    }
}