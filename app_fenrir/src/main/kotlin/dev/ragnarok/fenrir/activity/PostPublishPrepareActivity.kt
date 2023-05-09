package dev.ragnarok.fenrir.activity

import android.content.Context
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
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Icon
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Text
import dev.ragnarok.fenrir.model.WallEditorAttrs
import dev.ragnarok.fenrir.model.menu.AdvancedItem
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemesController.currentStyle
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.toast.CustomToast
import io.reactivex.rxjava3.disposables.CompositeDisposable

class PostPublishPrepareActivity : AppCompatActivity(), RecyclerMenuAdapter.ActionListener {
    private val compositeDisposable = CompositeDisposable()
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
                CustomToast.createCustomToast(this).setDuration(Toast.LENGTH_LONG)
                    .showToastError(R.string.error_post_creation_no_auth)
                finish()
            }
            streams = ActivityUtils.checkLocalStreams(this)
            mime = streams?.mime
            links = ActivityUtils.checkLinks(this)
            setLoading(true)
            val interactor = owners
            compositeDisposable.add(interactor.getCommunitiesWhereAdmin(
                accountId,
                admin = true,
                editor = true,
                moderator = false
            )
                .zipWith<Owner, List<Owner>>(
                    interactor.getBaseOwnerInfo(
                        accountId,
                        accountId,
                        IOwnersRepository.MODE_NET
                    )
                ) { owners: List<Owner>, owner: Owner ->
                    val result: MutableList<Owner> = ArrayList()
                    result.add(owner)
                    result.addAll(owners)
                    result
                }
                .fromIOToMain()
                .subscribe({ owners -> onOwnersReceived(owners) }) { throwable ->
                    onOwnersGetError(
                        throwable
                    )
                })
        }
        updateViews()
    }

    private fun onOwnersGetError(throwable: Throwable) {
        setLoading(false)
        CustomToast.createCustomToast(this).setDuration(Toast.LENGTH_LONG)
            .showToastError(Utils.firstNonEmptyString(throwable.message, throwable.toString()))
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
        compositeDisposable.dispose()
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