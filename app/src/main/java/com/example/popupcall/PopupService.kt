// PopupService.kt
package com.example.popupcall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.IBinder
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.*
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class PopupService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var dbHelper: DBHelper
    private val CHANNEL_ID = "popup_call_channel"

    override fun onCreate() {
        super.onCreate()
        dbHelper = DBHelper(this)
        createNotificationChannel()
        startForegroundServiceWithNotification()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Popup Call Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Notifications for call popups" }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceWithNotification() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Popup Service Running")
            .setContentText("Listening for call events.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra("number") ?: "Unknown Number"
        val callState = intent?.getStringExtra("state") ?: "IDLE"

        if (callState == TelephonyManager.EXTRA_STATE_IDLE) {
            val callHistory = getCallHistory(phoneNumber)

            if (callHistory.isNotEmpty()) {
                val latestCall = callHistory.first()
                dbHelper.insertCall(phoneNumber, "Existing", "", latestCall.duration, latestCall.date)
                showSavedCallPopup(phoneNumber, callHistory)
            } else {
                showNewCallPopup(phoneNumber)
            }
        }

        return START_NOT_STICKY
    }

    data class CallDetails(val duration: String, val date: String)

    private fun getCallHistory(phoneNumber: String): List<CallDetails> {
        val history = mutableListOf<CallDetails>()
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE
        )

        val cursor: Cursor? = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            "${CallLog.Calls.NUMBER} = ?",
            arrayOf(phoneNumber),
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val durationIndex = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val dateIndex = it.getColumnIndexOrThrow(CallLog.Calls.DATE)

            while (it.moveToNext()) {
                val durationInSeconds = it.getLong(durationIndex)
                val callDate = Date(it.getLong(dateIndex))
                val formattedDuration = formatDuration(durationInSeconds)
                val formattedDate = dateFormat.format(callDate)
                history.add(CallDetails(formattedDuration, formattedDate))
            }
        }
        return history
    }

    private fun formatDuration(seconds: Long): String =
        if (seconds < 60) "$seconds sec" else "${seconds / 60} min ${seconds % 60} sec"

    private fun showSavedCallPopup(number: String, callHistory: List<CallDetails>) {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_saved_call, null)

        popupView.findViewById<TextView>(R.id.tvPhoneNumber).text = "Number: $number"

        val historyListView = popupView.findViewById<ListView>(R.id.lvCallHistory)
        val historyItems = callHistory.map { "${it.date}  |  Duration: ${it.duration}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historyItems)
        historyListView.adapter = adapter

        popupView.findViewById<Button>(R.id.btnClose).setOnClickListener {
            removePopupView(popupView)
        }

        showPopupWindow(popupView)
    }

    private fun showNewCallPopup(number: String) {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_new_call, null)

        popupView.findViewById<TextView>(R.id.tvPhoneNumber).text = "Incoming Number: $number"
        val notesEditText = popupView.findViewById<EditText>(R.id.etNotes).apply {
            isFocusableInTouchMode = true
            requestFocus()
        }

        val statusSpinner = popupView.findViewById<Spinner>(R.id.spinnerStatus)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.call_status_options,
            android.R.layout.simple_spinner_item
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        statusSpinner.adapter = adapter

        popupView.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val notes = notesEditText.text.toString().trim()
            val status = statusSpinner.selectedItem?.toString() ?: "New"
            dbHelper.insertCall(number, status, notes, "0 sec", getCurrentTime())
            Toast.makeText(this, "Call details saved.", Toast.LENGTH_SHORT).show()
            removePopupView(popupView)
        }

        popupView.findViewById<Button>(R.id.btnSkip).setOnClickListener {
            removePopupView(popupView)
        }

        showPopupWindow(popupView)
    }

    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun showPopupWindow(popupView: android.view.View) {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        try {
            windowManager.addView(popupView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removePopupView(popupView: android.view.View) {
        try {
            windowManager.removeViewImmediate(popupView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
