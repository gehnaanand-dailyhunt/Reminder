package com.example.reminder

import android.os.AsyncTask
import android.util.Log
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import java.io.IOException
import java.util.*

class CreateEventTask(mService: Calendar?) : AsyncTask<Void, Void, Void>() {
    var mService: Calendar? = null


    override fun doInBackground(vararg params: Void?): Void? {
        addCalendarEvent()
        return null
    }

    fun addCalendarEvent() {
        val event =
            Event()
                .setSummary("Google I/O 2015")
                .setLocation("800 Howard St., San Francisco, CA 94103")
                .setDescription("A chance to hear more about Google's developer products.")
        val startDateTime =
            DateTime(System.currentTimeMillis())
        val start = EventDateTime()
            .setDateTime(startDateTime)
            .setTimeZone("Asia/Calcutta")
        event.start = start
        val endDateTime =
            DateTime(System.currentTimeMillis() + 10000)
        val end = EventDateTime()
            .setDateTime(endDateTime)
            .setTimeZone("Asia/Calcutta")
        event.end = end
        val recurrence =
            arrayOf("RRULE:FREQ=DAILY;COUNT=2")
        event.recurrence = listOf(*recurrence)
        val attendees = arrayOf(
            EventAttendee().setEmail("gehnaanand@gmail.com"),
            EventAttendee().setEmail("sweetiegehna@gmail.com")
        )
        event.attendees = listOf(*attendees)
        val reminderOverrides = arrayOf(
            EventReminder().setMethod("email").setMinutes(24 * 60),
            EventReminder().setMethod("popup").setMinutes(10)
        )
        val reminders =
            Event.Reminders()
                .setUseDefault(false)
                .setOverrides(listOf(*reminderOverrides))
        event.reminders = reminders
        val calendarId = "primary"
        try {
            Log.i("Testingggg", "Inserting")
            mService!!.events().insert(calendarId, event).execute()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}