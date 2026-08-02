package com.example.cliqnotifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
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
        
        // زر إضافة بطاقة جديدة
        binding.btnAddCard.setOnClickListener {
            addNewFilterCard()
        }

        binding.btnTest.setOnClickListener { checkAndRequestSmsPermission() }
        checkAndRequestSmsPermission()

        // إضافة بطاقة أولية افتراضية عند فتح التطبيق
        addNewFilterCard()
    }

    private fun addNewFilterCard() {
        val cardView = LayoutInflater.from(this).inflate(R.layout.item_filter_card, binding.cardsContainerLayout, false)
        
        val spinner = cardView.findViewById<Spinner>(R.id.spinnerBank)
        val btnDelete = cardView.findViewById<Button>(R.id.btnDeleteCard)

        // تعبئة القائمة المنسدلة بأسماء البنوك الشائعة
        val banks = arrayOf("اختر البنك...", "Reflect", "Cairo Amman Bank", "Arab Bank", "Housing Bank", "Capital Bank")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, banks)
        spinner.adapter = adapter

        // زر حذف البطاقة
        btnDelete.setOnClickListener {
            binding.cardsContainerLayout.removeView(cardView)
            Toast.makeText(this, "تم حذف البطاقة", Toast.LENGTH_SHORT).show()
        }

        // إضافة البطاقة للحاوية على الشاشة
        binding.cardsContainerLayout.addView(cardView)
    }

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
