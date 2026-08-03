package com.example.cliqnotifier

data class FilterRule(
    val id: String = System.currentTimeMillis().toString(),
    var bankSenderId: String = "",      // اسم مرسل الـ SMS (مثلاً: Reflect أو CAB)
    var sampleMessage: String = "",     // الرسالة النموذجية المختارة من القائمة
    var requiredKeyword: String = "",   // كلمة ملزمة بالرسالة
    var amountPrefix: String = "",      // الكلمة قبل المبلغ
    var amountSuffix: String = "",      // الكلمة بعد المبلغ
    var senderPrefix: String = ""       // الكلمة قبل اسم الشخص
)
