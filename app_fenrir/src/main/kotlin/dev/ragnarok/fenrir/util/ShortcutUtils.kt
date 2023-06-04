package dev.ragnarok.fenrir.util

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.ContextCompat
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.MainActivity
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.io.IOException

object ShortcutUtils {
    private const val SHURTCUT_ACTION = "com.android.launcher.action.INSTALL_SHORTCUT"
    private const val MAX_DYNAMIC_COUNT = 5
    private fun getLauncherIconSize(context: Context): Int {
        return ContextCompat.getDrawable(context, R.mipmap.ic_launcher)!!.intrinsicWidth
    }

    @Throws(IOException::class)
    private fun createAccountShortcut(
        context: Context,
        accountId: Long,
        title: String,
        url: String
    ) {
        var avatar: Bitmap? = null
        if (url.nonNullNoEmpty()) {
            val size = getLauncherIconSize(context)
            avatar = with()
                .load(url)
                .transform(RoundTransformation())
                .resize(size, size)
                .get()
        }
        val intent = Intent(context.applicationContext, MainActivity::class.java)
        intent.action = MainActivity.ACTION_SWITCH_ACCOUNT
        intent.putExtra(Extra.ACCOUNT_ID, accountId)
        val id = "fenrir_account_$accountId"
        sendShortcutBroadcast(context, id, intent, title, avatar)
    }

    fun createAccountShortcutRx(
        context: Context,
        accountId: Long,
        title: String,
        url: String
    ): Completable {
        return Includes.stores.tempStore().addShortcut(
            "fenrir_account_$accountId",
            url.ifEmpty { VKApiUser.CAMERA_50 }, title
        ).andThen {
            createAccountShortcut(context, accountId, title, url)
            it.onComplete()
        }
    }

    @Throws(IOException::class)
    private fun createWallShortcut(
        context: Context,
        accountId: Long,
        ownerId: Long,
        title: String,
        url: String?
    ) {
        var avatar: Bitmap? = null
        if (url.nonNullNoEmpty()) {
            val size = getLauncherIconSize(context)
            avatar = with()
                .load(url)
                .transform(RoundTransformation())
                .resize(size, size)
                .get()
        }
        val intent = Intent(context.applicationContext, MainActivity::class.java)
        intent.action = MainActivity.ACTION_SHORTCUT_WALL
        intent.putExtra(Extra.ACCOUNT_ID, accountId)
        intent.putExtra(Extra.OWNER_ID, ownerId)
        val id = "fenrir_wall_" + ownerId + "_aid_" + accountId
        sendShortcutBroadcast(context, id, intent, title, avatar)
    }


    fun createWallShortcutRx(
        context: Context,
        accountId: Long,
        ownerId: Long,
        title: String?,
        url: String?
    ): Completable {
        return Includes.stores.tempStore()
            .addShortcut(
                "fenrir_wall_" + ownerId + "_aid_" + accountId,
                url.nonNullNoEmpty({
                    it
                }, {
                    VKApiUser.CAMERA_50
                }), title ?: ("id$ownerId")
            ).andThen { its ->
                createWallShortcut(
                    context,
                    accountId,
                    ownerId,
                    title ?: ("id$ownerId"),
                    url.nonNullNoEmpty({
                        it
                    }, {
                        VKApiUser.CAMERA_50
                    })
                )
                its.onComplete()
            }
    }

