package dev.ragnarok.filegallery.activity

import android.content.Context
import android.os.Bundle
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.fragment.pin.enterpin.EnterPinFragment
import dev.ragnarok.filegallery.util.Utils

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