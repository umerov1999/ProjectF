package dev.ragnarok.filegallery.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dev.ragnarok.filegallery.R

object AppNotificationChannels {
    const val AUDIO_CHANNEL_ID = "audio_channel"
    const val DOWNLOAD_CHANNEL_ID = "download_channel"

    fun getAudioChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.audio_channel)
        val channel =
            NotificationChannel(AUDIO_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW)
        channel.enableLights(false)
        channel.enableVibration(false)
        return channel
    }

    fun getDownloadChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.downloading)
        val channel = NotificationChannel(
            DOWNLOAD_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.enableLights(true)
        channel.enableVibration(false)
        return channel
    }
}