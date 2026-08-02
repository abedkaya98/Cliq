package com.example.cliqnotifier

data class FilterRule(
    val id: String = System.currentTimeMillis().toString(),
    var bankSenderId: String,      // اسم مرسل الـ SMS (مثلاً: Reflect أو CAB)
    var requiredKeyword: String,   // كلمة ملزمة بالرسالة (مثلاً: CliQ)
    var amountPrefix: String,      // الكلمة قبل المبلغ (مثلاً: مبلغ)
    var amountSuffix: String,      // الكلمة بعد المبلغ (مثلاً: JOD)
    var senderPrefix: String       // الكلمة قبل اسم الشخص (مثلاً: من)
)
