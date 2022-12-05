package dev.ragnarok.fenrir.model

class PeerUpdate(val accountId: Int, val peerId: Int) {
    var readIn: Read? = null
    var readOut: Read? = null
    var lastMessage: LastMessage? = null
    var unread: Unread? = null
    var pin: Pin? = null
    var title: Title? = null

    class Read(val messageId: Int)

    class Unread(val count: Int)

    class LastMessage(val messageId: Int)

    class Pin(val pinned: Message?)

    class Title(val title: String?)
}