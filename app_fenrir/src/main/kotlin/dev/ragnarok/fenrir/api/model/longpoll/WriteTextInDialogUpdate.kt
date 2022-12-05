package dev.ragnarok.fenrir.api.model.longpoll

class WriteTextInDialogUpdate(var is_text: Boolean) :
    AbsLongpollEvent(if (is_text) ACTION_USER_WRITE_TEXT_IN_DIALOG else ACTION_USER_WRITE_VOICE_IN_DIALOG) {
    var peer_id = 0
    var from_ids: IntArray? = null
    var from_ids_count = 0
}