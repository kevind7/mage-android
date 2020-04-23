package mil.nga.giat.mage.login

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_account_created.*
import mil.nga.giat.mage.R

class AccountCreatedActivity: Activity() {

    override fun onCreate(savedInstanceBundle: Bundle?) {
        super.onCreate(savedInstanceBundle)

        setContentView(R.layout.activity_account_created)

        val appName = findViewById<TextView>(R.id.mage)
        appName.typeface = Typeface.createFromAsset(assets, "fonts/GondolaMage-Regular.otf")

        ok.setOnClickListener { finish() }
    }
}