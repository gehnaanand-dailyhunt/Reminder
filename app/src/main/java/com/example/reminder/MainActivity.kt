package com.example.reminder

import android.Manifest
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.provider.CalendarContract
import android.util.Log
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.reminder.databinding.ActivityMainBinding
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import java.lang.Math.abs
import java.sql.Time
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var mYear: Int = 0
    var mMonth: Int = 0
    private var mDay: Int = 0
    private var mHour: Int = 0
    var mMinute: Int = 0
    var user: String = "Gehna"
    var mCredential: GoogleAccountCredential? = null
    var mProgress: ProgressDialog? = null
    lateinit var viewModel: CalendarViewModel
    private val SCOPES = arrayOf(CalendarScopes.CALENDAR_READONLY, CalendarScopes.CALENDAR)

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel = ViewModelProvider(this).get(CalendarViewModel::class.java)
        //methodWithPermissions()
        binding.btnDate.setOnClickListener{
            val calendar = Calendar.getInstance()
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth -> binding.date.text = dayOfMonth.toString() + "-" + (month + 1).toString() + "-" + "-" + year.toString()
                mYear = year
                mMonth = month
                mDay = dayOfMonth
                }, mYear, mMonth, mDay)

            datePickerDialog.show()
        }

        binding.btnTime.setOnClickListener{
            val calendar = Calendar.getInstance()
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this,
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute -> binding.time.text = "$hourOfDay:$minute"
                mMinute = minute
                mHour = hourOfDay
                }, mHour, mMinute, false)
            timePickerDialog.show()
            Log.i("Actual", mMinute.toString())
        }

        binding.notify.setOnClickListener {

            if(mHour == 0 || mMinute == 0 || mDay == 0)
                showToast("Select Date and Time")
            else {
                timer = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    clear()
                    set(Calendar.HOUR_OF_DAY, mHour)
                    set(Calendar.MINUTE, mMinute - 2)
                    set(Calendar.YEAR, mYear)
                    set(Calendar.MONTH, mMonth)
                    set(Calendar.DAY_OF_MONTH, mDay)
                }

                Log.i("mHour", mHour.toString())
                Log.i("mMinute", mMinute.toString())
                Log.i("mYear", mYear.toString())
                Log.i("mMonth", mMonth.toString())
                Log.i("mDay", mDay.toString())
                Log.i("Timer", timer.timeInMillis.toString())
                scheduleNotification(getNotification("$user is going live in 5min"), timer)
            }
        }

        initCredentials()

        binding.event.setOnClickListener {
            if(mHour == 0 || mMinute == 0 || mDay == 0)
                showToast("Select Date and Time")
            else {
                mProgress = ProgressDialog(this)
                mProgress!!.setMessage("Loading...")

                mCredential?.selectedAccountName = null
                chooseAccount()

                //startActivity(Intent(this, Calendar2::class.java))
            }
        }
    }

//---------------------------------------NOTIFICATION---------------------------------------

    private fun scheduleNotification(notification: Notification, timer: Calendar){
        val notificationIntent = Intent(this, NotificationPublisher::class.java)
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 1)
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification)

        val pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        //val futureInMillis = System.currentTimeMillis() + delay
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, timer.timeInMillis, pendingIntent)
    }

    private fun getNotification(content: String) : Notification{
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Live Stream by $user")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
        return builder.build()
    }

