package dev.ragnarok.fenrir.link

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.fragment.app.FragmentActivity
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast

class LinkSpan(
    private val context: Context,
    private val link: String,
    private val is_underline: Boolean
) : ClickableSpan() {
    override fun onClick(widget: View) {
        if (Settings.get().other().is_notification_force_link()) {
            LinkHelper.openUrl(context as Activity, Settings.get().accounts().current, link, false)
            return
        }
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.add(
            OptionRequest(
                R.id.button_ok,
                context.getString(R.string.open),
                R.drawable.web,
                true
            )
        )
        menus.add(
            OptionRequest(
                R.id.button_cancel,
                context.getString(R.string.copy_simple),
                R.drawable.content_copy,
                true
            )
        )
        menus.show(
            (context as FragmentActivity).supportFragmentManager,
            "left_options"
        ) { _, option ->
            when (option.id) {
                R.id.button_ok -> {
                    LinkHelper.openUrl(
                        context as Activity,
                        Settings.get().accounts().current,
                        link, false
                    )
                }

                R.id.button_cancel -> {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    val clip = ClipData.newPlainText("response", link)
                    clipboard?.setPrimaryClip(clip)
                    createCustomToast(context).showToast(R.string.copied_to_clipboard)
                }
            }
        }
    }

    override fun updateDrawState(textPaint: TextPaint) {
        super.updateDrawState(textPaint)
        if (is_underline) textPaint.color =
            CurrentTheme.getColorPrimary(context) else textPaint.color =
            CurrentTheme.getColorSecondary(
                context
            )
        textPaint.isUnderlineText = is_underline
    }
}