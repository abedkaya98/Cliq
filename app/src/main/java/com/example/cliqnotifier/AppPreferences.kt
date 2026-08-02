package com.example.cliqnotifier

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("cliq_settings", Context.MODE_PRIVATE)

    var webhookUrl: String
        get() = prefs.getString("webhook_url", "https://yourdomain.com/webhook.php") ?: ""
        set(value) = prefs.edit().putString("webhook_url", value).apply()

    var secretToken: String
        get() = prefs.getString("secret_token", "CliqSecret2026") ?: ""
        set(value) = prefs.edit().putString("secret_token", value).apply()

    var walletName: String
        get() = prefs.getString("wallet_name", "MainWallet") ?: ""
        set(value) = prefs.edit().putString("wallet_name", value).apply()
}
