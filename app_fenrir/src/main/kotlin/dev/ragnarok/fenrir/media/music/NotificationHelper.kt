package dev.ragnarok.fenrir.media.music

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.MainActivity
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Utils

class NotificationHelper(private val mService: MusicPlaybackService) {
    private val mNotificationManager: NotificationManager?
    private var mNotificationBuilder: NotificationCompat.Builder? = null
    private val onGoing = Settings.get().other().isOngoing_player_notification

    @Suppress("DEPRECATION")
    fun buildNotification(
        context: Context, artistName: String?, trackName: String?,
        isPlaying: Boolean, cover: Bitmap?, mediaSessionToken: MediaSessionCompat.Token?
    ) {
        if (Utils.hasOreo()) {
            mNotificationManager?.createNotificationChannel(
                AppNotificationChannels.getAudioChannel(
                    context
                )
            )
        }

        // Notification Builder
        mNotificationBuilder =
            NotificationCompat.Builder(mService, AppNotificationChannels.AUDIO_CHANNEL_ID)
                .setShowWhen(false)
                .setVisibility(VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.song)
                .setContentTitle(artistName)
                .setContentText(trackName)
                .setContentIntent(getOpenIntent(context))
                .setLargeIcon(cover)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSessionToken)
                        .setShowActionsInCompactView(0, 1, 2)
                )
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.skip_previous,
                        context.resources.getString(R.string.previous),
                        retreivePlaybackActions(ACTION_PREV)
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        if (isPlaying) R.drawable.pause_notification else R.drawable.play_notification,
                        context.resources.getString(if (isPlaying) R.string.pause else R.string.play),
                        retreivePlaybackActions(ACTION_PLAY_PAUSE)
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.skip_next,
                        context.resources.getString(R.string.next),
                        retreivePlaybackActions(ACTION_NEXT)
                    )
                ).addAction(
                    NotificationCompat.Action(
                        R.drawable.close,
                        context.resources.getString(R.string.close),
                        retreivePlaybackActions(SWIPE_DISMISS_ACTION)
                    )
                )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            mNotificationBuilder?.setDeleteIntent(retreivePlaybackActions(SWIPE_DISMISS_ACTION))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            mNotificationBuilder?.priority = NotificationManager.IMPORTANCE_HIGH
        else
            mNotificationBuilder?.priority = Notification.PRIORITY_MAX
        if (isPlaying) {
            if (onGoing) {
                mNotificationBuilder?.setOngoing(true)
            }
            mService.goForeground(
                FENRIR_MUSIC_SERVICE,
                mNotificationBuilder?.build(),
                mNotificationManager
            )
        } else if (onGoing) {
            mNotificationBuilder?.setOngoing(false)
        }
    }

    fun killNotification() {
        mService.outForeground(true)
        mNotificationBuilder = null
    }

    @SuppressLint("RestrictedApi")
    fun updatePlayState(isPlaying: Boolean) {
        if (mNotificationBuilder == null || mNotificationManager == null) {
            return
        }
        if (!isPlaying) {
            mService.outForeground(false)
        }
        //Remove pause action
        mNotificationBuilder?.mActions?.removeAt(1)
        mNotificationBuilder?.mActions?.add(
            1, NotificationCompat.Action(
                if (isPlaying) R.drawable.pause_notification else R.drawable.play_notification,
                null,
                retreivePlaybackActions(ACTION_PLAY_PAUSE)
            )
        )
        if (AppPerms.hasNotificationPermissionSimple(mService)) {
            mNotificationManager.notify(FENRIR_MUSIC_SERVICE, mNotificationBuilder?.build())
        }
    }

    private fun getOpenIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.action = MainActivity.ACTION_OPEN_AUDIO_PLAYER
        return PendingIntent.getActivity(
            mService,
            0,
            intent,
            Utils.makeMutablePendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)
        )
    }

    private fun retreivePlaybackActions(which: Int): PendingIntent? {
        val action: Intent
        val pendingIntent: PendingIntent
        val serviceName = ComponentName(mService, MusicPlaybackService::class.java)
        when (which) {
            ACTION_PLAY_PAUSE -> {
                // Play and pause
                action = Intent(MusicPlaybackService.TOGGLEPAUSE_ACTION)
                action.component = serviceName
                pendingIntent = PendingIntent.getService(
                    mService,
                    ACTION_PLAY_PAUSE,
                    action,
                    Utils.makeMutablePendingIntent(0)
                )
                return pendingIntent
            }

            ACTION_NEXT -> {
                // Skip tracks
                action = Intent(MusicPlaybackService.NEXT_ACTION)
                action.component = serviceName
                pendingIntent = PendingIntent.getService(
                    mService,
                    ACTION_NEXT,
                    action,
                    Utils.makeMutablePendingIntent(0)
                )
                return pendingIntent
            }

            ACTION_PREV -> {
                // Previous tracks
                action = Intent(MusicPlaybackService.PREVIOUS_ACTION)
                action.component = serviceName
                pendingIntent = PendingIntent.getService(
                    mService,
                    ACTION_PREV,
                    action,
                    Utils.makeMutablePendingIntent(0)
                )
                return pendingIntent
            }

            SWIPE_DISMISS_ACTION -> {
                // Stop and collapse the notification
                action = Intent(MusicPlaybackService.SWIPE_DISMISS_ACTION)
                action.component = serviceName
                pendingIntent = PendingIntent.getService(
                    mService,
                    SWIPE_DISMISS_ACTION,
                    action,
                    Utils.makeMutablePendingIntent(0)
                )
                return pendingIntent
            }

            else -> {
            }
        }
        return null
    }

    companion object {
        private const val FENRIR_MUSIC_SERVICE = 1
        private const val ACTION_PLAY_PAUSE = 1
        private const val ACTION_NEXT = 2
        private const val ACTION_PREV = 3
        private const val SWIPE_DISMISS_ACTION = 4
    }

    init {
        mNotificationManager = mService
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}
