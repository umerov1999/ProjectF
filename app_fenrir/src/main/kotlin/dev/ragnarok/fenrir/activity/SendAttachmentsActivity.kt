package dev.ragnarok.fenrir.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.ModelsBundle
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.util.MainActivityTransforms
import dev.ragnarok.fenrir.util.ViewUtils

/**
 * Тот же MainActivity, предназначенный для шаринга контента
 * Отличие только в том, что этот активити может существовать в нескольких экземплярах
 */
class SendAttachmentsActivity : MainActivity() {
    @MainActivityTransforms
    override fun getMainActivityTransform(): Int {
        return MainActivityTransforms.SEND_ATTACHMENTS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // потому, что в onBackPressed к этому числу будут прибавлять 2000 !!!! и выход за границы
        mLastBackPressedTime = Long.MAX_VALUE - DOUBLE_BACK_PRESSED_TIMEOUT
    }

    public override fun onDestroy() {
        ViewUtils.keyboardHide(this)
        super.onDestroy()
    }

    companion object {
        fun startForSendAttachments(context: Context, accountId: Long, bundle: ModelsBundle?) {
            val intent = Intent(context, SendAttachmentsActivity::class.java)
            intent.action = ACTION_SEND_ATTACHMENTS
            intent.putExtra(EXTRA_INPUT_ATTACHMENTS, bundle)
            intent.putExtra(EXTRA_NO_REQUIRE_PIN, true)
            intent.putExtra(Extra.PLACE, PlaceFactory.getDialogsPlace(accountId, accountId, null))
            context.startActivity(intent)
        }

        fun startForSendAttachmentsIntent(
            context: Context,
            accountId: Long,
            bundle: ModelsBundle?
        ): Intent {
            val intent = Intent(context, SendAttachmentsActivity::class.java)
            intent.action = ACTION_SEND_ATTACHMENTS
            intent.putExtra(EXTRA_INPUT_ATTACHMENTS, bundle)
            intent.putExtra(EXTRA_NO_REQUIRE_PIN, true)
            intent.putExtra(Extra.PLACE, PlaceFactory.getDialogsPlace(accountId, accountId, null))
            return intent
        }

        fun startForSendAttachmentsFor(
            context: Context,
            accountId: Long,
            peer: Peer,
            bundle: ModelsBundle?
        ) {
            val intent = Intent(context, ChatActivity::class.java)
            intent.action = ChatActivity.ACTION_OPEN_PLACE
            intent.putExtra(EXTRA_INPUT_ATTACHMENTS, bundle)
            intent.putExtra(Extra.PLACE, PlaceFactory.getChatPlace(accountId, accountId, peer))
            context.startActivity(intent)
        }

        fun startForSendLink(context: Context, link: String?) {
            val intent = Intent(context, SendAttachmentsActivity::class.java)
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, link)
            context.startActivity(intent)
        }

        fun startForSendAttachments(context: Context, accountId: Long, model: AbsModel) {
            startForSendAttachments(context, accountId, ModelsBundle(1).append(model))
        }

        fun startForSendAttachmentsIntent(
            context: Context,
            accountId: Long,
            model: AbsModel
        ): Intent {
            return startForSendAttachmentsIntent(context, accountId, ModelsBundle(1).append(model))
        }

        fun startForSendAttachmentsFor(
            context: Context,
            accountId: Long,
            peer: Peer,
            model: AbsModel
        ) {
            startForSendAttachmentsFor(context, accountId, peer, ModelsBundle(1).append(model))
        }
    }
}