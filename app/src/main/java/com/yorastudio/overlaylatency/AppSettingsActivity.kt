package com.yorastudio.overlaylatency

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AppSettingsActivity : AppCompatActivity() {


    private var selectedColor: Int = Color.WHITE // Başlangıçta seçili renk beyaz

    // SharedPreferences'ı kolayca erişmek için lazy init ile tanımlıyoruz
    private val sharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", Context.MODE_PRIVATE)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings) // Kendi layout'umuzu yüklüyoruz

        // XML'deki elemanları kodda buluyoruz
        val btnAppSettings_BackMain: ImageView = findViewById(R.id.btnAppSettings_BackMain)
        val btnLanguageTR: Button = findViewById(R.id.btnLanguageTR)
        val btnLanguageEN: Button = findViewById(R.id.btnLanguageEN)
        val btnLanguageDE: Button = findViewById(R.id.btnLanguageDE)
        val btnLanguageFR: Button = findViewById(R.id.btnLanguageFR)
        val btnLanguageZH: Button = findViewById(R.id.btnLanguageZH)
        val btnLanguageRU: Button = findViewById(R.id.btnLanguageRU)

        // Mevcut kaydedilmiş ayarları yükle ve UI'a uygula
        //loadAndApplySettings()

        //setNewLocale(sharedPreferences.getString(OverlayService.KEY_APP_LANGUAGE, "en").toString(), false)

        btnAppSettings_BackMain.setOnClickListener{
            finish()
        }

        btnLanguageEN.setOnClickListener {
            setNewLocale("en")
        }

        btnLanguageTR.setOnClickListener {
            setNewLocale("tr")
        }

        btnLanguageDE.setOnClickListener {
            setNewLocale("de")
        }

        btnLanguageFR.setOnClickListener {
            setNewLocale("fr")
        }

        btnLanguageZH.setOnClickListener {
            setNewLocale("zh")
        }

        btnLanguageRU.setOnClickListener {
            setNewLocale("ru")
        }
    }

    // AppSettingsActivity sınıfının içine ekle
    private fun setNewLocale(languageCode: String, viewToast: Boolean = true, restartService: Boolean = false) {
        // Seçilen dili LocaleHelper aracılığıyla kaydet
        LocaleHelper.setLanguage(this, languageCode)

        // Kullanıcıya bilgi ver (metin strings.xml'den çekiliyor)
        if (viewToast) {
            Toast.makeText(this, getString(R.string.toast_langauge_changed), Toast.LENGTH_SHORT).show()
        }

        // Uygulamanın dilini güncellemek için ana aktiviteyi yeniden başlat
        // Bu, MainActivity'deki attachBaseContext'i tekrar tetikleyecek ve tüm uygulama yeni dilde açılacak.
        val intent = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Mevcut aktiviteyi kapat

        if (restartService) {
            // Overlay servisini de yeniden başlat ki metinleri yeni dilde çeksin
            restartOverlayService()
        }

        val tvCurrentLanguage: TextView = findViewById(R.id.tvCurrentLanguage)
        tvCurrentLanguage.text = getString(R.string.app_settings_language_current) + " ${languageCode.uppercase()}"
    }

    // AppSettingsActivity sınıfının içine ekle
    private fun restartOverlayService() {
        val serviceIntent = Intent(this, OverlayService::class.java)
        // Eğer servis çalışıyorsa önce durdur, sonra yeniden başlat
        if (OverlayService.isServiceRunning) {
            stopService(serviceIntent)
        }
        // Servisi başlat (bu, onCreate'ini tekrar tetikleyecektir)
        startService(serviceIntent)
    }

    // Kaydedilmiş ayarları yükle ve UI'a uygula
    private fun loadAndApplySettings() {
        // Load
    }
}