package com.example.cliqnotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

            val appPrefs = AppPreferences(context)
            val rulePrefs = RulePreferences(context)

            val webhookUrl = appPrefs.webhookUrl
            val secretToken = appPrefs.secretToken
            val rules = rulePrefs.getRules()

            // إذا لم يكن رابط الـ Webhook مضافاً أو القواعد فارغة، نوقف المعالجة
            if (webhookUrl.isEmpty() || rules.isEmpty()) {
                Log.d("CliQ_Receiver", "رابط الـ Webhook أو قائمة القواعد فارغة.")
                return
            }

            val parser = SmsParser(rules)

            for (sms in messages) {
                val sender = sms.originatingAddress ?: "مجهول"
                val messageBody = sms.messageBody ?: ""

                Log.d("CliQ_Receiver", "وصلت رسالة جديدة من: $sender")

                // فحص الرسالة عبر القواعد
                val result = parser.parse(sender, messageBody)

                if (result.isMatched && result.amount > 0.0) {
                    Log.d("CliQ_Receiver", "تم تطابق الرسالة بنجاح! المبلغ: ${result.amount}")

                    // إرسال البيانات فوراً للـ Webhook
                    WebhookManager.sendPayload(
                        context = context,
                        webhookUrl = webhookUrl,
                        secretToken = secretToken,
                        bankSender = result.matchedBank,
                        customerName = result.customerName,
                        amount = result.amount,
                        fullText = messageBody
                    )
                } else {
                    Log.d("CliQ_Receiver", "الرسالة لم تطابق أي قاعدة محددة.")
                }
            }
        }
    }
}
