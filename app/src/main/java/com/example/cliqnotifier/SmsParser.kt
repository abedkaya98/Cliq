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
            // المطابقة حسب اسم المرسل
            val isSenderMatch = smsSender.contains(rule.bankSenderId, ignoreCase = true) || 
                                rule.bankSenderId.contains(smsSender, ignoreCase = true) || 
                                rule.bankSenderId == "*"

            if (isSenderMatch) {
                Log.d("CliQ_Parser", "طابقت الرسالة قاعدة البنك: ${rule.bankSenderId}")

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
            // البحث عن النمط الأوضح أولاً: رقم متبوع بـ JOD أو د.أ أو دينار أو مسبوق بـ Amount/بقيمة
            val patterns = listOf(
                Regex("(?:Amount|مبلغ|بقيمة)\\s*([0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE),
                Regex("([0-9]+(?:\\.[0-9]+)?)\\s*(?:JOD|د\\.أ|دينار)", RegexOption.IGNORE_CASE),
                Regex("([0-9]+\\.[0-9]{2,3})") // أي رقم عشري ملفت
            )

            for (pattern in patterns) {
                val match = pattern.find(text)
                if (match != null) {
                    val valStr = match.groupValues[1]
                    val parsed = valStr.toDoubleOrNull()
                    if (parsed != null && parsed > 0) return parsed
                }
            }

            return 0.0
        }

        fun extractAnyName(text: String): String {
            // استخراج الاسم بعد كلمات الربط الشهيرة باللغتين
            val regexList = listOf(
                Regex("(?:من|from|by|received from)\\s+([\\p{L}\\s]+?)(?:\\.|,|\\s+using|\\s+via|\\s+\\d|$)", RegexOption.IGNORE_CASE),
                Regex("CliQ Service:?\\s*([\\p{L}\\s]+)", RegexOption.IGNORE_CASE)
            )

            for (regex in regexList) {
                val match = regex.find(text)
                if (match != null) {
                    val name = match.groupValues[1].trim()
                    if (name.length > 2) return name
                }
            }

            return "غير محدد"
        }
    }
}
