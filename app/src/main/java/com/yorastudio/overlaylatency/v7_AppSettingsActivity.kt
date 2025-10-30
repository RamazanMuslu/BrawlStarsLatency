package com.yorastudio.overlaylatency

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebSettings
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColor
import org.w3c.dom.Text
import android.content.Context
import android.media.Image
import android.widget.ImageButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class v7_AppSettingsActivity : AppCompatActivity() {

    private val OVERLAY_PERMISSION_REQUEST_CODE = 101


    // SharedPreferences'ı kolayca erişmek için lazy init ile tanımlıyoruz
    private val sharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", MODE_PRIVATE)
    }

    override fun attachBaseContext(newBase: Context?) {
        // Activity ilk oluşturulurken Context'i kaydedilen dile göre ayarla
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
    }

    // MARK: UI Components
    private lateinit var changeLanguageButton: LinearLayout

    private lateinit var textSizeCurrent: TextView
    private lateinit var textSizeSeekBar: SeekBar

    private lateinit var textColorCurrent: TextView
    private lateinit var textColorRadioGroup: RadioGroup
    private lateinit var latencyIconVisSwitch: androidx.appcompat.widget.SwitchCompat
    private lateinit var latencyTextVisSwitch: androidx.appcompat.widget.SwitchCompat
    private lateinit var latencyCloseVisSwitch: androidx.appcompat.widget.SwitchCompat

    private lateinit var automaticServerSelectionSwitch: androidx.appcompat.widget.SwitchCompat
    private lateinit var selectedServerText: TextView
    private lateinit var changeSelectedServerButton: Button

    private lateinit var pingIntervalDelayCurrent: TextView
    private lateinit var pingIntervalDelaySeekBar: SeekBar

    private lateinit var checkForUpdateButton: LinearLayout
    private lateinit var aboutUsButton: LinearLayout
    private lateinit var contributorsButton: LinearLayout

    private lateinit var appVersion: TextView
    private lateinit var versionUpdate: TextView
    private lateinit var developerOptionsArea: LinearLayout

    // MARK: VARS
    private lateinit var serverList: List<Servers>
    private var SelectedServerURL: String? = ""
    private var TextSize: Float = 16F
    private var LatencyInterval: Long = 1000
    private var TextColor: Int = Color.WHITE
    private var LatencyIconVis: Int = 1
    private var LatencyTextVis: Int = 1
    private var LatencyCloseVis: Int = 1
    private var AutomaticServerSelection: Int = 0
    private var AppVersionInformation: AppVersionInfo = AppVersionInfo(0, "Bilinmiyor")
    // private var SelectedServerInformation:
    private var DeveloperOptions: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_version7)

        var appVersionClicks: Int = 0

        serverList = loadServersFromAssets(this)

        changeLanguageButton = findViewById(R.id.btn_settings_change_language)

        textSizeCurrent = findViewById(R.id.setting_text_size_value)
        textSizeSeekBar = findViewById(R.id.setting_text_size_slider)

        textColorRadioGroup = findViewById(R.id.setting_texts_color_group)
        textColorCurrent = findViewById(R.id.setting_texts_color_current)

        latencyIconVisSwitch = findViewById(R.id.setting_delay_icon_visibility)
        latencyTextVisSwitch = findViewById(R.id.setting_delay_text_visibility)
        latencyCloseVisSwitch = findViewById(R.id.setting_close_button_visibility)

        // automaticServerSelectionSwitch = findViewById(R.id.setting_auto_server)

        selectedServerText = findViewById(R.id.setting_selected_server_name)
        changeSelectedServerButton = findViewById(R.id.btn_change_server)

        pingIntervalDelaySeekBar = findViewById(R.id.setting_delay_slider)
        pingIntervalDelayCurrent = findViewById(R.id.setting_delay_value)

        checkForUpdateButton = findViewById(R.id.setting_check_update)
        aboutUsButton = findViewById(R.id.setting_about)
        contributorsButton = findViewById(R.id.setting_supporters)

        appVersion = findViewById(R.id.info_version)
        versionUpdate = findViewById(R.id.info_last_update)

        developerOptionsArea = findViewById(R.id.settings_developer_options_button)

        val btnBack: ImageView = findViewById(R.id.btn_back)

        loadAndApplySettings()

        btnBack.setOnClickListener {
            finish()
        }


        changeLanguageButton.setOnClickListener {
            val intent = Intent(this, v7_ChangeLanguage::class.java)
            startActivity(intent)
        }

        changeSelectedServerButton.setOnClickListener {
            val intent = Intent(this, v7_ChangeServer::class.java)
            startActivity(intent)
        }

        checkForUpdateButton.setOnClickListener {
            val intent = Intent(this, v7_VersionCheck::class.java)
            startActivity(intent)
        }

        aboutUsButton.setOnClickListener {
            val intent = Intent(this, v7_AboutApp::class.java)
            startActivity(intent)
        }

        contributorsButton.setOnClickListener {
            val intent = Intent(this, v7_ContributorsMenu::class.java)
            startActivity(intent)
        }

        developerOptionsArea.setOnClickListener {
            //
        }

        textColorRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radios_color_white -> {
                    TextColor = Color.WHITE
                    saveSettings()
                }

                R.id.radios_color_green -> {
                    TextColor = Color.GREEN
                    saveSettings()
                }

                else -> {
                    TextColor = Color.WHITE
                    saveSettings()
                }
            }
        }

        latencyIconVisSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            LatencyIconVis = if(isChecked) 1 else 0
            saveSettings()
        }

        latencyTextVisSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            LatencyTextVis = if (isChecked) 1 else 0
            saveSettings()
        }

        latencyCloseVisSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            LatencyCloseVis = if (isChecked) 1 else 0
            saveSettings()
        }

       /*  automaticServerSelectionSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            // isChecked: Switch'in yeni durumu (true: Açık / false: Kapalı)

            if (isChecked) {
                // Switch AÇIK duruma geldi, siber güvenlik sitesini mi açıyorsun, ne yapıyorsun? :)
                println("Switch ON oldu, amk!")
            } else {
                // Switch KAPALI duruma geldi.
                println("Switch OFF oldu, aq!")
            }
        } */

        textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                TextSize = if (progress > 0) progress.toFloat() else 10F
                saveSettings()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        pingIntervalDelaySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser)
                {
                    if (progress >= 500 && progress <= 2000)
                    {
                        LatencyInterval = progress.toLong()
                        saveSettings()
                    }
                    else
                    {
                        if (progress < 500)
                        {
                            LatencyInterval = 500L
                            saveSettings()
                        }
                        else if (progress > 2000)
                        {
                            LatencyInterval = 2000L
                            saveSettings()
                        }
                    }
                }
                else
                {
                    LatencyInterval = progress.toLong()
                    saveSettings()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }


    // Kaydedilmiş ayarları yükle ve UI'a uygula
    private fun loadAndApplySettings() {
        TextSize = sharedPreferences.getFloat(OverlayService.KEY_TEXT_SIZE, 16F)
        LatencyInterval = sharedPreferences.getLong(OverlayService.KEY_REFRESHING_TIME, 1000L)
        SelectedServerURL = sharedPreferences.getString(OverlayService.KEY_FEED_URL,  "https://dynamodb.eu-central-2.amazonaws.com")
        TextColor = sharedPreferences.getInt(OverlayService.KEY_TEXT_COLOR, Color.WHITE)
        LatencyIconVis = sharedPreferences.getInt(OverlayService.KEY_LATENCY_IMAGE_VISIBILITY, 1)
        LatencyTextVis = sharedPreferences.getInt(OverlayService.KEY_LATENCY_TEXT_VISIBILITY, 1)
        LatencyCloseVis = sharedPreferences.getInt(OverlayService.KEY_LATENCY_CLOSE_VISIBILITY, 1)
        AutomaticServerSelection = 0
        AppVersionInformation = getAppVersionDetails(this)

        textSizeCurrent.text = getString(R.string.v7_settings_personalization_text_size_current) + " " + TextSize.toString()
        textSizeSeekBar.progress = TextSize.toInt()

        pingIntervalDelayCurrent.text = LatencyInterval.toString() + " " + getString(R.string.v7_settings_network_and_performance_refreshing_time_current)
        pingIntervalDelaySeekBar.progress = LatencyInterval.toInt()

        if (TextColor == Color.WHITE)
        {
            textColorCurrent.text = getString(R.string.v7_settings_personalization_text_color_current) + " " + getString(R.string.v7_color_white)
            textColorRadioGroup.check(R.id.radios_color_white)
        }
        else if (TextColor == Color.GREEN)
        {
            textColorCurrent.text = getString(R.string.v7_settings_personalization_text_color_current) + " " + getString(R.string.v7_color_green)
            textColorRadioGroup.check(R.id.radios_color_green)
        }
        else
        {
            textColorCurrent.text = getString(R.string.v7_settings_personalization_text_color_current) + " " + getString(R.string.v7_color_white)
            textColorRadioGroup.check(R.id.radios_color_white)
        }

        latencyIconVisSwitch.isChecked = LatencyIconVis == 1
        latencyTextVisSwitch.isChecked = LatencyTextVis == 1
        latencyCloseVisSwitch.isChecked = LatencyCloseVis == 1

        appVersion.text = AppVersionInformation.versionName
        versionUpdate.text = AppVersionInformation.versionCode.toString()

        selectedServerText.text = findNameByUrl(SelectedServerURL.toString())
    }



    // Ayarları SharedPreferences'a kaydet ve servisi güncelle
    private fun saveSettings(restartService: Boolean = true) {


        sharedPreferences.edit().apply {
            putFloat(OverlayService.KEY_TEXT_SIZE, TextSize)
            putInt(OverlayService.KEY_TEXT_COLOR, TextColor)
            putString(OverlayService.KEY_FEED_URL, SelectedServerURL)
            putInt(OverlayService.KEY_LATENCY_TEXT_VISIBILITY, LatencyTextVis)
            putLong(OverlayService.KEY_REFRESHING_TIME, LatencyInterval)
            putInt(OverlayService.KEY_LATENCY_IMAGE_VISIBILITY, LatencyIconVis)
            putInt(OverlayService.KEY_LATENCY_CLOSE_VISIBILITY, LatencyCloseVis)
            putInt(OverlayService.KEY_LATENCY_ON_MAIN_MENU_VISIBILITY, 0)
            apply()
        }
        loadAndApplySettings()


        Toast.makeText(this, getString(R.string.toast_settings_saved), Toast.LENGTH_SHORT).show()

        if (restartService) {
            restartOverlayService()
        }
    }


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
                startService(serviceIntent)
            }
        }
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

    fun loadServersFromAssets(context: Context): List<Servers> {

        // 1. JSON dosyasını assets'ten String olarak oku
        val jsonString: String = try {
            context.assets.open("servers.json")
                .bufferedReader()
                .use { it.readText() }
        } catch (ioException: IOException) {
            // Dosya bulunamazsa veya okuma hatası olursa
            ioException.printStackTrace()
            println("servers.json READ ERROR!")
            return emptyList() // Hata durumunda boş liste dön
        }

        // 2. Gson objesini oluştur
        val gson = Gson()

        // 3. Tip Tanımlaması (TypeToken) - KRİTİK NOKTA!
        // Gson'a dönüştürülecek objenin tek bir "Server" değil, bir "List<Server>" olduğunu söylemek için gereklidir.
        val listServerType = object : TypeToken<List<Servers>>() {}.type

        // 4. Parselleme işlemini yap ve listeyi döndür
        return gson.fromJson(jsonString, listServerType)
    }

    fun findNameByUrl(url: String): String? {
        // Liste üzerinde döner ve URL'si eşleşen ilk Server objesini bulur.
        val foundServer = serverList.find { it.url.equals(url, ignoreCase = true) }

        // Eğer bir sunucu bulunduysa (null değilse), onun 'name' özelliğini döndürür.
        return foundServer?.name
    }

    fun findUrlByName(name: String): String? {
        // Liste üzerinde döner ve adı eşleşen ilk Server objesini bulur.
        val foundServer = serverList.find { it.name.equals(name, ignoreCase = true) }

        // Eğer bir sunucu bulunduysa (null değilse), onun 'url' özelliğini döndürür.
        return foundServer?.url
    }

}