package dev.ragnarok.fenrir.model

class LastReadId(private var outgoing: Int, private var incoming: Int) {
    fun getOutgoing(): Int {
        return outgoing
    }

    fun setOutgoing(outgoing: Int): LastReadId {
        this.outgoing = outgoing
        return this
    }

    fun getIncoming(): Int {
        return incoming
    }

    fun setIncoming(parcel: Int): LastReadId {
        incoming = parcel
        return this
    }
}