package dev.ragnarok.fenrir.link.internal

open class LinkActionAdapter : OwnerLinkSpanFactory.ActionListener {
    override fun onTopicLinkClicked(link: TopicLink) {}
    override fun onOwnerClick(ownerId: Int) {}
    override fun onOtherClick(URL: String) {}
}