package com.example.cliqnotifier

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
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
        
        val spinner = cardView.findViewById<Spinner>(R.id.spinnerBank)
        val btnDelete = cardView.findViewById<Button>(R.id.btnDeleteCard)

        // جلب أسماء المرسلين الحقيقيين من صندوق الرسائل بالهاتف
        val sendersList = getSmsSenders()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sendersList)
        spinner.adapter = adapter

        btnDelete.setOnClickListener {
            binding.cardsContainerLayout.removeView(cardView)
            Toast.makeText(this, "تم حذف البطاقة", Toast.LENGTH_SHORT).show()
        }

        binding.cardsContainerLayout.addView(cardView)
    }

    // دالة استخراج أحدث المرسلين الفريدين من صندوق الرسائل
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
                            if (!address.isNull_or_empty()) {
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

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()

    private fun loadSettings() {
        binding.etWebhookUrl.setText(prefs.webhookUrl)
        binding.etSecretToken.setText(prefs.secretToken)
        binding.etWalletName.setText(prefs.walletName)
    }

    private fun saveSettings() {
        val url = binding.etWebhookUrl.text.toString().trim()
        val token = binding.etSecretToken.text.toString().trim()
        val wallet = binding.etWalletName.text.toString().trim()

        if (url.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال رابط الـ Webhook", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.webhookUrl = url
        prefs.secretToken = token
        prefs.walletName = wallet

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
