package com.example.popupcall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.widget.Toast

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            if (TelephonyManager.EXTRA_STATE_IDLE == state && incomingNumber != null) {
                Toast.makeText(context, "Incoming Call: $incomingNumber", Toast.LENGTH_SHORT).show()

                val popupIntent = Intent(context, PopupService::class.java).apply {
                    putExtra("number", incomingNumber)
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(popupIntent)
                } else {
                    context.startService(popupIntent)
                }
            }
        }
    }
}
