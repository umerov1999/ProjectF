package dev.ragnarok.fenrir.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.ragnarok.fenrir.fragment.pin.enterpin.EnterPinFragment
import dev.ragnarok.fenrir.util.Utils

open class EnterPinActivity : NoMainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(noMainContainerViewId, EnterPinFragment.newInstance())
                .commit()
        }
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(
                context,
                if (Utils.is600dp(context)) EnterPinActivity::class.java else EnterPinActivityPortraitOnly::class.java
            )
        }
    }
}