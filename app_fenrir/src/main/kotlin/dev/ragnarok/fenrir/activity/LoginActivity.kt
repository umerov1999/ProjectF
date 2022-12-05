package dev.ragnarok.fenrir.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.Constants.USER_AGENT
import dev.ragnarok.fenrir.api.Auth
import dev.ragnarok.fenrir.api.util.VKStringUtils
import dev.ragnarok.fenrir.model.Token
import dev.ragnarok.fenrir.settings.theme.ThemesController.currentStyle
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import java.io.UnsupportedEncodingException
import java.util.regex.Pattern

class LoginActivity : AppCompatActivity() {
    private var TLogin: String? = null
    private var TPassword: String? = null
    private var TwoFA: String? = null
    private var isSave = false

    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(currentStyle())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val webview = findViewById<WebView>(R.id.vkontakteview)
        webview.settings.javaScriptEnabled = true
        webview.clearCache(true)
        webview.settings.userAgentString = USER_AGENT(Constants.DEFAULT_ACCOUNT_TYPE)

        //Чтобы получать уведомления об окончании загрузки страницы
        webview.webViewClient = VkontakteWebViewClient()
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies { aBoolean: Boolean ->
            Log.d(
                TAG,
                "Cookie removed: $aBoolean"
            )
        }
        if (intent.getStringExtra(EXTRA_VALIDATE) == null) {
            val clientId = intent.getStringExtra(EXTRA_CLIENT_ID) ?: return
            val scope = intent.getStringExtra(EXTRA_SCOPE) ?: return
            val groupIds = intent.getStringExtra(EXTRA_GROUP_IDS) ?: return
            try {
                if (groupIds.nonNullNoEmpty()) {
                    webview.settings.userAgentString = Constants.KATE_USER_AGENT
                }
                val url = Auth.getUrl(clientId, scope, groupIds)
                webview.loadUrl(url)
            } catch (e: UnsupportedEncodingException) {
                createCustomToast(this).showToastError(e.localizedMessage)
            }
        } else {
            TLogin = intent.getStringExtra(EXTRA_LOGIN)
            TPassword = intent.getStringExtra(EXTRA_PASSWORD)
            TwoFA = intent.getStringExtra(EXTRA_TWO_FA)
            isSave = intent.getBooleanExtra(EXTRA_SAVE, false)
            webview.loadUrl(intent.getStringExtra(EXTRA_VALIDATE) ?: return)
        }
    }

    internal fun parseUrl(url: String?) {
        try {
            if (url == null) {
                return
            }
            Logger.d(TAG, "url=$url")
            if (url.startsWith(Auth.redirect_url)) {
                if (!url.contains("error=")) {
                    val intent = Intent()
                    try {
                        val tokens = tryExtractAccessTokens(url)
                        intent.putParcelableArrayListExtra("group_tokens", tokens)
                    } catch (e: Exception) {
                        val accessToken = tryExtractAccessToken(url)
                        val userId = tryExtractUserId(url)
                        intent.putExtra(Extra.TOKEN, accessToken)
                        intent.putExtra(Extra.USER_ID, userId?.toInt())
                        intent.putExtra(Extra.LOGIN, TLogin)
                        intent.putExtra(Extra.PASSWORD, TPassword)
                        intent.putExtra(Extra.TWO_FA, TwoFA)
                        intent.putExtra(Extra.SAVE, isSave)
                    }
                    setResult(RESULT_OK, intent)
                }
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inner class VkontakteWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            parseUrl(url)
        }
    }

    companion object {
        private val TAG = LoginActivity::class.java.simpleName
        private const val EXTRA_CLIENT_ID = "client_id"
        private const val EXTRA_SCOPE = "scope"
        private const val EXTRA_VALIDATE = "validate"
        private const val EXTRA_LOGIN = "login"
        private const val EXTRA_PASSWORD = "password"
        private const val EXTRA_TWO_FA = "two_fa"
        private const val EXTRA_SAVE = "save"
        private const val EXTRA_GROUP_IDS = "group_ids"


        fun createIntent(context: Context?, clientId: String?, scope: String?): Intent {
            return Intent(context, LoginActivity::class.java)
                .putExtra(EXTRA_CLIENT_ID, clientId)
                .putExtra(EXTRA_SCOPE, scope)
        }


        fun createIntent(
            context: Context?,
            validate_url: String?,
            Login: String?,
            Password: String?,
            TwoFa: String?,
            isSave: Boolean
        ): Intent {
            return Intent(context, LoginActivity::class.java)
                .putExtra(EXTRA_VALIDATE, validate_url).putExtra(EXTRA_LOGIN, Login)
                .putExtra(EXTRA_PASSWORD, Password).putExtra(EXTRA_TWO_FA, TwoFa)
                .putExtra(EXTRA_SAVE, isSave)
        }


        fun createIntent(
            context: Context?,
            clientId: String?,
            scope: String?,
            groupIds: Collection<Int>?
        ): Intent {
            val ids = Utils.join(groupIds, ",", object : Utils.SimpleFunction<Int, String> {
                override fun apply(orig: Int): String {
                    return orig.toString()
                }
            })
            return Intent(context, LoginActivity::class.java)
                .putExtra(EXTRA_CLIENT_ID, clientId)
                .putExtra(EXTRA_SCOPE, scope)
                .putExtra(EXTRA_GROUP_IDS, ids)
        }

        internal fun tryExtractAccessToken(url: String): String? {
            return VKStringUtils.extractPattern(url, "access_token=(.*?)&")
        }

        @Throws(Exception::class)
        internal fun tryExtractAccessTokens(url: String): ArrayList<Token> {
            val p = Pattern.compile("access_token_(\\d*)=(.*?)(&|$)")
            val tokens = ArrayList<Token>()
            val matcher = p.matcher(url)
            while (matcher.find()) {
                val groupid = matcher.group(1)
                val token = matcher.group(2)
                if (groupid.nonNullNoEmpty() && token.nonNullNoEmpty()) {
                    tokens.add(Token(-groupid.toInt(), token))
                }
            }
            if (tokens.isEmpty()) {
                throw Exception("Failed to parse redirect url $url")
            }
            return tokens
        }

        internal fun tryExtractUserId(url: String): String? {
            return VKStringUtils.extractPattern(url, "user_id=(\\d*)")
        }


        fun extractGroupTokens(data: Intent): ArrayList<Token>? {
            return data.getParcelableArrayListExtraCompat("group_tokens")
        }
    }
}