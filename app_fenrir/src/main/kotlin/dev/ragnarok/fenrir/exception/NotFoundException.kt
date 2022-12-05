package dev.ragnarok.fenrir.exception

class NotFoundException : Exception {
    constructor()
    constructor(message: String?) : super(message)
}