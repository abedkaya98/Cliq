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
            // 1. التحقق من اسم المرسل والكلمة المفتاحية
            val isSenderMatch = smsSender.contains(rule.bankSenderId, ignoreCase = true) || rule.bankSenderId == "*"
            val isKeywordMatch = smsBody.contains(rule.requiredKeyword, ignoreCase = true)

            if (isSenderMatch && isKeywordMatch) {
                Log.d("CliQ_Parser", "طابقت الرسالة قاعدة البنك: ${rule.bankSenderId}")

                // 2. استخراج المبلغ برمجياً
                val extractedAmount = extractAmount(smsBody, rule.amountPrefix, rule.amountSuffix)

                // 3. استخراج اسم العميل/المرسل
                val extractedName = extractCustomerName(smsBody, rule.senderPrefix)

                return ParsedCliqData(
                    isMatched = true,
                    amount = extractedAmount,
                    customerName = extractedName,
                    matchedBank = rule.bankSenderId
                )
            }
        }

        return ParsedCliqData(isMatched = false)
    }

    private fun extractAmount(text: String, prefix: String, suffix: String): Double {
        return try {
            val regexString = "$prefix\\s*([0-9]+(?:\\.[0-9]+)?)\\s*$suffix"
            val regex = Regex(regexString, RegexOption.IGNORE_CASE)
            val match = regex.find(text)
            match?.groupValues?.get(1)?.toDoubleOrNull() ?: extractAnyNumber(text)
        } catch (e: Exception) {
            extractAnyNumber(text)
        }
    }

    private fun extractAnyNumber(text: String): Double {
        val regex = Regex("([0-9]+(?:\\.[0-9]+)?)")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun extractCustomerName(text: String, prefix: String): String {
        return try {
            val regex = Regex("$prefix\\s+([\\p{L}\\s]+)", RegexOption.IGNORE_CASE)
            val match = regex.find(text)
            match?.groupValues?.get(1)?.trim() ?: "مجهول"
        } catch (e: Exception) {
            "مجهول"
        }
    }
}
