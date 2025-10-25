package com.yorastudio.overlaylatency

// ... (Diğer tüm import'lar ve sınıf dışındaki kodlar aynı kalacak) ...
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.yorastudio.overlaylatency.AppVersionInfo
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.*
import android.content.pm.PackageManager

class MainActivity : AppCompatActivity() {

    // Bu, overlay izni istediğimizde kullanacağımız özel bir kod. Kafana takma, standart bir numara.
    private val OVERLAY_PERMISSION_REQUEST_CODE = 101
    private lateinit var btnToggleOverlay: Button
    private lateinit var tvMainPingText: TextView
    private lateinit var ivFlasgs: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    // Zamanlanmış görevin referansı, durdurmak için kullanacağız
    private var feedUpdateFuture: ScheduledFuture<*>? = null

    private val sharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", Context.MODE_PRIVATE)
    }

    // Bu fonksiyonu diğer dosyadan (örneğin LocaleHelper) alıp buraya eklemen gerekecek
    private fun FlagList(language: String): Int {
        return when (language) {
            "en" -> R.mipmap.ic_flags_usa_foreground
            "tr" -> R.mipmap.ic_flags_turkiye_foreground
            "de" -> R.mipmap.ic_flags_germany_foreground
            "fr" -> R.mipmap.ic_flags_french_foreground
            "zh" -> R.mipmap.ic_flags_china_foreground
            "ru" -> R.mipmap.ic_flags_russia_foreground
            else -> R.mipmap.ic_flags_usa_foreground
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bu, LocaleHelper'ı kullanıyorsan gerekli
        // attachBaseContext(LocaleHelper.onAttach(this))

        LowestPingFinder()

        btnToggleOverlay = findViewById(R.id.btnToggleOverlay)
        val btnFeedingSection: Button = findViewById(R.id.btnFeedingSection)
        val btnTextSettings: Button = findViewById(R.id.btnTextSettings)
        val btnAboutApp: Button = findViewById(R.id.btnAboutApp)
        val tvMain_BuildName: TextView = findViewById(R.id.tvMain_BuildName)
        val tvMain_BuildCode: TextView = findViewById(R.id.tvMain_BuildCode)
        val btnAppSettings: Button = findViewById(R.id.btnAppSettings)
        val btnCloseApp: Button = findViewById(R.id.btnCloseApp)
        val btnSeeLatencyLogs: Button = findViewById(R.id.btnSeeLatencyLogs)
        val btnAppStats: Button = findViewById(R.id.btnAppStats)
        tvMainPingText = findViewById(R.id.tvMainPingText)
        val btnThePremiumPage: ImageView = findViewById(R.id.btnThePremiumPage)
        ivFlasgs = findViewById(R.id.flags)

        ivFlasgs.setOnClickListener {
            val intent = Intent(this, AppSettingsActivity::class.java)
            startActivity(intent)
        }

        if (OverlayService.isServiceRunning) {
            btnToggleOverlay.text = getString(R.string.main_activate_overlay_toogle_disable)
        } else {
            btnToggleOverlay.text = getString(R.string.main_activate_overlay_toogle_activate)
        }

        val LanguageCode: String = "" // LocaleHelper'dan alacaksın
        ivFlasgs.setImageResource(FlagList(LanguageCode))

        btnThePremiumPage.setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_premium_features_coming_soon), Toast.LENGTH_LONG).show()
        }

        btnSeeLatencyLogs.setOnClickListener {
            val intent = Intent(this, LatencyLogsActivity::class.java)
            startActivity(intent)
        }

        btnAppStats.setOnClickListener {
            val intent = Intent(this, StatsActivity::class.java)
            startActivity(intent)
        }

        btnCloseApp.setOnClickListener {
            finishAffinity()
        }

        btnAppSettings.setOnClickListener {
            val intent = Intent(this, AppSettingsActivity::class.java)
            startActivity(intent)
        }

        val appInfo = getAppVersionDetails(this)

        tvMain_BuildName.text = getString(R.string.main_app_build_name) + " ${appInfo.versionName}"
        tvMain_BuildCode.text = getString(R.string.main_app_build_code) + " ${appInfo.versionCode}"

        btnToggleOverlay.setOnClickListener {
            checkOverlayPermission()
        }

        btnFeedingSection.setOnClickListener {
            val intent = Intent(this, FeedingSectionActivity::class.java)
            startActivity(intent)
        }

        btnTextSettings.setOnClickListener {
            val intent = Intent(this, TextSettingsActivity::class.java)
            startActivity(intent)
        }

        btnAboutApp.setOnClickListener {
            val intent = Intent(this, AboutAppActivity::class.java)
            startActivity(intent)
        }

        handleDeepLink(intent)

        val savedLatencyOnMainMenuVisibility = sharedPreferences.getInt(OverlayService.KEY_LATENCY_ON_MAIN_MENU_VISIBILITY, 1)

        if (savedLatencyOnMainMenuVisibility == 0) {
            tvMainPingText.visibility = TextView.GONE
        } else {
            tvMainPingText.visibility = TextView.VISIBLE
            pingMeterDemo()
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val appLinkAction: String? = intent?.action
        val appLinkData: Uri? = intent?.data

        if (Intent.ACTION_VIEW == appLinkAction && appLinkData != null) {
            if (appLinkData.scheme == "bspingmeter" && appLinkData.host == "server") {
                val serverName = appLinkData.lastPathSegment
                if (!serverName.isNullOrEmpty()) {
                    val feedingIntent = Intent(this, FeedingSectionActivity::class.java).apply {
                        putExtra("SERVER_NAME_FROM_DEEPLINK", serverName)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(feedingIntent)
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.toast_deeplink_invalid_server_name), Toast.LENGTH_SHORT).show()
                }
            } else if (appLinkData.scheme == "bspingmeter" && appLinkData.host == "start" && appLinkData.path == "/overlay/default") {
                DeepLink_StartOverlay_DefaultSettings()
            }
        }
    }

    private fun DeepLink_StartOverlay_DefaultSettings() {
        val serviceIntent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            Toast.makeText(this, getString(R.string.toast_error_permission_overlay), Toast.LENGTH_LONG).show()
        } else {
            if (!OverlayService.isServiceRunning) {
                startService(serviceIntent)
                Toast.makeText(this, getString(R.string.toast_overlay_text_activate), Toast.LENGTH_SHORT).show()
            }
        }
        finishAffinity()
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        } else {
            toggleOverlayService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                toggleOverlayService()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.toast_error_permission_overlay),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun toggleOverlayService() {
        val serviceIntent = Intent(this, OverlayService::class.java)
        val isServiceRunning = OverlayService.isServiceRunning

        if (isServiceRunning) {
            stopService(serviceIntent)
            Toast.makeText(this, getString(R.string.toast_overlay_text_hidden), Toast.LENGTH_SHORT).show()
            btnToggleOverlay.text = getString(R.string.main_activate_overlay_toogle_activate)
        } else {
            startService(serviceIntent)
            Toast.makeText(this, getString(R.string.toast_overlay_text_activate), Toast.LENGTH_SHORT).show()
            btnToggleOverlay.text = getString(R.string.main_activate_overlay_toogle_disable)
        }

        // Bu kısım, overlay durumunu QSTile'a senkronize eder.
        QSTileActivity.requestTileUpdate(this, !isServiceRunning)
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

    private fun pingMeterDemo() {
        val EU_Central2 = "https://dynamodb.eu-central-2.amazonaws.com"
        val feedUrl = sharedPreferences.getString(OverlayService.KEY_FEED_URL, EU_Central2)
        val pingInterval = 1000L

        if (!feedUrl.isNullOrEmpty()) {
            val targetHost: String
            val targetPort: Int

            try {
                val urlObj = URL(feedUrl)
                targetHost = urlObj.host
                targetPort = if (urlObj.port != -1) urlObj.port else if (urlObj.protocol == "https") 443 else 80
            } catch (e: Exception) {
                handler.post { tvMainPingText.text = getString(R.string.overlay_invalid_url) }
                return
            }
            feedUpdateFuture = scheduledExecutorService.scheduleWithFixedDelay({
                if (sharedPreferences.getInt(OverlayService.KEY_LATENCY_ON_MAIN_MENU_VISIBILITY, 1) != 0) {
                    val latency = measureTcpLatency(targetHost.trim(), targetPort)
                    handler.post {
                        if (latency >= 0) {
                            tvMainPingText.text = getString(R.string.overlay_ping_text) + " " + latency
                            saveToDataUsage()
                        } else if (latency == -1L) {
                            tvMainPingText.text = getString(R.string.overlay_ping_error_connection)
                        } else {
                            tvMainPingText.text = getString(R.string.overlay_ping_failed)
                        }
                    }
                }
            }, 0, pingInterval, TimeUnit.MILLISECONDS)
        } else {
            handler.post {
                tvMainPingText.text = getString(R.string.overlay_enter_url_hint)
            }
        }
    }

    private fun saveToDataUsage() {
        var currentUsageData = sharedPreferences.getLong(OverlayService.KEY_USAGE_DATA_MAIN_MENU, 0)
        var currentPingCount = sharedPreferences.getLong(OverlayService.KEY_PING_COUNT_MAIN_MENU, 0)
        var newUsageData = currentUsageData + 60
        var newPingCount = currentPingCount + 1
        sharedPreferences.edit().apply {
            putLong(OverlayService.KEY_USAGE_DATA_MAIN_MENU, newUsageData)
            putLong(OverlayService.KEY_PING_COUNT_MAIN_MENU, newPingCount)
        }.apply()
    }

    private fun measureTcpLatency(host: String, port: Int): Long {
        var latency = -1L
        var socket: Socket? = null
        try {
            val startTime = System.currentTimeMillis()
            socket = Socket(host, port)
            val endTime = System.currentTimeMillis()
            latency = endTime - startTime
        } catch (e: Exception) {
            latency = -1L
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
            }
        }
        return latency
    }

    private fun LowestPingFinder() {
        var currentAddress = sharedPreferences.getString(OverlayService.KEY_FEED_URL, "")
        if (currentAddress == "") {
            lifecycleScope.launch {
                val bestServer = findLowestPingServer()
                sharedPreferences.edit().apply {
                    putString(OverlayService.KEY_FEED_URL, bestServer)
                    apply()
                }
            }
        }
    }

    private suspend fun getPing(url: String): Long = withContext(Dispatchers.IO) {
        var pingTime = -1L
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            pingTime = measureTimeMillis {
                connection.connect()
            }
            connection.disconnect()
        } catch (e: IOException) {
            pingTime = -1L
        }
        pingTime
    }

    suspend fun findLowestPingServer(): String? {
        val servers = mapOf(
            "AP/India" to "https://dynamodb.ap-south-1.amazonaws.com",
            "AP/Singapore" to "https://dynamodb.ap-southeast-1.amazonaws.com",
            "AP/HongKong" to "https://dynamodb.ap-east-1.amazonaws.com",
            "AP/Japan" to "https://dynamodb.ap-northeast-1.amazonaws.com",
            "EU/Italy" to "https://dynamodb.eu-south-1.amazonaws.com",
            "EU/Ireland" to "https://dynamodb.eu-west-1.amazonaws.com",
            "AP/Sydney" to "https://dynamodb.ap-southeast-2.amazonaws.com",
            "EU/Finland" to "https://dynamodb.eu-north-1.amazonaws.com",
            "NA/Virginia" to "https://dynamodb.us-east-1.amazonaws.com",
            "ME/Bahrain" to "https://dynamodb.me-south-1.amazonaws.com",
            "NA/LosAngeles" to "https://dynamodb.us-west-2.amazonaws.com",
            "NA/Oregon" to "https://dynamodb.us-west-2.amazonaws.com",
            "NA/Dallas" to "https://dynamodb.us-east-1.amazonaws.com",
            "NA/Miami" to "https://dynamodb.us-east-1.amazonaws.com",
            "SA/Peru" to "https://dynamodb.us-east-1.amazonaws.com",
            "SA/Chile" to "https://dynamodb.us-east-1.amazonaws.com",
            "SA/Brasil-1" to "https://dynamodb.sa-east-1.amazonaws.com",
            "EU/Germany-1" to "https://dynamodb.eu-central-1.amazonaws.com",
            "EU/Germany-2" to "https://dynamodb.eu-central-2.amazonaws.com"
        )
        var lowestPing = Long.MAX_VALUE
        var bestServerUrl: String? = null
        coroutineScope {
            val deferredPings = servers.map { (name, url) ->
                async {
                    val ping = getPing(url)
                    if (ping != -1L) {
                        println("$name: $ping ms")
                    } else {
                        println("$name: Hata oluştu veya bağlantı kurulamadı.")
                    }
                    Pair(ping, url)
                }
            }
            deferredPings.awaitAll().forEach { (ping, url) ->
                if (ping != -1L && ping < lowestPing) {
                    lowestPing = ping
                    bestServerUrl = url
                }
            }
        }
        return bestServerUrl
    }
}