    @Suppress("DEPRECATION")
    private fun sendShortcutBroadcast(
        context: Context,
        shortcutId: String,
        shortcutIntent: Intent,
        title: String,
        bitmap: Bitmap?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val icon = Icon.createWithBitmap(bitmap)
            val shortcutInfo = ShortcutInfo.Builder(context, shortcutId)
                .setIcon(icon)
                .setShortLabel(title)
                .setIntent(shortcutIntent)
                .build()
            val manager = context.getSystemService(
                ShortcutManager::class.java
            )
            manager?.requestPinShortcut(shortcutInfo, null)
        } else {
            val intent = Intent()
            intent.action = SHURTCUT_ACTION
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title)
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap)
            context.sendBroadcast(intent)
        }
    }

    fun chatOpenIntent(
        context: Context,
        url: String?,
        accountId: Long,
        peerId: Long,
        title: String?
    ): Intent {
        val intent = Intent(context.applicationContext, MainActivity::class.java)
        intent.action = MainActivity.ACTION_CHAT_FROM_SHORTCUT
        intent.putExtra(Extra.PEER_ID, peerId)
        intent.putExtra(Extra.TITLE, title)
        intent.putExtra(Extra.IMAGE, url)
        intent.putExtra(Extra.ACCOUNT_ID, accountId)
        return intent
    }

    @Throws(IOException::class)
    private fun createChatShortcut(
        context: Context,
        url: String,
        accountId: Long,
        peerId: Long,
        title: String
    ) {
        val id = "fenrir_peer_" + peerId + "_aid_" + accountId
        val bm = createBitmap(context, url)
        val intent = chatOpenIntent(context, url, accountId, peerId, title)
        sendShortcutBroadcast(context, id, intent, title, bm)
    }


    fun createChatShortcutRx(
        context: Context,
        url: String,
        accountId: Long,
        peerId: Long,
        title: String
    ): Completable {
        return Includes.stores.tempStore()
            .addShortcut(
                "fenrir_peer_" + peerId + "_aid_" + accountId,
                url.nonNullNoEmpty({
                    it
                }, {
                    VKApiUser.CAMERA_50
                }), title
            ).andThen {
                createChatShortcut(context, url, accountId, peerId, title)
                it.onComplete()
            }
    }

    private fun loadRoundAvatar(url: String): Single<Bitmap> {
        return Single.fromCallable {
            with()
                .load(url)
                .transform(RoundTransformation())
                .get()!!
        }
    }


    @SuppressLint("ReportShortcutUsage")
    @TargetApi(Build.VERSION_CODES.N_MR1)
    fun addDynamicShortcut(context: Context, accountId: Long, peer: Peer): Completable {
        val app = context.applicationContext
        return loadRoundAvatar(peer.avaUrl ?: VKApiUser.CAMERA_50)
            .flatMapCompletable { bitmap: Bitmap? ->
                Completable.fromAction {
                    val manager = app.getSystemService(ShortcutManager::class.java)
                    val infos: ArrayList<ShortcutInfo> = ArrayList(manager.dynamicShortcuts)
                    val mustBeRemoved: MutableList<String> = ArrayList(1)
                    if (infos.size >= MAX_DYNAMIC_COUNT) {
                        infos.sortWith { o1: ShortcutInfo, o2: ShortcutInfo ->
                            o1.rank.compareTo(o2.rank)
                        }
                        val infoWhichMustBeRemoved = infos[infos.size - 1]
                        mustBeRemoved.add(infoWhichMustBeRemoved.id)
                    }
                    val title = peer.getTitle()
                    val id = "fenrir_peer_" + peer.id + "_aid_" + accountId
                    val avaurl = peer.avaUrl
                    val intent = chatOpenIntent(app, avaurl, accountId, peer.id, title)
                    val rank = 0
                    val builder = ShortcutInfo.Builder(app, id)
                        .setShortLabel(title ?: ("id" + peer.id))
                        .setIntent(intent)
                        .setRank(rank)
                    val icon = Icon.createWithBitmap(bitmap)
                    builder.setIcon(icon)
                    if (mustBeRemoved.isNotEmpty()) {
                        manager.removeDynamicShortcuts(mustBeRemoved)
                    }
                    manager.addDynamicShortcuts(listOf(builder.build()))
                }
            }
    }

    @Throws(IOException::class)
    private fun createBitmap(mContext: Context, url: String): Bitmap? {
        val appIconSize = getLauncherIconSize(mContext)
        val bm: Bitmap?
        val transformation: Transformation = RoundTransformation()
        bm = if (url.isEmpty()) {
            with()
                .load(R.drawable.ic_avatar_unknown)
                .transform(transformation)
                .resize(appIconSize, appIconSize)
                .get()
        } else {
            with()
                .load(url)
                .transform(transformation)
                .resize(appIconSize, appIconSize)
                .get()
        }
        return bm
    }
}