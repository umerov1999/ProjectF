package dev.ragnarok.filegallery.activity

import android.os.Bundle
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.fragment.pin.createpin.CreatePinFragment

class CreatePinActivity : NoMainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment, CreatePinFragment.newInstance())
                .commit()
        }
    }
}