package com.example.popupcall

import android.app.Service
import android.content.Intent
import android.database.Cursor
import android.graphics.PixelFormat
import android.net.Uri
import android.os.IBinder
import android.provider.ContactsContract
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView

class PopupService : Service() {
    private lateinit var windowManager: WindowManager
    private var popupView: android.view.View? = null
    private lateinit var dbHelper: DBHelper

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        dbHelper = DBHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("number")?.let { showPopup(it) }
        return START_NOT_STICKY
    }

    private fun showPopup(number: String) {
        val inflater = LayoutInflater.from(this)
        popupView = inflater.inflate(R.layout.popup_layout, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        popupView?.apply {
            val nameText = findViewById<TextView>(R.id.nameText)
            val numberText = findViewById<TextView>(R.id.numberText)
            val typeSpinner = findViewById<Spinner>(R.id.typeSpinner)
            val noteEdit = findViewById<EditText>(R.id.noteEdit)
            val saveButton = findViewById<Button>(R.id.saveButton)
            val skipButton = findViewById<Button>(R.id.skipButton)

            nameText.text = getContactName(number)?.let { "Name: $it" } ?: "Unknown Number"
            numberText.text = "Number: $number"

            if (dbHelper.isNumberSaved(number)) {
                typeSpinner.visibility = android.view.View.GONE
                noteEdit.visibility = android.view.View.GONE
                saveButton.visibility = android.view.View.GONE
                skipButton.text = "Close"
                skipButton.setOnClickListener { stopSelf() }
            } else {
                typeSpinner.adapter = ArrayAdapter(
                    this@PopupService,
                    android.R.layout.simple_spinner_item,
                    listOf("New", "Lead", "Existing")
                )

                saveButton.setOnClickListener {
                    val type = typeSpinner.selectedItem.toString()
                    val note = noteEdit.text.toString()
                    dbHelper.saveCall(number, type, note)
                    stopSelf()
                }

                skipButton.setOnClickListener { stopSelf() }
            }
        }

        windowManager.addView(popupView, params)
    }

    private fun getContactName(number: String): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        return null
    }

    override fun onDestroy() {
        popupView?.let { windowManager.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
