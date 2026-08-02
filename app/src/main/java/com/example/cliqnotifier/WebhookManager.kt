package com.example.cliqnotifier

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class WebhookManager(private val context: Context) {

    private val prefs = AppPreferences(context)

    suspend fun sendPaymentNotification(
        amount: Double,
        customerName: String,
        rawMessage: String,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean = withContext(Dispatchers.IO) {
        val webhookUrl = prefs.webhookUrl
        val secretToken = prefs.secretToken
        val walletName = prefs.walletName

        if (webhookUrl.isEmpty()) {
            Log.e("CliQ_Webhook", "رابط الـ Webhook غير مفعّل أو فارغ!")
            return@withContext false
        }

        try {
            val url = URL(webhookUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Secret-Token", secretToken)
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val jsonInput = JSONObject().apply {
                put("wallet_name", walletName)
                put("amount", amount)
                put("customer_name", customerName)
                put("raw_message", rawMessage)
                put("timestamp", timestamp)
            }

            connection.outputStream.use { os ->
                val input = jsonInput.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            Log.d("CliQ_Webhook", "Webhook Response Code: $responseCode")

            responseCode == 200
        } catch (e: Exception) {
            Log.e("CliQ_Webhook", "Exception during sending webhook: ${e.message}", e)
            false
        }
    }
}
