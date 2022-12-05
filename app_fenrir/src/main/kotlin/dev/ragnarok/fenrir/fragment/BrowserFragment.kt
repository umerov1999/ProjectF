package dev.ragnarok.fenrir.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants.USER_AGENT
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.domain.IUtilsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.BaseFragment
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.link.LinkHelper.openLinkInBrowser
import dev.ragnarok.fenrir.link.LinkHelper.openVKLink
import dev.ragnarok.fenrir.link.VkLinkParser.parse
import dev.ragnarok.fenrir.link.types.AwayLink
import dev.ragnarok.fenrir.link.types.DomainLink
import dev.ragnarok.fenrir.link.types.PageLink
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.Option
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.DownloadWorkUtils
import dev.ragnarok.fenrir.util.Logger.d
import dev.ragnarok.fenrir.util.toast.CustomToast
import java.io.File
import java.net.URL
import java.util.*

class BrowserFragment : BaseFragment(), MenuProvider, BackPressCallback,
    View.OnCreateContextMenuListener {
    private var mWebView: WebView? = null
    private var mAccountId = 0
    private var title: String? = null
    private var webState: Bundle? = null
    private var mUtilsInteractor: IUtilsInteractor = InteractorFactory.createUtilsInteractor()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAccountId = requireArguments().getInt(Extra.ACCOUNT_ID)
        savedInstanceState?.let { restoreFromInstanceState(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.copy_url) {
            val clipboard =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText(getString(R.string.link), mWebView?.url)
            clipboard?.setPrimaryClip(clip)
            CustomToast.createCustomToast(requireActivity()).showToast(R.string.copied)
            return true
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.browser_menu, menu)
    }

    @Suppress("SetJavaScriptEnabled", "RequiresFeature", "deprecation")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_browser, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mWebView = root.findViewById(R.id.webview)
        mWebView?.settings?.builtInZoomControls = true
        mWebView?.settings?.displayZoomControls = false
        mWebView?.webViewClient = VkLinkSupportWebClient()
        mWebView?.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String) {
                this@BrowserFragment.title = title
                refreshActionBar()
            }
        }
        if (Settings.get().main().isWebview_night_mode && WebViewFeature.isFeatureSupported(
                WebViewFeature.FORCE_DARK
            ) && Settings.get().ui().isDarkModeEnabled(requireActivity())
        ) {
            mWebView?.settings
                ?.let { WebSettingsCompat.setForceDark(it, WebSettingsCompat.FORCE_DARK_ON) }
        }
        if (mWebView != null) {
            registerForContextMenu(mWebView ?: return null)
        }
        mWebView?.settings?.userAgentString = USER_AGENT(AccountType.BY_TYPE)
        mWebView?.settings?.javaScriptEnabled = true // из-за этого не срабатывал метод
        // shouldOverrideUrlLoading в WebClient
        when {
            savedInstanceState != null -> {
                restoreFromInstanceState(savedInstanceState)
            }
            webState != null -> {
                mWebView?.restoreState(webState ?: return null)
                webState = null
            }
            else -> {
                loadAtFirst()
            }
        }
        return root
    }

    internal fun downloadResult(Prefix: String?, dirL: File, url: String, type: String) {
        var dir = dirL
        if (Prefix != null && Settings.get().other().isPhoto_to_user_dir) {
            val dir_final = File(dir.absolutePath + "/" + Prefix)
            if (!dir_final.isDirectory) {
                val created = dir_final.mkdirs()
                if (!created) {
                    CustomToast.createCustomToast(requireActivity())
                        .showToastError("Can't create directory $dir_final")
                    return
                }
            } else dir_final.setLastModified(Calendar.getInstance().time.time)
            dir = dir_final
        }
        DownloadWorkUtils.doDownloadPhoto(
            requireActivity(),
            url,
            dir.absolutePath,
            (if (Prefix != null) Prefix + "_" else "") + type
        )
    }

    override
    fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        if (v is WebView) {
            val result = v.hitTestResult
            val type = result.type

            if (type == HitTestResult.IMAGE_TYPE || type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                val imageUrl = result.extra ?: return

                val menus = ModalBottomSheetDialogFragment.Builder()
                val owner = arguments?.getString(Extra.OWNER)
                var typeRes = arguments?.getString(Extra.TYPE)
                if (owner != null && typeRes != null) {
                    menus.add(
                        OptionRequest(
                            R.id.button_ok,
                            getString(R.string.download),
                            R.drawable.save,
                            true
                        )
                    )
                }
                menus.add(
                    OptionRequest(
                        R.id.button_cancel,
                        getString(R.string.copy_simple),
                        R.drawable.content_copy,
                        true
                    )
                )
                menus.show(
                    requireActivity().supportFragmentManager,
                    "left_options",
                    object : ModalBottomSheetDialogFragment.Listener {
                        override fun onModalOptionSelected(option: Option) {
                            if (option.id == R.id.button_ok) {
                                val urlObj = URL(imageUrl)
                                val urlPath: String = urlObj.path
                                var fileName = urlPath.substring(urlPath.lastIndexOf('/') + 1)
                                if (fileName.lastIndexOf('.') != -1) {
                                    fileName = fileName.substring(0, fileName.lastIndexOf('.'))
                                }

                                typeRes ?: return
                                owner ?: return
                                typeRes += ("_$fileName")
                                val dir = File(Settings.get().other().photoDir)
                                if (!dir.isDirectory) {
                                    val created = dir.mkdirs()
                                    if (!created) {
                                        CustomToast.createCustomToast(requireActivity())
                                            .showToastError("Can't create directory $dir")
                                        return
                                    }
                                } else dir.setLastModified(Calendar.getInstance().time.time)
                                downloadResult(
                                    DownloadWorkUtils.makeLegalFilename(
                                        (DownloadWorkUtils.fixStart(owner) ?: typeRes),
                                        null
                                    ), dir, imageUrl, typeRes
                                )
                            } else if (option.id == R.id.button_cancel) {
                                val clipboard =
                                    requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                                val clip = ClipData.newPlainText("response", imageUrl)
                                clipboard?.setPrimaryClip(clip)
                                CustomToast.createCustomToast(context)
                                    .showToast(R.string.copied_to_clipboard)
                            }
                        }
                    })
            }
        }
    }

    private fun loadAtFirst() {
        val url = requireArguments().getString(Extra.URL)
        d(TAG, "url: $url")
        if (url != null) {
            mWebView?.loadUrl(url)
        }
    }

    internal fun refreshActionBar() {
        if (!isAdded) {
            return
        }
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.browser)
            actionBar.subtitle = title
        }
    }

    override fun onResume() {
        super.onResume()
        refreshActionBar()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onPause() {
        super.onPause()
        webState = Bundle()
        mWebView?.saveState(webState ?: return)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVE_TITLE, title)
        mWebView?.saveState(outState)
    }

    private fun restoreFromInstanceState(bundle: Bundle) {
        mWebView?.restoreState(bundle)
        title = bundle.getString(SAVE_TITLE)
        d(TAG, "restoreFromInstanceState, bundle: $bundle")
    }

    override fun onBackPressed(): Boolean {
        if (mWebView?.canGoBack() == true) {
            mWebView?.goBack()
            return false
        }
        return true
    }

    private inner class VkLinkSupportWebClient : WebViewClient() {
        override fun onLoadResource(view: WebView, url: String) {
            super.onLoadResource(view, url)
            d(TAG, "onLoadResource, url: $url")
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            if (url == "vk.com" || url == "m.vk.com" || url.contains("github.com") || url.contains("vk.com/@") || url.contains(
                    "vk.com/activation"
                ) || url.contains("vk.com/login") || url.contains("login.vk.com") || url.contains(
                    "vk.com/app"
                )
            ) {
                view.loadUrl(url)
                return true
            }
            val link = parse(url)
            d(TAG, "shouldOverrideUrlLoading, link: $link, url: $url")

            //link: null, url: https://vk.com/doc124456557_415878705
            if (link == null) {
                activity?.let { openLinkInBrowser(it, url) }
                return true
            }
            if (link is PageLink) {
                view.loadUrl("$url?api_view=0df43cdc43a25550c6beb7357c9d41")
                return true
            }
            if (link is DomainLink) {
                appendDisposable(
                    mUtilsInteractor.resolveDomain(mAccountId, link.domain)
                        .fromIOToMain()
                        .subscribe({ optionalOwner ->
                            if (optionalOwner.isEmpty) {
                                view.loadUrl(url)
                            } else {
                                optionalOwner.get()?.let {
                                    PlaceFactory.getOwnerWallPlace(
                                        mAccountId,
                                        it
                                    ).tryOpenWith(requireActivity())
                                }
                            }
                        }) {
                            view.loadUrl(url)
                        })
                return true
            }
            if (link is AwayLink) {
                activity?.let { openLinkInBrowser(it, link.link) }
                return true
            }
            activity?.let {
                if (openVKLink(it, mAccountId, link, false)) {
                    return true
                }
            }
            view.loadUrl(url)
            return true
        }

        override fun onPageFinished(view: WebView, url: String) {
            title = view.title
            refreshActionBar()
        }
    }

    companion object {
        val TAG: String = BrowserFragment::class.java.simpleName
        private const val SAVE_TITLE = "save_title"
        fun buildArgs(accountId: Int, url: String, owner: String?, type: String?): Bundle {
            val args = Bundle()
            args.putString(Extra.URL, url)
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putString(Extra.OWNER, owner)
            args.putString(Extra.TYPE, type)
            return args
        }

        fun newInstance(args: Bundle?): BrowserFragment {
            val fragment = BrowserFragment()
            fragment.arguments = args
            return fragment
        }
    }
}