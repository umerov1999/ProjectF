package dev.ragnarok.filegallery.activity.photopager

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
import androidx.core.net.toFile
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.*
import com.squareup.picasso3.BitmapTarget
import com.squareup.picasso3.Picasso
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.activity.qr.CameraScanActivity
import dev.ragnarok.filegallery.fragment.base.RxSupportPresenter
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.model.Video
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.picasso.PicassoInstance
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorPrimary
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorSecondary
import dev.ragnarok.filegallery.settings.Settings.get
import dev.ragnarok.filegallery.util.AssertUtils
import dev.ragnarok.filegallery.util.Utils
import java.io.File
import java.util.Calendar

open class PhotoPagerPresenter internal constructor(
    initialData: ArrayList<Photo>,
    savedInstanceState: Bundle?
) : RxSupportPresenter<IPhotoPagerView>(savedInstanceState) {
    protected var mPhotos: ArrayList<Photo> = initialData
    protected var currentIndex = 0
    private var mLoadingNow = false
    private var mFullScreen = false
    open fun close() {
        view?.closeOnly()
    }

    fun changeLoadingNowState(loading: Boolean) {
        mLoadingNow = loading
        resolveLoadingView()
    }

    private fun resolveLoadingView() {
        view?.displayPhotoListLoading(mLoadingNow)
    }

    fun refreshPagerView() {
        view?.displayPhotos(
            mPhotos,
            currentIndex
        )
    }

    override fun onGuiCreated(viewHost: IPhotoPagerView) {
        super.onGuiCreated(viewHost)
        view?.displayPhotos(
            mPhotos,
            currentIndex
        )
        refreshInfoViews()
        resolveToolbarVisibility()
        resolveButtonsBarVisible()
        resolveLoadingView()
    }

    fun firePageSelected(position: Int) {
        val old = currentIndex
        changePageTo(position)
        afterPageChangedFromUi(old, position)
    }

    protected open fun afterPageChangedFromUi(oldPage: Int, newPage: Int) {}
    private fun changePageTo(position: Int) {
        if (currentIndex == position) return
        currentIndex = position
        onPositionChanged()
    }

    fun count(): Int {
        return mPhotos.size
    }

    private fun resolveToolbarTitleSubtitleView() {
        if (!hasPhotos()) return
        view?.setToolbarTitle(currentIndex + 1, count())
        view?.setToolbarSubtitle(current.text)
    }

    private val current: Photo
        get() = mPhotos[currentIndex]

    private fun onPositionChanged() {
        refreshInfoViews()
        view?.let { resolveOptionMenu(it) }
    }

    fun refreshInfoViews() {
        resolveToolbarTitleSubtitleView()
    }

    fun fireSaveOnDriveClick(): Boolean {
        if (!get().main().isDownload_photo_tap()) {
            return true
        }
        if (current.isGif && current.photo_url != null && !current.photo_url!!.endsWith(
                "gif",
                true
            )
        ) {
            val v = Video()
            v.setId(current.id)
            v.setOwnerId(current.ownerId)
            v.setDate(current.date)
            v.setTitle(current.text)
            v.setLink(current.photo_url)
            view?.displayVideo(v)
            return false
        }
        if (current.inLocal()) {
            return true
        }
        val dir = File(get().main().getPhotoDir())
        if (!dir.isDirectory) {
            val created = dir.mkdirs()
            if (!created) {
                view?.showError("Can't create directory $dir")
                return false
            }
        } else dir.setLastModified(Calendar.getInstance().time.time)
        val photo = current
        var path = photo.text
        val ndx = path?.indexOf('/')
        if (ndx != -1) {
            path = ndx?.let { path?.substring(0, it) }
        }
        DownloadResult(path, dir, photo)
        return false
    }

    @Suppress("deprecation")
    private fun getCustomTabsPackages(context: Context): ArrayList<ResolveInfo> {
        val pm = context.packageManager
        val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))
        val resolvedActivityList = if (Utils.hasTiramisu()) pm.queryIntentActivities(
            activityIntent,
            PackageManager.ResolveInfoFlags.of(0)
        ) else pm.queryIntentActivities(activityIntent, 0)
        val packagesSupportingCustomTabs = ArrayList<ResolveInfo>()
        for (info in resolvedActivityList) {
            val serviceIntent = Intent()
            serviceIntent.action = ACTION_CUSTOM_TABS_CONNECTION
            serviceIntent.setPackage(info.activityInfo.packageName)
            if ((if (Utils.hasTiramisu()) pm.resolveService(
                    serviceIntent,
                    PackageManager.ResolveInfoFlags.of(0)
                ) else pm.resolveService(serviceIntent, 0)) != null
            ) {
                packagesSupportingCustomTabs.add(info)
            }
        }
        return packagesSupportingCustomTabs
    }

    internal fun openLinkInBrowser(context: Context, url: String?) {
        val intentBuilder = CustomTabsIntent.Builder()
        intentBuilder.setDefaultColorSchemeParams(
            CustomTabColorSchemeParams.Builder()
                .setToolbarColor(getColorPrimary(context)).setSecondaryToolbarColor(
                    getColorSecondary(
                        context
                    )
                ).build()
        )
        val customTabsIntent: CustomTabsIntent = intentBuilder.build()
        getCustomTabsPackages(context)
        if (getCustomTabsPackages(context).isNotEmpty()) {
            customTabsIntent.intent.setPackage(getCustomTabsPackages(context)[0].resolvePackageName)
        }
        try {
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun fireDetectQRClick(context: Activity) {
        PicassoInstance.with().load(current.photo_url)
            .into(object : BitmapTarget {
                override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                    val data = CameraScanActivity.decodeFromBitmap(bitmap)
                    MaterialAlertDialogBuilder(context)
                        .setIcon(R.drawable.qr_code)
                        .setMessage(data)
                        .setTitle(getString(R.string.qr))
                        .setPositiveButton(R.string.button_open) { _: DialogInterface?, _: Int ->
                            openLinkInBrowser(context, data)
                        }
                        .setNeutralButton(R.string.button_copy) { _: DialogInterface?, _: Int ->
                            val clipboard = context.getSystemService(
                                Context.CLIPBOARD_SERVICE
                            ) as ClipboardManager?
                            val clip = ClipData.newPlainText("response", data)
                            clipboard?.setPrimaryClip(clip)
                            view?.customToast?.showToast(R.string.copied_to_clipboard)
                        }
                        .setCancelable(true)
                        .show()
                }

                override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
                    view?.customToast?.showToastThrowable(e)
                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
            })
    }

    private fun DownloadResult(Prefix: String?, diru: File, photo: Photo) {
        var dir = diru
        if (Prefix != null && get().main().isPhoto_to_user_dir()) {
            val dir_final = File(dir.absolutePath + "/" + Prefix)
            if (!dir_final.isDirectory) {
                val created = dir_final.mkdirs()
                if (!created) {
                    view?.showError("Can't create directory $dir_final")
                    return
                }
            } else dir_final.setLastModified(Calendar.getInstance().time.time)
            dir = dir_final
        }
        photo.photo_url?.let {
            view?.downloadPhoto(
                it,
                dir.absolutePath,
                (if (Prefix != null) Prefix + "_" else "") + photo.ownerId + "_" + photo.id
            )
        }
    }

    private fun hasPhotos(): Boolean {
        return mPhotos.nonNullNoEmpty()
    }

    fun firePhotoTap() {
        if (!hasPhotos()) return
        mFullScreen = !mFullScreen
        resolveToolbarVisibility()
        resolveButtonsBarVisible()
    }

    fun resolveButtonsBarVisible() {
        view?.setButtonsBarVisible(hasPhotos() && !mFullScreen)
    }

    fun resolveToolbarVisibility() {
        view?.setToolbarVisible(hasPhotos() && !mFullScreen)
    }

    override fun onViewHostAttached(view: IPhotoPagerView) {
        super.onViewHostAttached(view)
        resolveOptionMenu(view)
    }

    private fun isLocal(): Boolean {
        return hasPhotos() && current.inLocal()
    }

    private fun resolveOptionMenu(view: IPhotoPagerView) {
        view.setupOptionMenu(
            isLocal()
        )
    }

    val currentFile: String
        get() = Uri.parse(mPhotos[currentIndex].photo_url).toFile().absolutePath

    init {
        AssertUtils.requireNonNull(mPhotos, "'mPhotos' not initialized")
    }
}