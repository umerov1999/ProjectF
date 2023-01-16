package dev.ragnarok.fenrir.link.internal

open class LinkActionAdapter : OwnerLinkSpanFactory.ActionListener {
    override fun onTopicLinkClicked(link: TopicLink) {}
    override fun onOwnerClick(ownerId: Long) {}
    override fun onOtherClick(URL: String) {}
}