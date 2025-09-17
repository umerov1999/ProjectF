package dev.ragnarok.fenrir.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityUtils.StreamData
import dev.ragnarok.fenrir.activity.PostCreateActivity.Companion.newIntent
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.base.RecyclerMenuAdapter
import dev.ragnarok.fenrir.model.Icon
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Text
import dev.ragnarok.fenrir.model.WallEditorAttrs
import dev.ragnarok.fenrir.model.menu.AdvancedItem
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemesController.currentStyle
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.toast.CustomToast
import kotlinx.coroutines.flow.zip

class PostPublishPrepareActivity : AppCompatActivity(), RecyclerMenuAdapter.ActionListener {
    private val compositeJob = CompositeJob()
    private var adapter: RecyclerMenuAdapter? = null
    private var recyclerView: RecyclerView? = null
    private var proressView: View? = null
    private var streams: StreamData? = null
    private var links: String? = null
    private var mime: String? = null
    private var accountId = 0L
    private var loading = false
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.updateActivityContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(currentStyle())
        Utils.prepareDensity(this)
        Utils.registerColorsThorVG(this)
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setTranslucent(true)
        }
        window.setBackgroundDrawableResource(R.color.transparent)

        setContentView(R.layout.activity_post_publish_prepare)
        adapter = RecyclerMenuAdapter(R.layout.item_advanced_menu_alternative, emptyList())
        adapter?.setActionListener(this)
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = adapter
        proressView = findViewById(R.id.progress_view)
        if (savedInstanceState == null) {
            accountId = Settings.get().accounts().current
            if (accountId == ISettings.IAccountsSettings.INVALID_ID) {
                CustomToast.createCustomToast(this, null)?.setDuration(Toast.LENGTH_LONG)
                    ?.showToastError(R.string.error_post_creation_no_auth)
                finish()
            }
            streams = ActivityUtils.checkLocalStreams(this)
            mime = streams?.mime
            links = ActivityUtils.checkLinks(this)
            setLoading(true)
            val interactor = owners
            compositeJob.add(
                interactor.getCommunitiesWhereAdmin(
                    accountId,
                    admin = true,
                    editor = true,
                    moderator = false
                )
                    .zip(
                        interactor.getBaseOwnerInfo(
                            accountId,
                            accountId,
                            IOwnersRepository.MODE_NET
                        )
                    ) { owners, owner ->
                        val result: MutableList<Owner> = ArrayList()
                        result.add(owner)
                        result.addAll(owners)
                        result
                    }
                    .fromIOToMain({ owners -> onOwnersReceived(owners) }) { throwable ->
                        onOwnersGetError(
                            throwable
                        )
                    })
        }
        updateViews()
    }

    private fun onOwnersGetError(throwable: Throwable) {
        setLoading(false)
        CustomToast.createCustomToast(this, null)?.setDuration(Toast.LENGTH_LONG)
            ?.showToastError(Utils.firstNonEmptyString(throwable.message, throwable.toString()))
        finish()
    }

    private fun onOwnersReceived(owners: List<Owner>) {
        setLoading(false)
        if (owners.isEmpty()) {
            finish() // wtf???
            return
        }
        val iam = owners[0]
        val items: MutableList<AdvancedItem> = ArrayList()
        for (owner in owners) {
            val attrs = WallEditorAttrs(owner, iam)
            items.add(
                AdvancedItem(owner.ownerId, Text(owner.fullName))
                    .setIcon(Icon.fromUrl(owner.get100photoOrSmaller()))
                    .setSubtitle(Text("@" + owner.domain))
                    .setTag(attrs)
            )
        }
        adapter?.setItems(items)
    }

    private fun setLoading(loading: Boolean) {
        this.loading = loading
        updateViews()
    }

    private fun updateViews() {
        recyclerView?.visibility = if (loading) View.GONE else View.VISIBLE
        proressView?.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        compositeJob.cancel()
        super.onDestroy()
    }

    override fun onClick(item: AdvancedItem) {
        val attrs = item.tag as WallEditorAttrs
        val intent = newIntent(
            this,
            accountId,
            attrs,
            streams?.uris,
            links,
            mime
        )
        startActivity(intent)
        finish()
    }

    override fun onLongClick(item: AdvancedItem) {}
}