package dev.ragnarok.fenrir.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.attachments.postcreate.PostCreateFragment
import dev.ragnarok.fenrir.getParcelableArrayListExtraCompat
import dev.ragnarok.fenrir.getParcelableExtraCompat
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.WallEditorAttrs

class PostCreateActivity : NoMainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val accountId = (intent.extras ?: return).getLong(Extra.ACCOUNT_ID)
            val streams = intent.getParcelableArrayListExtraCompat<Uri>("streams")
            val attrs: WallEditorAttrs = intent.getParcelableExtraCompat("attrs") ?: return
            val links = intent.getStringExtra("links")
            val mime = intent.getStringExtra(Extra.TYPE)
            val args = PostCreateFragment.buildArgs(
                accountId,
                attrs.getOwner().ownerId,
                EditingPostType.TEMP,
                null,
                attrs,
                streams,
                links,
                mime
            )
            val fragment = PostCreateFragment.newInstance(args)
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(getMainContainerViewId(), fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }
    }

    companion object {

        fun newIntent(
            context: Context,
            accountId: Long,
            attrs: WallEditorAttrs,
            streams: ArrayList<Uri>?,
            links: String?,
            mime: String?
        ): Intent {
            return Intent(context, PostCreateActivity::class.java)
                .putExtra(Extra.ACCOUNT_ID, accountId)
                .putParcelableArrayListExtra("streams", streams)
                .putExtra("attrs", attrs)
                .putExtra("links", links)
                .putExtra(Extra.TYPE, mime)
        }
    }
}