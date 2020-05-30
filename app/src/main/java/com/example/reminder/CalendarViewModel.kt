package com.example.reminder

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import com.example.reminder.MainActivity.Companion.REQUEST_GOOGLE_PLAY_SERVICES
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability


class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    //var mCredential: GoogleAccountCredential? = null

    fun isGooglePlayServicesAvailable(): Boolean {
        //showToast("Google 1")
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(getApplication())
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    fun acquireGooglePlayServices() {
        //showToast("Google 2")
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(getApplication())
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
        }
    }

    fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
        //showToast("Google 3")
        val apiAvailability = GoogleApiAvailability.getInstance()
        val dialog = apiAvailability.getErrorDialog(
            getApplication<Application>().applicationContext as Activity,
            connectionStatusCode,
            REQUEST_GOOGLE_PLAY_SERVICES
        )
        dialog.show()
        //Log.i("Error Dialog", "true")
    }

    fun isDeviceOnline(): Boolean {
        val connMgr = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}