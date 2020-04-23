package mil.nga.giat.mage.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import mil.nga.giat.mage.R
import mil.nga.giat.mage.disclaimer.DisclaimerActivity
import mil.nga.giat.mage.event.EventsActivity
import mil.nga.giat.mage.sdk.login.AccountDelegate
import mil.nga.giat.mage.sdk.login.AccountStatus
import mil.nga.giat.mage.sdk.login.IdpLoginTask
import mil.nga.giat.mage.sdk.utils.DeviceUuidFactory

/**
 * Created by wnewman on 10/14/15.
 */
class IdpSigninActivity : FragmentActivity(), AccountDelegate {

    companion object {
        private val LOG_NAME = IdpSigninActivity::class.java.name

        private const val KEY_AUTHORIZATION_STARTED = "AUTHORIZATION_STARTED"
        private const val EXTRA_SERVER_URL = "EXTRA_SERVER_URL"
        private const val EXTRA_IDP_STRATEGY = "EXTRA_IDP_STRATEGY"

        fun intent(context: Context, url: String, strategy: String): Intent {
            val intent = Intent(context, IdpSigninActivity::class.java)
            intent.putExtra(EXTRA_SERVER_URL, url)
            intent.putExtra(EXTRA_IDP_STRATEGY, strategy)
            return intent
        }
    }

    private var authorizationStarted = false
    private var serverURL: String? = null
    private var idpURL: String? = null
    private lateinit var  idpStrategy: String
    private lateinit var uuid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_idp_signin)

        require(intent.hasExtra(EXTRA_SERVER_URL)) {"EXTRA_SERVER_URL is required to launch IdpSigninActivity"}
        require(intent.hasExtra(EXTRA_IDP_STRATEGY)) {"EXTRA_IDP_STRATEGY is required to launch IdpSigninActivity"}

        serverURL = intent.getStringExtra(EXTRA_SERVER_URL)
        idpStrategy = intent.getStringExtra(EXTRA_IDP_STRATEGY)
        idpURL = String.format("%s/auth/%s/signin?state=mobile", serverURL, idpStrategy)
        uuid = DeviceUuidFactory(this).deviceUuid.toString()

        if (savedInstanceState == null) {
            extractState(intent.extras)
        } else {
            extractState(savedInstanceState)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        if (!authorizationStarted) {
            val customTabsIntent = CustomTabsIntent.Builder()
                    .setCloseButtonIcon(backButtonIcon())
                    .setToolbarColor(primaryColor())
                    .build()

            customTabsIntent.launchUrl(this, Uri.parse(idpURL))

            authorizationStarted = true

            return
        }

        if (intent.data != null) {
            handleAuthenticationComplete()
        } else {
            handlAuthenticationCanceled()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AUTHORIZATION_STARTED, authorizationStarted)
    }

    private fun extractState(state: Bundle?) {
        if (state == null) {
            finish()
            return
        }

        authorizationStarted = state.getBoolean(KEY_AUTHORIZATION_STARTED, false)
    }

    private fun handleAuthenticationComplete() {
        when (intent.data?.path) {
            "/account_created" -> {
                val intent = Intent(applicationContext, AccountCreatedActivity::class.java)
                startActivity(intent)
                finish()
            }
            "/authentication" -> {
                val jwt = intent.data?.getQueryParameter("token") ?: ""
                val loginTask = IdpLoginTask(this, applicationContext)
                loginTask.execute(idpStrategy, uuid, jwt)
            }
            else -> {
                handlAuthenticationCanceled()
            }
        }
    }

    private fun handlAuthenticationCanceled() {
        finish()
    }

    override fun finishAccount(accountStatus: AccountStatus) {
        when (accountStatus.status) {
            AccountStatus.Status.SUCCESSFUL_LOGIN -> {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
                val showDisclaimer = sharedPreferences.getBoolean(getString(R.string.serverDisclaimerShow), false)
                val intent = if (showDisclaimer) Intent(applicationContext, DisclaimerActivity::class.java) else Intent(applicationContext, EventsActivity::class.java)
                startActivity(intent)
                finish()
            }
            AccountStatus.Status.FAILED_AUTHORIZATION -> {
                val intent = Intent()
                intent.putExtra(LoginActivity.EXTRA_IDP_UNREGISTERED_DEVICE, true)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            else -> {
                val intent = Intent()
                intent.putExtra(LoginActivity.EXTRA_IDP_ERROR, true)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun primaryColor(): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute (R.attr.colorPrimary, typedValue, true)
        return typedValue.data
    }

    private fun backButtonIcon(): Bitmap {
        return AppCompatResources.getDrawable(applicationContext, R.drawable.ic_arrow_back_white_24dp)!!.toBitmap()
    }
}