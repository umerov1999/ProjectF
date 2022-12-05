package dev.ragnarok.fenrir.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.button.MaterialButton
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.module.BufferWriteNative
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.rlottie.RLottie2Gif
import dev.ragnarok.fenrir.module.rlottie.RLottie2Gif.Lottie2GifListener
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemesController.currentStyle
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import java.io.File

class LottieActivity : AppCompatActivity() {
    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        startExportGif()
    }
    private var toGif: MaterialButton? = null

    private val fManager = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val v = DocumentFile.fromSingleUri(
                this,
                intent.data ?: return@registerForActivityResult
            )
            val title: String = if (v == null || v.name.isNullOrEmpty()) {
                "converted.gif"
            } else {
                v.name + ".gif"
            }
            val file = File(
                result.data?.getStringExtra(Extra.PATH), title
            )
            toGif?.isEnabled = false
            RLottie2Gif.create(
                BufferWriteNative.fromStreamEndlessNull(
                    contentResolver.openInputStream(
                        intent.data ?: return@registerForActivityResult
                    ) ?: return@registerForActivityResult
                )
            ).setListener(object : Lottie2GifListener {
                var start: Long = 0
                var logs: String? = null
                override fun onStarted() {
                    start = System.currentTimeMillis()
                    logs = "Wait a moment...\r\n"
                    log(logs)
                }

                @SuppressLint("SetTextI18n")
                override fun onProgress(frame: Int, totalFrame: Int) {
                    log(logs + "progress : " + frame + "/" + totalFrame)
                }

                override fun onFinished() {
                    logs =
                        "GIF created (" + (System.currentTimeMillis() - start) + "ms)\r\n" +
                                "Resolution : " + 500 + "x" + 500 + "\r\n" +
                                "Path : " + file + "\r\n" +
                                "File Size : " + file.length() / 1024 + "kb"
                    log(logs)
                    toGif?.post { toGif?.isEnabled = true }
                }
            })
                .setBackgroundColor(Color.TRANSPARENT)
                .setOutputPath(file)
                .setSize(500, 500)
                .setBackgroundTask(true)
                .setDithering(false)
                .build()
        }
    }

    private var lottie: RLottieImageView? = null
    private var lg: TextView? = null
    internal fun log(log: String?) {
        lg?.post { lg?.text = log?.trim { it <= ' ' } }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.updateActivityContext(newBase))
    }

    @Suppress("DEPRECATION")
    private fun startExportGif() {
        fManager.launch(
            FileManagerSelectActivity.makeFileManager(
                this, Environment.getExternalStorageDirectory().absolutePath,
                "dirs"
            )
        )
    }

    @Suppress("DEPRECATION")
    public override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(currentStyle())
        Utils.prepareDensity(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lottie)
        lottie = findViewById(R.id.lottie_preview)
        lg = findViewById(R.id.log_tag)
        toGif = findViewById(R.id.lottie_to_gif)
        toGif?.setOnClickListener {
            if (!FenrirNative.isNativeLoaded) {
                return@setOnClickListener
            }
            if (!AppPerms.hasReadWriteStoragePermission(this)) {
                requestWritePermission.launch()
                return@setOnClickListener
            }
            startExportGif()
        }
        val w = window
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        w.statusBarColor = CurrentTheme.getStatusBarColor(this)
        w.navigationBarColor = CurrentTheme.getNavigationBarColor(this)
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            finish()
            return
        }
        val accountId = Settings.get().accounts().current
        if (accountId == ISettings.IAccountsSettings.INVALID_ID) {
            finish()
            return
        }
        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            try {
                lottie?.setAutoRepeat(true)
                lottie?.fromString(
                    BufferWriteNative.fromStreamEndlessNull(
                        contentResolver.openInputStream(
                            getIntent().data ?: return
                        ) ?: return
                    ), Utils.dp(500f), Utils.dp(500f)
                )
                lottie?.playAnimation()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}