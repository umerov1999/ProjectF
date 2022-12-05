package dev.ragnarok.fenrir.push

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.annotation.DrawableRes
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.ImageHelper.getRoundedBitmap
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.dpToPx
import dev.ragnarok.fenrir.util.Utils.hasFlag
import dev.ragnarok.fenrir.util.Utils.hasOreo
import io.reactivex.rxjava3.core.Single
import java.io.IOException

object NotificationUtils {
    fun loadRoundedImageRx(
        context: Context,
        url: String?,
        @DrawableRes ifErrorOrEmpty: Int
    ): Single<Bitmap> {
        val app = context.applicationContext
        return Single.fromCallable { loadRoundedImage(app, url, ifErrorOrEmpty) }
    }

    fun loadRoundedImage(
        context: Context,
        url: String?,
        @DrawableRes ifErrorOrEmpty: Int
    ): Bitmap {
        val app = context.applicationContext
        val transformation = CurrentTheme.createTransformationForAvatar()
        val size = dpToPx(64f, app).toInt()
        return if (url.nonNullNoEmpty()) {
            try {
                with()
                    .load(url)
                    .resize(size, size)
                    .centerCrop()
                    .transform(transformation)
                    .get()!!
            } catch (e: IOException) {
                loadRoundedImageFromResources(app, ifErrorOrEmpty, transformation, size)
            }
        } else {
            loadRoundedImageFromResources(app, ifErrorOrEmpty, transformation, size)
        }
    }

    private fun loadRoundedImageFromResources(
        context: Context,
        @DrawableRes res: Int,
        transformation: Transformation,
        size: Int
    ): Bitmap {
        return try {
            with()
                .load(res)
                .resize(size, size)
                .transform(transformation)
                .centerCrop()
                .get()!!
        } catch (e: IOException) {
            e.printStackTrace()
            val bitmap = BitmapFactory.decodeResource(context.resources, res)
            getRoundedBitmap(bitmap)
        }!!
    }

    @JvmOverloads
    fun optInt(extras: Bundle, name: String?, defaultValue: Int = 0): Int {
        val value = extras.getString(name)
        return try {
            if (value.isNullOrEmpty()) defaultValue else value.toInt()
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    @Suppress("DEPRECATION")
    fun configOtherPushNotification(notification: Notification) {
        if (hasOreo()) {
            return
        }
        val mask = Settings.get()
            .notifications()
            .otherNotificationMask
        if (hasFlag(mask, ISettings.INotificationSettings.FLAG_LED)) {
            notification.ledARGB = -0xffff01
            notification.flags = notification.flags or Notification.FLAG_SHOW_LIGHTS
            notification.ledOnMS = 100
            notification.ledOffMS = 1000
        }
        if (hasFlag(mask, ISettings.INotificationSettings.FLAG_VIBRO)) notification.defaults =
            notification.defaults or Notification.DEFAULT_VIBRATE
        if (hasFlag(mask, ISettings.INotificationSettings.FLAG_SOUND)) {
            notification.sound = Settings.get()
                .notifications()
                .feedbackRingtoneUri
        }
    }
}