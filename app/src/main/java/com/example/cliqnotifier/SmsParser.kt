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
            val isKeywordMatch = rule.requiredKeyword.isEmpty() || smsBody.contains(rule.requiredKeyword, ignoreCase = true)

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

    companion object {
        // دالة تجريبية سريعة تستخدم في زر التجربة (Test Match) بالشاشة
        fun parseQuick(smsBody: String): ParsedCliqData {
            val amount = extractAnyNumber(smsBody)
            val name = extractAnyName(smsBody)
            val matched = amount > 0.0

            return ParsedCliqData(
                isMatched = matched,
                amount = amount,
                customerName = name,
                matchedBank = "اختبار سريع"
            )
        }

        private fun extractAnyNumber(text: String): Double {
            // يبحث عن الأرقام التي تحتوي على فواصل عشرية أو أرقام صحيحة المتبوعة بـ JOD / د.أ / دينار
            val regex = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*(?:JOD|د\\.أ|دينار|JOD)?", RegexOption.IGNORE_CASE)
            val match = regex.find(text)
            return match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        }

        private fun extractAnyName(text: String): String {
            // محاولة استخراج الاسم العربي بعد كلمات مثل (من / From)
            val regex = Regex("(?:من|from)\\s+([\\p{L}\\s]+)", RegexOption.IGNORE_CASE)
            val match = regex.find(text)
            return match?.groupValues?.get(1)?.trim() ?: "غير محدد"
        }
    }

    private fun extractAmount(text: String, prefix: String, suffix: String): Double {
        return try {
            if (prefix.isNotEmpty() || suffix.isNotEmpty()) {
                val regexString = "${Regex.escape(prefix)}\\s*([0-9]+(?:\\.[0-9]+)?)\\s*${Regex.escape(suffix)}"
                val regex = Regex(regexString, RegexOption.IGNORE_CASE)
                val match = regex.find(text)
                match?.groupValues?.get(1)?.toDoubleOrNull() ?: extractAnyNumber(text)
            } else {
                extractAnyNumber(text)
            }
        } catch (e: Exception) {
            extractAnyNumber(text)
        }
    }

    private fun extractAnyNumber(text: String): Double {
        return Companion.extractAnyNumber(text)
    }

    private fun extractCustomerName(text: String, prefix: String): String {
        return try {
            if (prefix.isNotEmpty()) {
                val regex = Regex("${Regex.escape(prefix)}\\s+([\\p{L}\\s]+)", RegexOption.IGNORE_CASE)
                val match = regex.find(text)
                match?.groupValues?.get(1)?.trim() ?: "مجهول"
            } else {
                Companion.extractAnyName(text)
            }
        } catch (e: Exception) {
            "مجهول"
        }
    }
}
