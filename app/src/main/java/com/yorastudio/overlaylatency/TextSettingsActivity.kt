package com.yorastudio.overlaylatency

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TextSettingsActivity : AppCompatActivity() {

    private val OVERLAY_PERMISSION_REQUEST_CODE = 101

    // Yeni: Arka Plan Boyutu elemanları
    private lateinit var seekBarBackgroundSize: SeekBar
    private lateinit var tvCurrentBackgroundSize: TextView

    private lateinit var seekBarTextSize: SeekBar
    private lateinit var tvCurrentTextSize: TextView
    private lateinit var tvCurrentTextColor: TextView
    private lateinit var btnSaveTextSettings: Button // Butonu aktif hale getireceğiz
    private lateinit var tvCurrentLatencyImage_Visibility: TextView
    private lateinit var tvCurrentLatencyText_Visibility: TextView
    private lateinit var tvCurrentLatencyCloseBtn_Visibility: TextView
    private lateinit var tvLatencyOnMainMenu_VisibilityCurrent: TextView

    private var latencyImageVisibility: Int = 1
    private var latencyTextVisibility: Int = 1
    private var latencyCloseBtnVisibility: Int = 1
    private var latencyOnMainMenuVisibility: Int = 1
    private var backgroundScale: Int = 50 // Yeni: Başlangıç arka plan ölçeği (0-100)

    private var selectedColor: Int = Color.WHITE // Başlangıçta seçili renk beyaz

    // SharedPreferences'ı kolayca erişmek için lazy init ile tanımlıyoruz
    private val sharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", Context.MODE_PRIVATE)
    }

    override fun attachBaseContext(newBase: Context?) {
        // LocaleHelper'ı kullanıyorsan, bu satır kalsın
        // super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_settings)

        // XML'deki elemanları kodda buluyoruz
        seekBarTextSize = findViewById(R.id.seekBarTextSize)
        tvCurrentTextSize = findViewById(R.id.tvCurrentTextSize)
        tvCurrentTextColor = findViewById(R.id.tvCurrentTextColor)
        btnSaveTextSettings = findViewById(R.id.btnSaveTextSettings)

        // Yeni elemanları bul
        seekBarBackgroundSize = findViewById(R.id.seekBarBackgroundSize)
        tvCurrentBackgroundSize = findViewById(R.id.tvCurrentBackgroundSize)

        val btnColorGreen: Button = findViewById(R.id.btnColorGreen)
        val btnColorWhite: Button = findViewById(R.id.btnColorWhite)

        val btnTextSettings_BackMain: ImageView = findViewById(R.id.btnTextSettings_BackMain)

        tvCurrentLatencyImage_Visibility = findViewById(R.id.tvCurrentLatencyImage_Visibility)
        tvCurrentLatencyText_Visibility = findViewById(R.id.tvCurrentLatencyText_Visibility)
        tvCurrentLatencyCloseBtn_Visibility = findViewById(R.id.tvCurrentLatencyCloseBtn_Visibility)
        tvLatencyOnMainMenu_VisibilityCurrent = findViewById(R.id.tvLatencyOnMainMenu_VisibilityCurrent)

        val btnLatencyImage_VisibilityTrue: Button = findViewById(R.id.btnLatencyImage_VisibilityTrue)
        val btnLatencyImage_VisibilityFalse: Button = findViewById(R.id.btnLatencyImage_VisibilityFalse)
        val btnLatencyText_VisibilityTrue: Button = findViewById(R.id.btnLatencyText_VisibilityTrue)
        val btnLatencyText_VisibilityFalse: Button = findViewById(R.id.btnLatencyText_VisibilityFalse)
        val btnLatencyCloseBtn_VisibilityTrue: Button = findViewById(R.id.btnLatencyCloseBtn_VisibilityTrue)
        val btnLatencyCloseBtn_VisibilityFalse: Button = findViewById(R.id.btnLatencyCloseBtn_VisibilityFalse)
        val btnLatencyOnMainMenu_VisibilityTrue: Button = findViewById(R.id.btnLatencyOnMainMenu_VisibilityTrue)
        val btnLatencyOnMainMenu_VisibilityFalse: Button = findViewById(R.id.btnLatencyOnMainMenu_VisibilityFalse)


        // Mevcut kaydedilmiş ayarları yükle ve UI'a uygula
        loadAndApplySettings()

        // --- Listener'lar ---

        btnLatencyOnMainMenu_VisibilityTrue.setOnClickListener {
            latencyOnMainMenuVisibility = 1
            saveSettings(false)
        }

        btnLatencyOnMainMenu_VisibilityFalse.setOnClickListener {
            latencyOnMainMenuVisibility = 0
            saveSettings(false)
        }

        btnLatencyCloseBtn_VisibilityTrue.setOnClickListener {
            latencyCloseBtnVisibility = 1
            saveSettings()
        }

        btnLatencyCloseBtn_VisibilityFalse.setOnClickListener {
            latencyCloseBtnVisibility = 0
            saveSettings()
        }

        btnLatencyImage_VisibilityFalse.setOnClickListener {
            latencyImageVisibility = 0
            saveSettings()
        }

        btnLatencyImage_VisibilityTrue.setOnClickListener {
            latencyImageVisibility = 1
            saveSettings()
        }

        btnLatencyText_VisibilityFalse.setOnClickListener {
            latencyTextVisibility = 0
            saveSettings()
        }

        btnLatencyText_VisibilityTrue.setOnClickListener {
            latencyTextVisibility = 1
            saveSettings()
        }

        btnTextSettings_BackMain.setOnClickListener{
            finish() // MainActivity'e geri dönmek için bitir
            // val intent = Intent(this, MainActivity::class.java)
            // startActivity(intent)
        }

        // Metin Boyutu SeekBar listener'ı
        seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvCurrentTextSize.text = getString(R.string.text_options_text_size_current_size) + " ${progress}sp"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveSettings()
            }
        })

        // YENİ: Arka Plan Boyutu SeekBar listener'ı
        seekBarBackgroundSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                backgroundScale = progress
                tvCurrentBackgroundSize.text = getString(R.string.text_options_background_size_current_scale) + " ${backgroundScale}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveSettings()
            }
        })

        // Renk düğmelerine tıklama olayları
        btnColorWhite.setOnClickListener { selectColor(Color.WHITE, getString(R.string.text_options_text_color_white)) }
        btnColorGreen.setOnClickListener { selectColor(Color.GREEN, getString(R.string.text_options_text_color_green)) }

        // Kaydet butonu sadece emin olmak için kalsın, diğerleri zaten saveSettings() çağırıyor.
        btnSaveTextSettings.setOnClickListener {
            saveSettings()
        }
    }

    // --- Fonksiyonlar ---

    // Kaydedilmiş ayarları yükle ve UI'a uygula
    private fun loadAndApplySettings() {
        // Yeni: Arka Plan Boyutunu yükle
        backgroundScale = sharedPreferences.getInt(OverlayService.KEY_BACKGROUND_SCALE, 50)
        seekBarBackgroundSize.progress = backgroundScale
        tvCurrentBackgroundSize.text = getString(R.string.text_options_background_size_current_scale) + " ${backgroundScale}%"

        // Eski ayarları yükle
        val savedTextSize = sharedPreferences.getFloat(OverlayService.KEY_TEXT_SIZE, 20f)
        val savedTextColor = sharedPreferences.getInt(OverlayService.KEY_TEXT_COLOR, Color.GREEN)
        val savedLatencyImageVisibility = sharedPreferences.getInt(OverlayService.KEY_LATENCY_IMAGE_VISIBILITY, 1)
        val savedLatencyTextVisibility = sharedPreferences.getInt(OverlayService.KEY_LATENCY_TEXT_VISIBILITY, 1)
        val savedLatencyCloseBtnVisibility = sharedPreferences.getInt(OverlayService.KEY_LATENCY_CLOSE_VISIBILITY, 1)
        val savedLatencyOnMainMenuVisibility = sharedPreferences.getInt(OverlayService.KEY_LATENCY_ON_MAIN_MENU_VISIBILITY, 1)

        seekBarTextSize.progress = savedTextSize.toInt()
        tvCurrentTextSize.text = getString(R.string.text_options_text_size_current_size) + " ${savedTextSize.toInt()}sp"

        // Görünürlük ayarlarını güncelle
        updateVisibilityUI(savedLatencyOnMainMenuVisibility, tvLatencyOnMainMenu_VisibilityCurrent, R.string.text_options_main_menu_ping_meter_visibility_current)
        updateVisibilityUI(savedLatencyCloseBtnVisibility, tvCurrentLatencyCloseBtn_Visibility, R.string.text_option_latency_close_visibility_current)
        updateVisibilityUI(savedLatencyImageVisibility, tvCurrentLatencyImage_Visibility, R.string.text_option_latency_image_visibility_current)
        updateVisibilityUI(savedLatencyTextVisibility, tvCurrentLatencyText_Visibility, R.string.text_option_latency_text_visibility_current)

        latencyTextVisibility = savedLatencyTextVisibility
        latencyImageVisibility = savedLatencyImageVisibility

        // Kaydedilmiş renge göre tvCurrentTextColor'ı güncelle
        selectedColor = savedTextColor
        tvCurrentTextColor.text = getString(R.string.text_options_text_color_current) + " " + getColorName(savedTextColor)
        tvCurrentTextColor.setTextColor(selectedColor)
    }

    // Görünürlük ayarlarını güncelleyen yardımcı fonksiyon
    private fun updateVisibilityUI(visibilityValue: Int, textView: TextView, currentStringRes: Int) {
        val visibilityStatusRes = if (visibilityValue == 0) R.string.text_option_visibility_types_hide else R.string.text_option_visibility_types_show
        textView.text = getString(currentStringRes) + " " + getString(visibilityStatusRes)
    }

    // Renk seçildiğinde çağrılan fonksiyon
    private fun selectColor(color: Int, colorName: String) {
        selectedColor = color
        tvCurrentTextColor.text = getString(R.string.text_options_text_color_current) + " " + colorName
        tvCurrentTextColor.setTextColor(selectedColor)
        saveSettings()
    }

    // Ayarları SharedPreferences'a kaydet ve servisi güncelle
    private fun saveSettings(restartService: Boolean = true) {
        val textSize = seekBarTextSize.progress.toFloat()

        sharedPreferences.edit().apply {
            putFloat(OverlayService.KEY_TEXT_SIZE, textSize)
            putInt(OverlayService.KEY_TEXT_COLOR, selectedColor)
            putInt(OverlayService.KEY_LATENCY_TEXT_VISIBILITY, latencyTextVisibility)
            putInt(OverlayService.KEY_LATENCY_IMAGE_VISIBILITY, latencyImageVisibility)
            putInt(OverlayService.KEY_LATENCY_CLOSE_VISIBILITY, latencyCloseBtnVisibility)
            putInt(OverlayService.KEY_LATENCY_ON_MAIN_MENU_VISIBILITY, latencyOnMainMenuVisibility)
            putInt(OverlayService.KEY_BACKGROUND_SCALE, backgroundScale) // Yeni: Arka Plan Ölçeğini kaydet
            apply()
        }

        Toast.makeText(this, getString(R.string.toast_settings_saved), Toast.LENGTH_SHORT).show()

        if (restartService) {
            restartOverlayService()
        }
    }

    // ... (restartOverlayService ve getColorName fonksiyonları aynı kalabilir)

    private fun restartOverlayService() {
        val serviceIntent = Intent(this, OverlayService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
        else {
            if (OverlayService.isServiceRunning) {
                stopService(serviceIntent)
            }
            startService(serviceIntent)
        }
    }

    private fun getColorName(color: Int): String {
        return when (color) {
            Color.WHITE -> getString(R.string.text_options_text_color_white) // Buradaki string anahtarlarını R.string. ile değiştirdim
            Color.BLACK -> getString(R.string.text_options_text_color_black) // Varsayımsal string
            Color.RED -> getString(R.string.text_options_text_color_red) // Varsayımsal string
            Color.BLUE -> getString(R.string.text_options_text_color_blue) // Varsayımsal string
            Color.GREEN -> getString(R.string.text_options_text_color_green)
            else -> getString(R.string.color_unknown)
        }
    }
}