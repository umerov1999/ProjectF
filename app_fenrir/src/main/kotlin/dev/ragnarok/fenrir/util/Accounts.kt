package dev.ragnarok.fenrir.util

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.syncSingle

object Accounts {
    fun showAccountSwitchedToast(context: Activity) {
        if (context.isFinishing || context.isDestroyed) {
            return
        }
        val aid = Settings.get()
            .accounts()
            .current
        if (aid == ISettings.IAccountsSettings.INVALID_ID) {
            return
        }
        val user = try {
            owners
                .getBaseOwnerInfo(aid, aid, IOwnersRepository.MODE_CACHE)
                .syncSingle()
        } catch (_: Exception) {
            // NotFountException
            return
        }

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Toast toast = new Toast(context);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setText(user.getFullName());
            toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, 0);
            toast.show();
            return;
        }
         */
        val view = View.inflate(context, R.layout.account_change_toast, null)
        val avatar = view.findViewById<ImageView>(R.id.avatar)
        val subtitle = view.findViewById<TextView>(R.id.subtitle)
        with()
            .load(user.maxSquareAvatar)
            .transform(CurrentTheme.createTransformationForAvatar())
            .into(avatar)
        subtitle.text = user.fullName
        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        @Suppress("deprecation")
        toast.view = view
        toast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.TOP, 0, 0)
        toast.show()
    }

    fun fromArgs(bundle: Bundle?): Long {
        return bundle?.getLong(Extra.ACCOUNT_ID) ?: ISettings.IAccountsSettings.INVALID_ID
    }
}