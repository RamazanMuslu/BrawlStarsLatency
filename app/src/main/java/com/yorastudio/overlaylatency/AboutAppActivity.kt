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

class AboutAppActivity : AppCompatActivity() {


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
        setContentView(R.layout.activity_about_app) // Kendi layout'umuzu yüklüyoruz

        // XML'deki elemanları kodda buluyoruz
        val btnAboutApp_BackMain: ImageView = findViewById(R.id.btnAboutApp_BackMain)

        // Mevcut kaydedilmiş ayarları yükle ve UI'a uygula
        //loadAndApplySettings()

        btnAboutApp_BackMain.setOnClickListener{
            finish()
        }

    }

    // Kaydedilmiş ayarları yükle ve UI'a uygula
    private fun loadAndApplySettings() {
        // Load
    }
}