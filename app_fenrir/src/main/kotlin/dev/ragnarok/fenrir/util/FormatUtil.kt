package dev.ragnarok.fenrir.util

import android.content.Context
import android.text.Spannable
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import java.text.DateFormat
import java.util.*

object FormatUtil {

    fun formatCommunityBanInfo(
        context: Context, adminId: Long, adminName: String?,
        endDate: Long, adminClickListener: OwnerLinkSpanFactory.ActionListener?
    ): Spannable? {
        val endDateString: String = if (endDate == 0L) {
            context.getString(R.string.forever).lowercase(Locale.getDefault())
        } else {
            val date = Date(endDate * 1000)
            val formattedDate = DateFormat.getDateInstance().format(date)
            val formattedTime = DateFormat.getTimeInstance().format(date)
            context.getString(R.string.until_date_time, formattedDate, formattedTime)
        }
        val adminLink = OwnerLinkSpanFactory.genOwnerLink(adminId, adminName)
        val fullInfoText =
            context.getString(R.string.ban_admin_and_date_text, adminLink, endDateString)
        return OwnerLinkSpanFactory.withSpans(
            fullInfoText,
            owners = true,
            topics = false,
            listener = adminClickListener
        )
    }
}