package com.example.cliqnotifier

import android.util.Log

data class ParsedCliqData(
    val isMatched: Boolean,
    val amount: Double = 0.0,
    val customerName: String = "",
    val matchedBank: String = ""
)

class SmsParser(private val rules: List<FilterRule>) {

    fun parse(smsSender: String, smsBody: String): ParsedCliqData {
        for (rule in rules) {
            // 1. المطابقة حسب اسم المرسل (البنك)
            val isSenderMatch = smsSender.contains(rule.bankSenderId, ignoreCase = true) || 
                                rule.bankSenderId.contains(smsSender, ignoreCase = true) || 
                                rule.bankSenderId == "*"

            // 2. إذا كانت هناك رسالة نموذجية محفوظة، نتحقق من وجود كلمات مفتاحية مشتركة أو مطابقة البنك
            val isContentMatch = if (rule.sampleMessage.isNotEmpty()) {
                // نعتبر الرسالة متطابقة إذا كانت من نفس البنك وتحتوي على أرقام/مبالغ
                isSenderMatch
            } else {
                isSenderMatch
            }

            if (isSenderMatch && isContentMatch) {
                Log.d("CliQ_Parser", "طابقت الرسالة قاعدة البنك: ${rule.bankSenderId}")

                // استخراج المبلغ والاسم
                val extractedAmount = extractAnyNumber(smsBody)
                val extractedName = extractAnyName(smsBody)

                return ParsedCliqData(
                    isMatched = extractedAmount > 0.0,
                    amount = extractedAmount,
                    customerName = extractedName,
                    matchedBank = rule.bankSenderId
                )
            }
        }

        return ParsedCliqData(isMatched = false)
    }

    companion object {
        fun parseQuick(smsBody: String): ParsedCliqData {
            val amount = extractAnyNumber(smsBody)
            val name = extractAnyName(smsBody)

            return ParsedCliqData(
                isMatched = amount > 0.0,
                amount = amount,
                customerName = name,
                matchedBank = "اختبار"
            )
        }

        fun extractAnyNumber(text: String): Double {
            // يبحث عن الأرقام (مع الفواصل) المتبوعة بـ JOD / د.أ / دينار أو حتى أرقام مجردة
            val regex = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*(?:JOD|د\\.أ|دينار)?", RegexOption.IGNORE_CASE)
            val match = regex.find(text)
            return match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        }

        fun extractAnyName(text: String): String {
            // استخراج الاسم بعد كلمات مثل (من / from / by)
            val regex = Regex("(?:من|from|by)\\s+([\\p{L}\\s]+)", RegexOption.IGNORE_CASE)
            val match = regex.find(text)
            return match?.groupValues?.get(1)?.trim() ?: "غير محدد"
        }
    }
}
