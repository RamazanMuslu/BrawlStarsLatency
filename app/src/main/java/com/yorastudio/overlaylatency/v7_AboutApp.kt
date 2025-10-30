package com.yorastudio.overlaylatency

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class v7_AboutApp : AppCompatActivity() {


    private var selectedColor: Int = Color.WHITE // Başlangıçta seçili renk beyaz
    private lateinit var tvVersionInfo: TextView

    // SharedPreferences'ı kolayca erişmek için lazy init ile tanımlıyoruz
    private val sharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", MODE_PRIVATE)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aboutus_version7) // Kendi layout'umuzu yüklüyoruz

        // XML'deki elemanları kodda buluyoruz
        val btnAboutApp_BackMain: ImageView = findViewById(R.id.btn_back_from_about)
        tvVersionInfo = findViewById(R.id.about_version_info)

        loadAndApplySettings()

        btnAboutApp_BackMain.setOnClickListener{
            finish()
        }

    }

    private fun loadAndApplySettings() {
        tvVersionInfo.text = getString(R.string.v7_about_app_version) + " " + getAppVersionDetails(this).versionName
    }

    fun getAppVersionDetails(context: Context): AppVersionInfo {
        try {
            val packageName = context.packageName
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            val versionCode: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                versionCode = packageInfo.versionCode
            }
            val versionName = packageInfo.versionName.toString()
            return AppVersionInfo(versionCode, versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return AppVersionInfo(0, "Bilinmiyor")
        }
    }
}