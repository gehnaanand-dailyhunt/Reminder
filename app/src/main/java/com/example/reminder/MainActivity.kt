package com.example.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import com.example.reminder.databinding.ActivityMainBinding
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

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

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
            /*var hour = kotlin.math.abs(mHour - Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
            var min = mMinute - Calendar.getInstance().get(Calendar.MINUTE)
            if(min < 0) {
                min += 60
                hour -= 1
            }
            val delay: Long = TimeUnit.HOURS.toMillis(hour.toLong()) + TimeUnit.MINUTES.toMillis(min.toLong())*/

            timer = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                clear()
                set(Calendar.HOUR_OF_DAY, mHour)
                set(Calendar.MINUTE, mMinute - 5)
                set(Calendar.YEAR, mYear)
                set(Calendar.MONTH, mMonth)
                set(Calendar.DAY_OF_MONTH, mDay)
            }

            Log.i("mHour", mHour.toString())
            Log.i("mMinute",mMinute.toString())
            Log.i("mYear", mYear.toString())
            Log.i("mMonth", mMonth.toString())
            Log.i("mDay", mDay.toString())
            Log.i("Timer", timer.timeInMillis.toString())
            scheduleNotification(getNotification("$user is going live in 5min"), timer)
        }

        binding.event.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_CALENDAR
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR), 3)
            } else {
                val calID: Long = 3
                val startMillis: Long = Calendar.getInstance().run {
                    set(mYear, mMonth, mDay, mHour, mMinute)
                    timeInMillis
                }
                val endMillis: Long = Calendar.getInstance().run {
                    set(mYear, mMonth, mDay, mHour + 1, mMinute)
                    timeInMillis
                }

                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.TITLE, "Live Streaming")
                    put(CalendarContract.Events.DESCRIPTION, "Calendar event")
                    put(CalendarContract.Events.CALENDAR_ID, calID)
                    put(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Calcutta")
                }

                val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                Log.i("000000", "Inserted")
                val eventID: Long = uri?.lastPathSegment?.toLong()!!
            }*/
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val calID: Long = 3
            val startMillis: Long = Calendar.getInstance().run {
                set(mYear, mMonth, mDay, mHour, mMinute)
                timeInMillis
            }
            val endMillis: Long = Calendar.getInstance().run {
                set(mYear, mMonth, mDay, mHour + 1, mMinute)
                timeInMillis
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, "Live Streaming")
                put(CalendarContract.Events.DESCRIPTION, "Calendar event")
                put(CalendarContract.Events.CALENDAR_ID, calID)
                put(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Calcutta")
            }

            val uri = if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_CALENDAR
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }else {
                Log.i("0000000"," Inserted")
                contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            }
            val eventID: Long = uri?.lastPathSegment?.toLong()!!
        }
        else {
            Toast.makeText(this,"Permission Required to Fetch Gallery.", Toast.LENGTH_SHORT).show()
            super.onBackPressed()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    /*fun calendarEvent() {
        val calID: Long = 3
        val startMillis: Long = Calendar.getInstance().run {
            set(mYear, mMonth, mDay, mHour, mMinute)
            timeInMillis
        }
        val endMillis: Long = Calendar.getInstance().run {
            set(mYear, mMonth, mDay, mHour + 1, mMinute)
            timeInMillis
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, "Live Streaming")
            put(CalendarContract.Events.DESCRIPTION, "Calendar event")
            put(CalendarContract.Events.CALENDAR_ID, calID)
            put(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Calcutta")
        }

        val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val eventID: Long = uri?.lastPathSegment?.toLong()!!
    }*/

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
            .setContentTitle("Scheduled Notification")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun methodWithPermissions() =
        runWithPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE) {
            binding.event.setOnClickListener {
                //calendarEvent()
                //startActivity(Intent(this, CalendarActivity::class.java))
            }
        }

    companion object{
        const val NOTIFICATION_CHANNEL_ID = "10001"
        lateinit var timer : Calendar
    }
}
