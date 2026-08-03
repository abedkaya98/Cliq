package com.example.cliqnotifier

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("cliq_settings", Context.MODE_PRIVATE)

    // جعل القيم الافتراضية فارغة تماماً لمنع ظهور بطاقة افتراضية عند أول تثبيت
    var webhookUrl: String
        get() = prefs.getString("webhook_url", "") ?: ""
        set(value) = prefs.edit().putString("webhook_url", value).apply()

    var secretToken: String
        get() = prefs.getString("secret_token", "") ?: ""
        set(value) = prefs.edit().putString("secret_token", value).apply()

    var walletName: String
        get() = prefs.getString("wallet_name", "") ?: ""
        set(value) = prefs.edit().putString("wallet_name", value).apply()
}
