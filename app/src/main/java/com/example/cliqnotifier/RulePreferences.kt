package com.example.cliqnotifier

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class RulePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("cliq_rules", Context.MODE_PRIVATE)

    fun saveRules(rules: List<FilterRule>) {
        val jsonArray = JSONArray()
        for (rule in rules) {
            val obj = JSONObject().apply {
                put("id", rule.id)
                put("bankSenderId", rule.bankSenderId)
                put("requiredKeyword", rule.requiredKeyword)
                put("amountPrefix", rule.amountPrefix)
                put("amountSuffix", rule.amountSuffix)
                put("senderPrefix", rule.senderPrefix)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("rules_list", jsonArray.toString()).apply()
    }

    fun getRules(): MutableList<FilterRule> {
        val rulesList = mutableListOf<FilterRule>()
        // إن لم تكن هناك قواعد مخزنة، نعيد قائمة فارغة تماماً دون إنشاء أي بطاقة افتراضية
        val jsonString = prefs.getString("rules_list", null) ?: return mutableListOf()

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                rulesList.add(
                    FilterRule(
                        id = obj.optString("id"),
                        bankSenderId = obj.optString("bankSenderId"),
                        requiredKeyword = obj.optString("requiredKeyword"),
                        amountPrefix = obj.optString("amountPrefix"),
                        amountSuffix = obj.optString("amountSuffix"),
                        senderPrefix = obj.optString("senderPrefix")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return rulesList
    }
}
