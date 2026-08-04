package com.example.cliqnotifier

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cliqnotifier.databinding.ActivityMainBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: AppPreferences
    private lateinit var rulePrefs: RulePreferences
    private val SMS_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)
        rulePrefs = RulePreferences(this)

        loadSettings()

        binding.btnSaveSettings.setOnClickListener { saveSettings() }
        binding.btnAddCard.setOnClickListener { addNewFilterCard() }
        binding.btnTest.setOnClickListener { checkAndRequestSmsPermission() }
        binding.btnRefreshLogs.setOnClickListener { fetchDatabaseLogs() }

        checkAndRequestSmsPermission()
        fetchDatabaseLogs()
    }

    private fun addNewFilterCard(savedSender: String? = null, savedMessage: String? = null) {
        val cardView = LayoutInflater.from(this).inflate(R.layout.item_filter_card, binding.cardsContainerLayout, false)

        val spinnerBank = cardView.findViewById<Spinner>(R.id.spinnerBank)
        val spinnerSampleSms = cardView.findViewById<Spinner>(R.id.spinnerSampleSms)
        val btnTestMatch = cardView.findViewById<Button>(R.id.btnTestMatch)
        val btnSendTestWebhook = cardView.findViewById<Button>(R.id.btnSendTestWebhook)
        val tvTestResult = cardView.findViewById<TextView>(R.id.tvTestResult)
        val btnDelete = cardView.findViewById<Button>(R.id.btnDeleteCard)

        val sendersList = getSmsSenders()
        val bankAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sendersList)
        spinnerBank.adapter = bankAdapter

        savedSender?.let { sender ->
            val index = sendersList.indexOf(sender)
            if (index >= 0) spinnerBank.setSelection(index)
        }

        spinnerBank.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedSender = sendersList[position]
                if (position > 0) {
                    val messages = getMessagesFromSender(selectedSender)
                    val smsAdapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, messages)
                    spinnerSampleSms.adapter = smsAdapter

                    savedMessage?.let { msg ->
                        val msgIndex = messages.indexOf(msg)
                        if (msgIndex >= 0) spinnerSampleSms.setSelection(msgIndex)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnTestMatch.setOnClickListener {
            val selectedMessage = spinnerSampleSms.selectedItem?.toString() ?: ""
            if (selectedMessage.isNotEmpty() && selectedMessage != "لا توجد رسائل سابقة") {
                val parsedResult = SmsParser.parseQuick(selectedMessage)

                if (parsedResult.isMatched) {
                    tvTestResult.text = "✅ تم التحليل بنجاح:\nالمبلغ: ${parsedResult.amount}\nالمرسل/الهدف: ${parsedResult.customerName}"
                    tvTestResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                } else {
                    tvTestResult.text = "⚠️ الرسالة لا تطابق صيغة التحليل الحالية!\nالنص: $selectedMessage"
                    tvTestResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                }
            } else {
                tvTestResult.text = "يرجى اختيار مرسل ورسالة أولاً!"
                tvTestResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
        }

        // إرسال تجريبي باستخدام WebhookManager مباشرة
btnSendTestWebhook.setOnClickListener {
    val selectedSender = spinnerBank.selectedItem?.toString() ?: ""
    val selectedMessage = spinnerSampleSms.selectedItem?.toString() ?: ""

    if (selectedMessage.isEmpty() || selectedMessage == "لا توجد رسائل سابقة") {
        Toast.makeText(this, "اختر رسالة أولاً لإرسالها!", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
    }

    val parsedResult = SmsParser.parseQuick(selectedMessage)
    val walletName = if (selectedSender.isNotEmpty() && selectedSender != "اختر اسم المرسل من القائمة...") selectedSender else "TestWallet"

    tvTestResult.text = "⏳ جاري الإرسال للسيرفر..."
    tvTestResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))

    WebhookManager.sendPayload(
        context = this,
        webhookUrl = prefs.webhookUrl.trim(),
        secretToken = prefs.secretToken.trim(),
        bankSender = walletName,
        customerName = parsedResult.customerName,
        amount = parsedResult.amount,
        fullText = selectedMessage
    ) { isSuccess, message ->
        if (isSuccess) {
            tvTestResult.text = "✅ $message"
            tvTestResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            fetchDatabaseLogs() // تحديث الجدول السفلي فوراً
        } else {
            tvTestResult.text = "❌ $message"
            tvTestResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }
}


        btnDelete.setOnClickListener {
            binding.cardsContainerLayout.removeView(cardView)
            saveSettings()
            Toast.makeText(this, "تم حذف البطاقة", Toast.LENGTH_SHORT).show()
        }

        binding.cardsContainerLayout.addView(cardView)
    }

    private fun getSmsSenders(): List<String> {
        val sendersSet = mutableSetOf("اختر اسم المرسل من القائمة...")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val cursor = contentResolver.query(
                    Uri.parse("content://sms/inbox"),
                    arrayOf("address"),
                    null,
                    null,
                    "date DESC"
                )

                cursor?.use {
                    val addressIndex = it.getColumnIndex("address")
                    while (it.moveToNext()) {
                        if (addressIndex != -1) {
                            val address = it.getString(addressIndex)
                            if (!address.isNullOrEmpty()) sendersSet.add(address)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return sendersSet.toList()
    }

    private fun getMessagesFromSender(sender: String): List<String> {
        val messages = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val cursor = contentResolver.query(
                    Uri.parse("content://sms/inbox"),
                    arrayOf("body"),
                    "address = ?",
                    arrayOf(sender),
                    "date DESC"
                )

                cursor?.use {
                    val bodyIndex = it.getColumnIndex("body")
                    while (it.moveToNext()) {
                        if (bodyIndex != -1) {
                            val body = it.getString(bodyIndex)
                            if (!body.isNullOrEmpty()) messages.add(body)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return if (messages.isEmpty()) listOf("لا توجد رسائل سابقة") else messages
    }

    private fun loadSettings() {
        binding.etWebhookUrl.setText(prefs.webhookUrl)
        binding.etSecretToken.setText(prefs.secretToken)

        val savedRules = rulePrefs.getRules()
        binding.cardsContainerLayout.removeAllViews()
        for (rule in savedRules) {
            addNewFilterCard(rule.bankSenderId, rule.sampleMessage)
        }
    }

    private fun saveSettings() {
        val url = binding.etWebhookUrl.text.toString().trim()
        val token = binding.etSecretToken.text.toString().trim()

        if (url.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال رابط الـ Webhook", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.webhookUrl = url
        prefs.secretToken = token

        val rulesList = mutableListOf<FilterRule>()
        val container = binding.cardsContainerLayout
        for (i in 0 until container.childCount) {
            val cardView = container.getChildAt(i)
            val spinnerBank = cardView.findViewById<Spinner>(R.id.spinnerBank)
            val spinnerSampleSms = cardView.findViewById<Spinner>(R.id.spinnerSampleSms)

            val selectedSender = spinnerBank.selectedItem?.toString() ?: ""
            val selectedMessage = spinnerSampleSms.selectedItem?.toString() ?: ""

            if (selectedSender.isNotEmpty() && selectedSender != "اختر اسم المرسل من القائمة...") {
                rulesList.add(
                    FilterRule(
                        bankSenderId = selectedSender,
                        sampleMessage = if (selectedMessage != "لا توجد رسائل سابقة") selectedMessage else ""
                    )
                )
            }
        }

        rulePrefs.saveRules(rulesList)
        Toast.makeText(this, "تم حفظ الإعدادات والرسائل بنجاح!", Toast.LENGTH_SHORT).show()
    }

    private fun fetchDatabaseLogs() {
        val baseUrl = prefs.webhookUrl.trim()
        val token = prefs.secretToken.trim()

        if (baseUrl.isEmpty()) {
            binding.tvLogsContent.text = "فارغ (قم بإدخال رابط الـ Webhook أولاً)"
            return
        }

        binding.tvLogsContent.text = "جاري التحميل..."

        thread {
            try {
                val logsUrl = if (baseUrl.endsWith("webhook.php")) {
                    baseUrl.replace("webhook.php", "get_logs.php")
                } else {
                    val cleanUrl = baseUrl.substringBeforeLast("/")
                    "$cleanUrl/get_logs.php"
                }

                val url = URL(logsUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("X-Secret-Token", token)
                conn.connectTimeout = 5000

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val dataArray = json.getJSONArray("data")

                    runOnUiThread {
                        if (dataArray.length() == 0) {
                            binding.tvLogsContent.text = "فارغ (لا توجد عمليات جديدة)"
                        } else {
                            val builder = StringBuilder()
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val amount = item.getDouble("amount")
                                val customer = item.getString("customer_name")
                                builder.append("${i + 1}- مبلغ $amount د.أ من $customer\n")
                            }
                            binding.tvLogsContent.text = builder.toString().trim()
                        }
                    }
                } else {
                    runOnUiThread {
                        binding.tvLogsContent.text = "خطأ في الاتصال بالسيرفر (${conn.responseCode})"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.tvLogsContent.text = "فارغ (تعذر الجلب من السيرفر)"
                }
            }
        }
    }

    private fun checkAndRequestSmsPermission() {
        val receiveSms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
        val readSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)

        if (receiveSms == PackageManager.PERMISSION_GRANTED && readSms == PackageManager.PERMISSION_GRANTED) {
            binding.btnTest.visibility = View.GONE
        } else {
            binding.btnTest.visibility = View.VISIBLE
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                SMS_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                binding.btnTest.visibility = View.GONE
                Toast.makeText(this, "تم منح الصلاحيات بنجاح!", Toast.LENGTH_SHORT).show()
                loadSettings()
            } else {
                binding.btnTest.visibility = View.VISIBLE
            }
        }
    }
}
