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
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayoutMediator
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.slidr.Slidr.attach
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.activity.slidr.model.SlidrPosition
import dev.ragnarok.fenrir.fragment.absownerslist.OwnersAdapter
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.kJsonPretty
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.model.DeltaOwner
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.CurrentTheme.getNavigationBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarNonColored
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemesController
import dev.ragnarok.fenrir.util.AppTextUtils.getDateFromUnixTime
import dev.ragnarok.fenrir.util.DownloadWorkUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.hasVanillaIceCreamTarget
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.toast.CustomToast
import kotlinx.serialization.json.decodeFromBufferedSource
import okio.buffer
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat

class DeltaOwnerActivity : AppCompatActivity(), PlaceProvider, AppStyleable {
    private var mToolbar: Toolbar? = null
    private var disposable = CancelableJob()
    private val DOWNLOAD_DATE_FORMAT: DateFormat =
        SimpleDateFormat("yyyyMMdd_HHmmss", Utils.appLocale)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemesController.currentStyle())
        Utils.prepareDensity(this)
        Utils.registerColorsThorVG(this)
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
                        val s = kJson.decodeFromBufferedSource(
                            DeltaOwner.serializer(),
                            it.source().buffer()
                        )
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

        val accountId = intent.extras?.getLong(Extra.ACCOUNT_ID, Settings.get().accounts().current)
            ?: Settings.get().accounts().current

        val Title: TextView = findViewById(R.id.delta_title)
        val Time: TextView = findViewById(R.id.delta_time)
        val Avatar: ImageView = findViewById(R.id.toolbar_avatar)
        val EmptyAvatar: TextView = findViewById(R.id.empty_avatar_text)

        Time.text = getDateFromUnixTime(this, delta.time)

        disposable += OwnerInfo.getRx(this, accountId, delta.ownerId)
            .fromIOToMain({ owner ->
                Export.setOnClickListener {
                    DownloadWorkUtils.CheckDirectory(Settings.get().main().docDir)
                    val file = File(
                        Settings.get().main().docDir, DownloadWorkUtils.makeLegalFilename(
                            "OwnerChanges_" + owner.owner.fullName.orEmpty() + "_" + DOWNLOAD_DATE_FORMAT.format(
                                delta.time * 1000L
                            ), "json"
                        )
                    )
                    var out: FileOutputStream? = null
                    try {
                        val bytes = kJsonPretty.encodeToString(
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
                            @Suppress("deprecation")
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
                        name = name.trim()
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
        ) { tab, position ->
            tab.text = adapter.DeltaOwner.content[position].name
        }.attach()

        @Suppress("deprecation")
        if (!hasVanillaIceCreamTarget()) {
            val w = window
            w.statusBarColor = getStatusBarColor(this)
            w.navigationBarColor = getNavigationBarColor(this)
        }
    }

    private class RecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivRecycler: RecyclerView = view.findViewById(R.id.alert_recycle)
    }

    private inner class Adapter(val DeltaOwner: DeltaOwner, private val accountId: Long) :
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
        disposable.cancel()
    }

    override fun setStatusbarColored(colored: Boolean, invertIcons: Boolean) {
        val w = window
        @Suppress("deprecation")
        if (!hasVanillaIceCreamTarget()) {
            w.statusBarColor =
                if (colored) getStatusBarColor(this) else getStatusBarNonColored(
                    this
                )
            w.navigationBarColor =
                if (colored) getNavigationBarColor(this) else Color.BLACK
        }
        val ins = WindowInsetsControllerCompat(w, w.decorView)
        ins.isAppearanceLightStatusBars = invertIcons
        ins.isAppearanceLightNavigationBars = invertIcons
    }

    companion object {
        fun showDeltaActivity(context: Context, accountId: Long, delta: DeltaOwner) {
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
