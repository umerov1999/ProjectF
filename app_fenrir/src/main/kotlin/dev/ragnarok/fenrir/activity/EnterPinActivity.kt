package dev.ragnarok.fenrir.activity

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.pin.enterpin.EnterPinFragment
import dev.ragnarok.fenrir.util.Utils

open class EnterPinActivity : NoMainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment, EnterPinFragment.newInstance())
                .commit()
        }
    }

    companion object {

        fun getClass(context: Context): Class<*> {
            return if (Utils.is600dp(context)) EnterPinActivity::class.java else EnterPinActivityPortraitOnly::class.java
        }
    }
}