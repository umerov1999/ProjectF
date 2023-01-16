package dev.ragnarok.fenrir.realtime

import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.api.model.interfaces.Identificable
import dev.ragnarok.fenrir.model.Message

class TmpResult(id: Int, accountId: Long, capacity: Int) {
    val data: MutableList<Msg>
    val accountId: Long
    val id: Int
    fun prepare(id: Int): Msg {
        for (m in data) {
            if (m.getObjectId() == id) {
                return m
            }
        }
        return add(id)
    }

    fun appendDtos(dtos: List<VKApiMessage>): TmpResult {
        for (dto in dtos) {
            prepare(dto.id).setDto(dto)
        }
        return this
    }

    fun appendModel(messages: List<Message>): TmpResult {
        for (m in messages) {
            prepare(m.getObjectId()).setMessage(m)
        }
        return this
    }

    fun collectDtos(): List<VKApiMessage> {
        val dtos: MutableList<VKApiMessage> = ArrayList(data.size)
        for (msg in data) {
            msg.dto?.let { dtos.add(it) }
        }
        return dtos
    }

    fun add(id: Int): Msg {
        val msg = Msg(id)
        data.add(msg)
        return msg
    }

    fun setMissingIds(ids: Collection<Int>): TmpResult {
        for (msg in data) {
            msg.setAlreadyExists(!ids.contains(msg.getObjectId()))
        }
        return this
    }

    val allIds: List<Int>
        get() {
            if (data.isEmpty()) {
                return emptyList()
            }
            if (data.size == 1) {
                return listOf(data[0].getObjectId())
            }
            val ids: MutableList<Int> = ArrayList(data.size)
            for (msg in data) {
                ids.add(msg.getObjectId())
            }
            return ids
        }

    override fun toString(): String {
        return "[$id] -> $data"
    }

    class Msg internal constructor(private val id: Int) : Identificable {
        var isAlreadyExists = false
            private set
        var message: Message? = null
            private set
        var dto: VKApiMessage? = null
            private set
        private var backup: VKApiMessage? = null

        fun setDto(dto: VKApiMessage): Msg {
            this.dto = dto
            if (backup?.keyboard != null) {
                dto.keyboard = backup?.keyboard
                dto.payload = backup?.payload
            }
            return this
        }

        fun setMessage(message: Message?): Msg {
            this.message = message
            return this
        }

        fun setAlreadyExists(alreadyExists: Boolean): Msg {
            isAlreadyExists = alreadyExists
            return this
        }

        fun setBackup(backup: VKApiMessage?): Msg {
            this.backup = backup
            return this
        }

        override fun getObjectId(): Int {
            return id
        }
    }

    init {
        data = ArrayList(capacity)
        this.id = id
        this.accountId = accountId
    }
}