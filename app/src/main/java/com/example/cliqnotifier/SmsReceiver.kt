package com.example.cliqnotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.originatingAddress ?: "مجهول"
                val messageBody = sms.messageBody ?: ""

                // إظهار إشعار سريع للتأكد من التقاط الرسالة
                Toast.makeText(context, "رسالة من: $sender\nالنص: $messageBody", Toast.LENGTH_LONG).show()
            }
        }
    }
}
