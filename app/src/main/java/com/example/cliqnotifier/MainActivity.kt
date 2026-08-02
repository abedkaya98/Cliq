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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: AppPreferences
    private val SMS_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)
        loadSettings()

        binding.btnSaveSettings.setOnClickListener { saveSettings() }

        binding.btnAddCard.setOnClickListener {
            addNewFilterCard()
        }

        binding.btnTest.setOnClickListener { checkAndRequestSmsPermission() }
        checkAndRequestSmsPermission()

        // إضافة بطاقة أولية
        addNewFilterCard()
    }

    private fun addNewFilterCard() {
        val cardView = LayoutInflater.from(this).inflate(R.layout.item_filter_card, binding.cardsContainerLayout, false)

        val spinnerBank = cardView.findViewById<Spinner>(R.id.spinnerBank)
        val spinnerSampleSms = cardView.findViewById<Spinner>(R.id.spinnerSampleSms)
        val btnTestMatch = cardView.findViewById<Button>(R.id.btnTestMatch)
        val tvTestResult = cardView.findViewById<TextView>(R.id.tvTestResult)
        val btnDelete = cardView.findViewById<Button>(R.id.btnDeleteCard)

        // تعبئة المرسلين
        val sendersList = getSmsSenders()
        val bankAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sendersList)
        spinnerBank.adapter = bankAdapter

        // عند اختيار مرسل معين، نجلب الرسائل التابعة له
        spinnerBank.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedSender = sendersList[position]
                if (position > 0) {
                    val messages = getMessagesFromSender(selectedSender)
                    val smsAdapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, messages)
                    spinnerSampleSms.adapter = smsAdapter
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // زر التجربة المبدئي
        btnTestMatch.setOnClickListener {
            val selectedMessage = spinnerSampleSms.selectedItem?.toString() ?: ""
            if (selectedMessage.isNotEmpty()) {
                tvTestResult.text = "تم اختيار الرسالة بنجاح:\n$selectedMessage"
                tvTestResult.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            } else {
                tvTestResult.text = "يرجى اختيار مرسل ورسالة أولاً!"
                tvTestResult.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            }
        }

        btnDelete.setOnClickListener {
            binding.cardsContainerLayout.removeView(cardView)
            Toast.makeText(this, "تم حذف البطاقة", Toast.LENGTH_SHORT).show()
        }

        binding.cardsContainerLayout.addView(cardView)
    }

    private fun getSmsSenders(): List<String> {
        val sendersSet = mutableSetOf<String>()
        sendersSet.add("اختر اسم المرسل من القائمة...")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val cursor = contentResolver.query(
                    Uri.parse("content://sms/inbox"),
                    arrayOf("address"),
                    null,
                    null,
                    "date DESC LIMIT 200"
                )

                cursor?.use {
                    val addressIndex = it.getColumnIndex("address")
                    while (it.moveToNext()) {
                        if (addressIndex != -1) {
                            val address = it.getString(addressIndex)
                            if (!address.isNullOrEmpty()) {
                                sendersSet.add(address)
                            }
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
                    "date DESC LIMIT 15"
                )

                cursor?.use {
                    val bodyIndex = it.getColumnIndex("body")
                    while (it.moveToNext()) {
                        if (bodyIndex != -1) {
                            val body = it.getString(bodyIndex)
                            if (!body.isNullOrEmpty()) {
                                messages.add(body)
                            }
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

        Toast.makeText(this, "تم حفظ الإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
    }

    private fun checkAndRequestSmsPermission() {
        val receiveSms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
        val readSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)

        if (receiveSms != PackageManager.PERMISSION_GRANTED || readSms != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                SMS_PERMISSION_CODE
            )
        }
    }
}
