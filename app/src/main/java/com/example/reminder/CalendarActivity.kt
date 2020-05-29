package com.example.reminder

import android.accounts.AccountManager
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import butterknife.BindView

import butterknife.ButterKnife

import butterknife.OnClick


class CalendarActivity : AppCompatActivity() {
    /**
     * A Google Calendar API service object used to access the API.
     * Note: Do not confuse this class with API library's model classes, which
     * represent specific data structures.
     */

    companion object{
        val REQUEST_ACCOUNT_PICKER = 1000
        val REQUEST_AUTHORIZATION = 1001
        val REQUEST_GOOGLE_PLAY_SERVICES = 1002
    }
    var mService: Calendar? = null
    var mProgress: ProgressDialog? = null
    var credential: GoogleAccountCredential? = null
    var mStatusText: TextView? = null
    var mResultsText: TextView? = null
    val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
    val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()


    val PREF_ACCOUNT_NAME = "accountName"
    val SCOPES = arrayOf(
        CalendarScopes.CALENDAR_READONLY, CalendarScopes.CALENDAR
    )


    @BindView(R.id.btnEvent)
    lateinit var btnEvent: Button


    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityLayout = LinearLayout(this)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        activityLayout.layoutParams = lp
        activityLayout.orientation = LinearLayout.VERTICAL
        activityLayout.setPadding(16, 16, 16, 16)
        val tlp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        btnEvent = Button(this)
        btnEvent.id = R.id.btnEvent
        btnEvent.text = "Create event"
        activityLayout.addView(btnEvent)

        mStatusText = TextView(this)
        mStatusText!!.layoutParams = tlp
        mStatusText!!.setTypeface(null, Typeface.BOLD)
        mStatusText!!.text = "Retrieving data..."
        activityLayout.addView(mStatusText)
        mResultsText = TextView(this)
        mResultsText!!.layoutParams = tlp
        mResultsText!!.setPadding(16, 16, 16, 16)
        mResultsText!!.isVerticalScrollBarEnabled = true
        mResultsText!!.movementMethod = ScrollingMovementMethod()
        activityLayout.addView(mResultsText)

        mProgress = ProgressDialog(this)
        mProgress!!.setMessage("Calling Google Calendar API ...")
        setContentView(activityLayout)
        ButterKnife.bind(this)

        // Initialize credentials and service object.
        val settings: SharedPreferences = getPreferences(Context.MODE_PRIVATE)
        credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, listOf(*SCOPES)
        )
            .setBackOff(ExponentialBackOff())
            .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null))
        mService = Calendar.Builder(
            transport, jsonFactory, credential
        )
            .setApplicationName("Google Calendar API Android Quickstart")
            .build()
    }

    @OnClick(R.id.btnEvent)
    fun addCalendarEvent() {
        CreateEventTask(mService).execute()
    }

    /**
     * Called whenever this activity is pushed to the foreground, such as after
     * a call to onCreate().
     */
    override fun onResume() {
        super.onResume()
        if (isGooglePlayServicesAvailable()) {
            refreshResults()
        } else {
            mStatusText!!.text = "Google Play Services required: " +
                    "after installing, close and relaunch this app."
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     * activity result.
     * @param data Intent (containing result data) returned by incoming
     * activity result.
     */
    override fun onActivityResult(
        requestCode: Int, resultCode: Int, data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode == Activity.RESULT_OK) {
                refreshResults()
            } else {
                isGooglePlayServicesAvailable()
            }
            REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data != null && data.extras != null
            ) {
                val accountName =
                    data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                if (accountName != null) {

                    credential?.setSelectedAccountName(accountName)
                    val settings: SharedPreferences =
                        getPreferences(Context.MODE_PRIVATE)
                    val editor = settings.edit()
                    editor.putString(PREF_ACCOUNT_NAME, accountName)
                    editor.commit()
                    refreshResults()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                mStatusText!!.text = "Account unspecified."
            }
            REQUEST_AUTHORIZATION -> if (resultCode == Activity.RESULT_OK) {
                refreshResults()
            } else {
                chooseAccount()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Attempt to get a set of data from the Google Calendar API to display. If the
     * email address isn't known yet, then call chooseAccount() method so the
     * user can pick an account.
     */
    open fun refreshResults() {
        if (credential?.getSelectedAccountName() == null) {
            chooseAccount()
        } else {
            if (isDeviceOnline()) {
                ApiAsyncTask(this).execute()
            } else {
                mStatusText!!.text = "No network connection available."
            }
        }
    }

    /**
     * Clear any existing Google Calendar API data from the TextView and update
     * the header message; called from background threads and async tasks
     * that need to update the UI (in the UI thread).
     */
    fun clearResultsText() {
        runOnUiThread(Runnable {
            mStatusText!!.text = "Retrieving data…"
            mResultsText!!.text = ""
        })
    }

    /**
     * Fill the data TextView with the given List of Strings; called from
     * background threads and async tasks that need to update the UI (in the
     * UI thread).
     * @param dataStrings a List of Strings to populate the main TextView with.
     */
    fun updateResultsText(dataStrings: List<String?>?) {
        runOnUiThread(Runnable {
            if (dataStrings == null) {
                mStatusText!!.text = "Error retrieving data!"
            } else if (dataStrings.size == 0) {
                mStatusText!!.text = "No data found."
            } else {
                mStatusText!!.text = "Data retrieved using" +
                        " the Google Calendar API:"
                mResultsText!!.text = TextUtils.join("\n\n", dataStrings)
            }
        })
    }

    /**
     * Show a status message in the list header TextView; called from background
     * threads and async tasks that need to update the UI (in the UI thread).
     * @param message a String to display in the UI header TextView.
     */
    fun updateStatus(message: String?) {
        runOnUiThread(Runnable { mStatusText!!.text = message })
    }

    /**
     * Starts an activity in Google Play Services so the user can pick an
     * account.
     */
    open fun chooseAccount() {
        startActivityForResult(
            credential?.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER
        )
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    open fun isDeviceOnline(): Boolean {
        val connMgr =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    /**
     * Check that Google Play services APK is installed and up to date. Will
     * launch an error dialog for the user to update Google Play Services if
     * possible.
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    open fun isGooglePlayServicesAvailable(): Boolean {
        val connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
            return false
        } else if (connectionStatusCode != ConnectionResult.SUCCESS) {
            return false
        }
        return true
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     * Google Play Services on this device.
     */
    fun showGooglePlayServicesAvailabilityErrorDialog(
        connectionStatusCode: Int
    ) {
        runOnUiThread(Runnable {
            val dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                this@CalendarActivity,
                REQUEST_GOOGLE_PLAY_SERVICES
            )
            dialog.show()
        })
    }

}