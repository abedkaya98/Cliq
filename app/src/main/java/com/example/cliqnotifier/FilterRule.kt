package com.example.cliqnotifier

data class FilterRule(
    val bankSenderId: String,
    val sampleMessage: String = "", // الحقل الجديد للحفظ
    val requiredKeyword: String = "",
    val amountPrefix: String = "",
    val amountSuffix: String = "",
    val senderPrefix: String = ""
)
