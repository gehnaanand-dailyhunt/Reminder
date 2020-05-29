package com.example.reminder

import android.os.AsyncTask
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Events
import java.io.IOException
import java.util.*

class ApiAsyncTask(calendarActivity: CalendarActivity) : AsyncTask<Void, Void, Void>() {
    private var mActivity = calendarActivity

    /**
     * Background task to call Google Calendar API.
     * @param params no parameters needed for this task.
     */
    override fun doInBackground(vararg params: Void?): Void? {
        try {
            mActivity.clearResultsText()
            mActivity.updateResultsText(getDataFromApi())
        } catch (availabilityException: GooglePlayServicesAvailabilityIOException) {
            mActivity.showGooglePlayServicesAvailabilityErrorDialog(
                availabilityException.connectionStatusCode
            )
        } catch (userRecoverableException: UserRecoverableAuthIOException) {
            mActivity.startActivityForResult(
                userRecoverableException.intent,
                CalendarActivity.REQUEST_AUTHORIZATION
            )
        } catch (e: Exception) {
            mActivity.updateStatus(
                "The following error occurred:${e.message}".trimIndent()
            )
        }
        if (mActivity.mProgress?.isShowing!!) {
            mActivity.mProgress?.dismiss()
        }
        return null
    }

    /**
     * Fetch a list of the next 10 events from the primary calendar.
     * @return List of Strings describing returned events.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun getDataFromApi(): List<String>? {
        // List the next 10 events from the primary calendar.
        val now =
            DateTime(System.currentTimeMillis())
        val eventStrings: MutableList<String> =
            ArrayList()
        val events: Events? =
            mActivity.mService?.events()?.list("primary")
                ?.setMaxResults(10)
                ?.setTimeMin(now)
                ?.setOrderBy("startTime")
                ?.setSingleEvents(true)
                ?.execute()
        val items =
            events?.items
        if (items != null) {
            for (event in items) {
                var start = event.start.dateTime
                if (start == null) {
                    // All-day events don't have start times, so just use
                    // the start date.
                    start = event.start.date
                }
                eventStrings.add(String.format("%s (%s)", event.summary, start))
            }
        }
        return eventStrings
    }

}