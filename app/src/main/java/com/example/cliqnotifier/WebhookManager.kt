package com.example.cliqnotifier

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
        fullText: String
    ) {
        // تشغيل الإرسال في الخلفية (Background Thread) لعدم تعطيل التطبيق
        CoroutineScope(Dispatchers.IO).launch {
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

                // بناء الـ JSON المطلوب إرساله لسيرفرك
                val jsonPayload = JSONObject().apply {
                    put("bank_sender", bankSender)
                    put("amount", amount)
                    put("customer_name", customerName)
                    put("full_text", fullText)
                    put("secret_token", secretToken)
                    put("timestamp", System.currentTimeMillis())
                }

                connection.outputStream.use { os ->
                    val input = jsonPayload.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                Log.d("CliQ_Webhook", "تم إرسال الـ Webhook بنجاح. كود الاستجابة: $responseCode")

            } catch (e: Exception) {
                Log.e("CliQ_Webhook", "خطأ أثناء إرسال الـ Webhook: ${e.message}", e)
            }
        }
    }
}
