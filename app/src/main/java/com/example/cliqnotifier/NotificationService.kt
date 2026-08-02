package com.example.cliqnotifier

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn?.packageName ?: return
        val extras = sbn.notification?.extras
        val title = extras?.getString("android.title") ?: ""
        val text = extras?.getCharSequence("android.text")?.toString() ?: ""

        Log.d("CliQ_Service", "إشعار جديد من: $packageName | العنوان: $title | النص: $text")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
