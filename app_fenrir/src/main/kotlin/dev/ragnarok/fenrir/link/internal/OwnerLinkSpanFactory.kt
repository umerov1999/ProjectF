package dev.ragnarok.fenrir.link.internal

import android.text.Spannable
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.safeCountOfMultiple
import java.util.Collections
import java.util.regex.Pattern
import kotlin.math.abs

object OwnerLinkSpanFactory {
    private val LINK_COMPARATOR =
        Comparator { link1: AbsInternalLink, link2: AbsInternalLink -> link1.start - link2.start }
    private val ownerPattern: Pattern = Pattern.compile("\\[(id|club)(\\d+)\\|([^]]+)]")
    private val topicCommentPattern: Pattern =
        Pattern.compile("\\[(id|club)(\\d*):bp(-\\d*)_(\\d*)\\|([^]]+)]")
    private val linkPattern: Pattern =
        Pattern.compile("\\[(https:[^]]+|http:[^]]+|vk\\.com[^]]+|)\\|([^]]+)]")

    fun findPatterns(
        input: String?,
        owners: Boolean,
        topics: Boolean
    ): List<AbsInternalLink>? {
        if (input.isNullOrEmpty()) {
            return null
        }
        val ownerLinks = if (owners) findOwnersLinks(input) else null
        val topicLinks = if (topics) findTopicLinks(input) else null
        val othersLinks = findOthersLinks(input)
        val count = safeCountOfMultiple(ownerLinks, topicLinks, othersLinks)
        if (count > 0) {
            val all: MutableList<AbsInternalLink> = ArrayList(count)
            if (ownerLinks.nonNullNoEmpty()) {
                all.addAll(ownerLinks)
            }
            if (topicLinks.nonNullNoEmpty()) {
                all.addAll(topicLinks)
            }
            if (othersLinks.nonNullNoEmpty()) {
                all.addAll(othersLinks)
            }
            Collections.sort(all, LINK_COMPARATOR)
            return all
        }
        return null
    }

    fun withSpans(
        input: String?,
        owners: Boolean,
        topics: Boolean,
        listener: ActionListener?
    ): Spannable? {
        if (input.isNullOrEmpty()) {
            return null
        }
        val ownerLinks = if (owners) findOwnersLinks(input) else null
        val topicLinks = if (topics) findTopicLinks(input) else null
        val othersLinks = findOthersLinks(input)
        val count = safeCountOfMultiple(ownerLinks, topicLinks, othersLinks)
        if (count > 0) {
            val all: MutableList<AbsInternalLink> = ArrayList(count)
            if (ownerLinks.nonNullNoEmpty()) {
                all.addAll(ownerLinks)
            }
            if (topicLinks.nonNullNoEmpty()) {
                all.addAll(topicLinks)
            }
            if (othersLinks.nonNullNoEmpty()) {
                all.addAll(othersLinks)
            }
            Collections.sort(all, LINK_COMPARATOR)
            val result = Spannable.Factory.getInstance().newSpannable(replace(input, all))
            for (link in all) {
                //TODO Нужно ли удалять spannable перед установкой новых
                val clickableSpan: ClickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        if (listener != null) {
                            if (link is TopicLink) {
                                listener.onTopicLinkClicked(link)
                            }
                            if (link is OwnerLink) {
                                listener.onOwnerClick(link.ownerId)
                            }
                            if (link is OtherLink) {
                                listener.onOtherClick(link.Link)
                            }
                        }
                    }
                }
                result.setSpan(
                    clickableSpan,
                    link.start,
                    link.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return result
        }
        return Spannable.Factory.getInstance().newSpannable(input)
    }

    private fun toLong(str: String?, pow_n: Int): Long {
        if (str.isNullOrEmpty()) {
            return Settings.get().accounts().current
        }
        try {
            return str.toLong() * pow_n
        } catch (ignored: NumberFormatException) {
        }
        return Settings.get().accounts().current
    }

    private fun findTopicLinks(input: String?): List<TopicLink>? {
        val matcher = input?.let { topicCommentPattern.matcher(it) }
        var links: MutableList<TopicLink>? = null
        while (matcher?.find() == true) {
            if (links == null) {
                links = ArrayList(1)
            }
            val link = TopicLink()
            val club = "club" == matcher.group(1)
            link.start = matcher.start()
            link.end = matcher.end()
            link.replyToOwner = toLong(matcher.group(2), if (club) -1 else 1)
            link.topicOwnerId = toLong(matcher.group(3), 1)
            link.replyToCommentId = matcher.group(4)?.toInt().orZero()
            link.targetLine = matcher.group(5)
            links.add(link)
        }
        return links
    }

    private fun findOwnersLinks(input: String?): List<OwnerLink>? {
        var links: MutableList<OwnerLink>? = null
        val matcher = input?.let { ownerPattern.matcher(it) }
        while (matcher?.find() == true) {
            if (links == null) {
                links = ArrayList(1)
            }
            val club = "club" == matcher.group(1)
            val ownerId = toLong(matcher.group(2), if (club) -1 else 1)
            val name = matcher.group(3)
            name?.let { OwnerLink(matcher.start(), matcher.end(), ownerId, it) }
                ?.let { links.add(it) }
        }
        return links
    }

    private fun findOthersLinks(input: String?): List<OtherLink>? {
        var links: MutableList<OtherLink>? = null
        val matcher = input?.let { linkPattern.matcher(it) }
        while (matcher?.find() == true) {
            if (links == null) {
                links = ArrayList(1)
            }
            matcher.group(1)
                ?.let {
                    matcher.group(2)
                        ?.let { it1 -> OtherLink(matcher.start(), matcher.end(), it, it1) }
                }
                ?.let { links.add(it) }
        }
        return links
    }

    fun getTextWithCollapseOwnerLinks(input: String?): String? {
        if (input.isNullOrEmpty()) {
            return null
        }
        val links = findOwnersLinks(input)
        return replace(input, links)
    }

    private fun replace(input: String?, links: List<AbsInternalLink>?): String? {
        if (links.isNullOrEmpty()) {
            return input
        }
        val result = StringBuilder(input ?: "")
        for (y in links.indices) {
            val link = links[y]
            if (link.targetLine.isNullOrEmpty()) {
                continue
            }
            val origLenght = link.end - link.start
            val newLenght = link.targetLine?.length ?: 0
            shiftLinks(links, link, origLenght - newLenght)
            link.targetLine?.let { result.replace(link.start, link.end, it) }
            link.end = link.end - (origLenght - newLenght)
        }
        return result.toString()
    }

    private fun shiftLinks(links: List<AbsInternalLink>?, after: AbsInternalLink?, count: Int) {
        links ?: return
        var shiftAllowed = false
        for (link in links) {
            if (shiftAllowed) {
                link.start = link.start - count
                link.end = link.end - count
            }
            if (link === after) {
                shiftAllowed = true
            }
        }
    }

    fun genOwnerLink(ownerId: Long, title: String?): String {
        return "[" + (if (ownerId > 0) "id" else "club") + abs(ownerId) + "|" + title + "]"
    }

    interface ActionListener {
        fun onTopicLinkClicked(link: TopicLink)
        fun onOwnerClick(ownerId: Long)
        fun onOtherClick(URL: String)
    }

}