//---------------------------------------CALENDAR EVENT------------------------------------------

    private fun initCredentials() {
        //showToast("Permissions")
        val settings = getPreferences(Context.MODE_PRIVATE)
        mCredential = GoogleAccountCredential.usingOAuth2(
            applicationContext, listOf(*SCOPES)
        )
            .setBackOff(ExponentialBackOff())
            .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null))

    }

    private fun getResults() {
        //initCredentials()
        if (!viewModel.isGooglePlayServicesAvailable()) {
            viewModel.acquireGooglePlayServices()
        } else if (mCredential!!.selectedAccountName == null) {
            Log.i("GetData", "Choose account")
            chooseAccount()
        } else if (!viewModel.isDeviceOnline()) {
            showToast("No network connection available.")
        } else {
            Log.i("GetFunction", "true")
            //MakeRequestTask(mCredential!!).execute()
            createCalendarEvent()
        }
    }

    fun showToast(string: String){
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
    }

    private fun chooseAccount() {
        startActivityForResult(
            mCredential?.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER
        )
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode != Activity.RESULT_OK) {
                viewModel.isGooglePlayServicesAvailable()
                showToast("This app requires Google Play Services. Please install " + "Google Play Services on your device and relaunch this app.")
            } else {
                getResults()
                //createCalendarEvent()
                //getResultsFromApi()
            }
            REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data != null &&
                data.extras != null) {
                val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                if (accountName != null) {
                    val settings = getPreferences(Context.MODE_PRIVATE)
                    val editor = settings.edit()
                    editor.putString(PREF_ACCOUNT_NAME, accountName)
                    editor.apply()
                    mCredential!!.selectedAccountName = accountName
                    //createCalendarEvent()
                    //getResultsFromApi()
                    getResults()
                } else{
                    mProgress?.hide()
                }
            }
            REQUEST_AUTHORIZATION -> if (resultCode != Activity.RESULT_OK) {
                //getResultsFromApi()
                chooseAccount()
                //getResults()
            }
        }
    }

    private fun createCalendarEvent() {

        val event = Event()
            .setSummary("Live Stream by $user")
            .setDescription("Description")

        val startDateTime = DateTime(timer.timeInMillis + TimeUnit.MINUTES.toMillis(2))
        val start = EventDateTime()
            .setDateTime(startDateTime)
            .setTimeZone("Asia/Calcutta")
        event.start = start

        val endDateTime = DateTime(timer.timeInMillis + TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(2))
        val end = EventDateTime()
            .setDateTime(endDateTime)
            .setTimeZone("Asia/Calcutta")
        event.end = end

        //val recurrence = listOf("RRULE:FREQ=DAILY;COUNT=2")
        //event.recurrence = recurrence

        val attendees = listOf(
            EventAttendee().setEmail("gehnaanand@gmail.com"),
            EventAttendee().setEmail("gehna.anand@dailyhunt.in"))
        event.attendees = attendees

        val reminderOverrides = listOf(
            EventReminder().setMethod("email").setMinutes(24 * 60),
            EventReminder().setMethod("popup").setMinutes(5))

        val reminders = Event.Reminders()
            .setUseDefault(false)
            .setOverrides(reminderOverrides)
        event.reminders = reminders

        val calendarId = "primary"

        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()
        val service = com.google.api.services.calendar.Calendar.Builder(
            transport, jsonFactory, mCredential)
            .setApplicationName("Live Stream Reminder")
            .build()

        EventCreator(service, calendarId, event, mCredential).execute()
    }

    @SuppressLint("StaticFieldLeak")
    private inner class EventCreator internal constructor(val service: com.google.api.services.calendar.Calendar,
                                                          val calendarId: String,
                                                          val event: Event,
                                                          val credential: GoogleAccountCredential?) :
        AsyncTask<Void, Void, MutableList<String>>() {

        private var mLastError: Exception? = null

        override fun doInBackground(vararg params: Void?): MutableList<String>? {
            return try {

                service.events().insert(calendarId, event).execute().recurrence
            } catch (e: Exception) {
                e.printStackTrace()
                mLastError = e
                cancel(true)
                null
            }
        }

        override fun onPreExecute() {
            super.onPreExecute()
            mProgress!!.show()
        }

        override fun onPostExecute(result: MutableList<String>?) {
            super.onPostExecute(result)
            Log.d("MainActivity", result.toString())
            Toast.makeText(this@MainActivity, "Event added successfully", Toast.LENGTH_SHORT).show()
            mProgress!!.hide()
        }

        override fun onCancelled() {
            mProgress!!.hide()
            if (mLastError != null) {
                when (mLastError) {
                    is GooglePlayServicesAvailabilityIOException -> {
                        viewModel.showGooglePlayServicesAvailabilityErrorDialog(
                            (mLastError as GooglePlayServicesAvailabilityIOException)
                                .connectionStatusCode)
                    }
                    is UserRecoverableAuthIOException -> {
                        showToast("Credentials")
                        startActivityForResult(
                            (mLastError as UserRecoverableAuthIOException).intent,
                            REQUEST_AUTHORIZATION
                        )
                    }
                    else -> {
                        showToast("The following error occurred:\n" + mLastError!!.message)
                    }
                }
            } else {
                showToast("Request cancelled.")
            }
        }
    }

    companion object{
        const val NOTIFICATION_CHANNEL_ID = "10001"
        lateinit var timer : Calendar
        const val REQUEST_ACCOUNT_PICKER = 1000
        const val REQUEST_AUTHORIZATION = 1001
        const val REQUEST_GOOGLE_PLAY_SERVICES = 1002
        const val REQUEST_PERMISSION_GET_ACCOUNTS = 1003
        const val PREF_ACCOUNT_NAME = "accountName"
    }
}
