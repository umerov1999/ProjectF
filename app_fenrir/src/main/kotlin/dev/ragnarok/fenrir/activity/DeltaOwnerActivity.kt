package dev.ragnarok.fenrir.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.slidr.Slidr.attach
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.activity.slidr.model.SlidrPosition
import dev.ragnarok.fenrir.fragment.absownerslist.OwnersAdapter
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.model.DeltaOwner
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemesController
import dev.ragnarok.fenrir.util.AppTextUtils.getDateFromUnixTime
import dev.ragnarok.fenrir.util.DownloadWorkUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.serializeble.json.Json
import dev.ragnarok.fenrir.util.serializeble.json.decodeFromStream
import dev.ragnarok.fenrir.util.toast.CustomToast
import io.reactivex.rxjava3.disposables.Disposable
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat

class DeltaOwnerActivity : AppCompatActivity(), PlaceProvider, AppStyleable {
    private var mToolbar: Toolbar? = null
    private var disposable: Disposable = Disposable.disposed()
    private val DOWNLOAD_DATE_FORMAT: DateFormat =
        SimpleDateFormat("yyyyMMdd_HHmmss", Utils.appLocale)

    @Suppress("DEPRECATION")
    public override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemesController.currentStyle())
        Utils.prepareDensity(this)
        super.onCreate(savedInstanceState)
        attach(
            this,
            SlidrConfig.Builder().fromUnColoredToColoredStatusBar(true)
                .position(SlidrPosition.LEFT).scrimColor(CurrentTheme.getColorBackground(this))
                .build()
        )
        setContentView(R.layout.activity_delta_owner)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = null
        supportActionBar?.subtitle = null

        val Export: FloatingActionButton = findViewById(R.id.delta_export)

        val action = intent.action
        val delta: DeltaOwner = if (Intent.ACTION_VIEW == action) {
            try {
                Export.visibility = View.GONE
                intent.data?.let { uri ->
                    contentResolver.openInputStream(
                        uri
                    )?.let {
                        val s = kJson.decodeFromStream(DeltaOwner.serializer(), it)
                        it.close()
                        s
                    }
                } ?: DeltaOwner()
            } catch (e: Exception) {
                e.printStackTrace()
                CustomToast.createCustomToast(this).showToastError(e.localizedMessage)
                DeltaOwner()
            }
        } else {
            Export.visibility = View.VISIBLE
            intent.extras?.getParcelableCompat(Extra.LIST) ?: DeltaOwner()
        }

        val accountId = intent.extras?.getInt(Extra.ACCOUNT_ID, Settings.get().accounts().current)
            ?: Settings.get().accounts().current

        val Title: TextView = findViewById(R.id.delta_title)
        val Time: TextView = findViewById(R.id.delta_time)
        val Avatar: ImageView = findViewById(R.id.toolbar_avatar)
        val EmptyAvatar: TextView = findViewById(R.id.empty_avatar_text)

        Time.text = getDateFromUnixTime(this, delta.time)

        disposable = OwnerInfo.getRx(this, accountId, delta.ownerId)
            .fromIOToMain()
            .subscribe({ owner ->
                Export.setOnClickListener {
                    DownloadWorkUtils.CheckDirectory(Settings.get().other().docDir)
                    val file = File(
                        Settings.get().other().docDir, DownloadWorkUtils.makeLegalFilename(
                            "OwnerChanges_" + owner.owner.fullName.orEmpty() + "_" + DOWNLOAD_DATE_FORMAT.format(
                                delta.time * 1000L
                            ), "json"
                        )
                    )
                    var out: FileOutputStream? = null
                    try {
                        val bytes = Json { prettyPrint = true }.encodeToString(
                            DeltaOwner.serializer(),
                            delta
                        ).toByteArray(
                            Charsets.UTF_8
                        )
                        out = FileOutputStream(file)
                        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                        out.write(bom)
                        out.write(bytes)
                        out.flush()
                        Includes.provideApplicationContext().sendBroadcast(
                            Intent(
                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.fromFile(file)
                            )
                        )
                        CustomToast.createCustomToast(this).showToast(
                            R.string.saved_to_param_file_name,
                            file.absolutePath
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        CustomToast.createCustomToast(this).showToastError(e.localizedMessage)
                    } finally {
                        Utils.safelyClose(out)
                    }
                }

                Avatar.setOnClickListener {
                    PlaceFactory.getOwnerWallPlace(accountId, owner.owner).tryOpenWith(this)
                }
                if (owner.owner.maxSquareAvatar.nonNullNoEmpty()) {
                    EmptyAvatar.visibility = View.GONE
                    Avatar.let {
                        PicassoInstance.with()
                            .load(owner.owner.maxSquareAvatar)
                            .transform(RoundTransformation())
                            .into(it)
                    }
                } else {
                    Avatar.let { PicassoInstance.with().cancelRequest(it) }
                    if (owner.owner.fullName.nonNullNoEmpty()) {
                        EmptyAvatar.visibility = View.VISIBLE
                        var name: String = owner.owner.fullName.orEmpty()
                        if (name.length > 2) name = name.substring(0, 2)
                        name = name.trim { it <= ' ' }
                        EmptyAvatar.text = name
                    } else {
                        EmptyAvatar.visibility = View.GONE
                    }
                    Avatar.setImageBitmap(
                        RoundTransformation().localTransform(
                            Utils.createGradientChatImage(
                                200,
                                200,
                                owner.owner.ownerId.orZero()
                            )
                        )
                    )
                }
                Title.text = owner.owner.fullName
            }, { CustomToast.createCustomToast(this).showToastThrowable(it) }
            )

        val viewPager: ViewPager2 = findViewById(R.id.delta_pager)
        viewPager.offscreenPageLimit = 1
        viewPager.setPageTransformer(
            Utils.createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        val adapter = Adapter(delta, accountId)
        viewPager.adapter = adapter
        TabLayoutMediator(
            findViewById(R.id.delta_tabs),
            viewPager
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = adapter.DeltaOwner.content[position].name
        }.attach()

        val w = window
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        w.statusBarColor = CurrentTheme.getStatusBarColor(this)
        w.navigationBarColor = CurrentTheme.getNavigationBarColor(this)
    }

    private class RecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivRecycler: RecyclerView = view.findViewById(R.id.alert_recycle)
    }

    private inner class Adapter(val DeltaOwner: DeltaOwner, private val accountId: Int) :
        RecyclerView.Adapter<RecyclerViewHolder>() {
        @SuppressLint("ClickableViewAccessibility")
        override fun onCreateViewHolder(container: ViewGroup, viewType: Int): RecyclerViewHolder {
            return RecyclerViewHolder(
                LayoutInflater.from(container.context)
                    .inflate(R.layout.recycle_frame, container, false)
            )
        }

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
            val list = DeltaOwner.content[position]
            val adapter = OwnersAdapter(this@DeltaOwnerActivity, list.ownerList)
            adapter.setClickListener(object : OwnersAdapter.ClickListener {
                override fun onOwnerClick(owner: Owner) {
                    PlaceFactory.getOwnerWallPlace(accountId, owner)
                        .tryOpenWith(this@DeltaOwnerActivity)
                }
            })
            holder.ivRecycler.layoutManager =
                LinearLayoutManager(this@DeltaOwnerActivity, RecyclerView.VERTICAL, false)
            holder.ivRecycler.adapter = adapter
        }

        override fun getItemCount(): Int {
            return DeltaOwner.content.size
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.updateActivityContext(newBase))
    }

    override fun openPlace(place: Place) {
        Utils.openPlaceWithSwipebleActivity(this, place)
    }

    override fun hideMenu(hide: Boolean) {}
    override fun openMenu(open: Boolean) {}

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)
        mToolbar = toolbar
        resolveToolbarNavigationIcon()
    }

    private fun resolveToolbarNavigationIcon() {
        mToolbar?.setNavigationIcon(R.drawable.close)
        mToolbar?.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    @Suppress("DEPRECATION")
    override fun setStatusbarColored(colored: Boolean, invertIcons: Boolean) {
        val statusbarNonColored = CurrentTheme.getStatusBarNonColored(this)
        val statusbarColored = CurrentTheme.getStatusBarColor(this)
        val w = window
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        w.statusBarColor = if (colored) statusbarColored else statusbarNonColored
        @ColorInt val navigationColor =
            if (colored) CurrentTheme.getNavigationBarColor(this) else Color.BLACK
        w.navigationBarColor = navigationColor
        if (Utils.hasMarshmallow()) {
            var flags = window.decorView.systemUiVisibility
            flags = if (invertIcons) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            window.decorView.systemUiVisibility = flags
        }
        if (Utils.hasOreo()) {
            var flags = window.decorView.systemUiVisibility
            if (invertIcons) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                w.decorView.systemUiVisibility = flags
                w.navigationBarColor = Color.WHITE
            } else {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                w.decorView.systemUiVisibility = flags
            }
        }
    }

    companion object {
        fun showDeltaActivity(context: Context, accountId: Int, delta: DeltaOwner) {
            if (delta.content.isEmpty()) {
                return
            }
            val intent = Intent(context, DeltaOwnerActivity::class.java)
            intent.putExtra(Extra.LIST, delta)
            intent.putExtra(Extra.ACCOUNT_ID, accountId)
            context.startActivity(intent)
        }
    }
}
