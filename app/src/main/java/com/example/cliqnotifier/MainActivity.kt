package com.example.cliqnotifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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

        // تحميل الإعدادات المحفوظة وتعبئتها بالحقول
        loadSettings()

        // زر حفظ الإعدادات
        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        // زر منح الإذن
        binding.btnTest.setOnClickListener {
            checkAndRequestSmsPermission()
        }

        checkAndRequestSmsPermission()
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
