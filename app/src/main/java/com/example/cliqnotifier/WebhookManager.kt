package com.example.cliqnotifier

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object WebhookManager {

    fun sendPayload(
        context: Context,
        webhookUrl: String,
        secretToken: String,
        bankSender: String,
        customerName: String,
        amount: Double,
        fullText: String,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(webhookUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("X-Secret-Token", secretToken)
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val jsonPayload = JSONObject().apply {
                    put("wallet_name", bankSender)
                    put("amount", amount)
                    put("customer_name", customerName)
                    put("raw_message", fullText)
                    put("timestamp", System.currentTimeMillis())
                }

                connection.outputStream.use { os ->
                    val input = jsonPayload.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }

                Log.d("CliQ_Webhook", "كود الاستجابة: $responseCode | النص: $responseText")

                withContext(Dispatchers.Main) {
                    if (responseCode == 200) {
                        onResult?.invoke(true, "نجح الإرسال ($responseCode)")
                    } else {
                        onResult?.invoke(false, "خطأ سيرفر ($responseCode): $responseText")
                    }
                }

            } catch (e: Exception) {
                Log.e("CliQ_Webhook", "خطأ أثناء إرسال الـ Webhook: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, "فشل الاتصال: ${e.message}")
                }
            } finally {
                connection?.disconnect()
            }
        }
    }
}
