package dev.ragnarok.fenrir.listener

import dev.ragnarok.fenrir.model.drawer.SectionMenuItem

interface OnSectionResumeCallback {
    fun onSectionResume(sectionDrawerItem: SectionMenuItem)
    fun onChatResume(accountId: Long, peerId: Long, title: String?, imgUrl: String?)
    fun onClearSelection()
    fun readAllNotifications()
}