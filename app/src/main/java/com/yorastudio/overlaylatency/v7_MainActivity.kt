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
import android.widget.ImageButton
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.plus
import kotlin.apply
import kotlin.compareTo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class v7_MainActivity : AppCompatActivity() {

    // Bu, overlay izni istediğimizde kullanacağımız özel bir kod. Kafana takma, standart bir numara.
    private val OVERLAY_PERMISSION_REQUEST_CODE = 101
    private lateinit var btnToggleOverlay: Button
    private lateinit var tvConnectionInfo: TextView
    private lateinit var tvSelectedServer: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    // Zamanlanmış görevin referansı, durdurmak için kullanacağız
    private var feedUpdateFuture: ScheduledFuture<*>? = null

    private val sharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", MODE_PRIVATE)
    }

    override fun attachBaseContext(newBase: Context?) {
        // Activity ilk oluşturulurken Context'i kaydedilen dile göre ayarla
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
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

    private lateinit var serverList: List<Servers>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_version7)

        checkUpdateInBackground()

        // Bu, LocaleHelper'ı kullanıyorsan gerekli
        // attachBaseContext(LocaleHelper.onAttach(this))

       //  LowestPingFinder()

        serverList = loadServersFromAssets(this)

        btnToggleOverlay = findViewById(R.id.btn_main_start_stop_overlay)
        val btnAppSettings: ImageButton = findViewById(R.id.btn_main_settings)
        val btnStats: ImageButton = findViewById(R.id.btn_main_stats)
        val btnSeeLatencyLogs: Button = findViewById(R.id.btn_main_activity_records)
        tvConnectionInfo = findViewById(R.id.text_connection_status)
        tvSelectedServer = findViewById(R.id.text_server_info)

        tvConnectionInfo.text = if (OverlayService.isServiceRunning) getString(R.string.v7_main_status_working) else getString(R.string.v7_main_status_not_working)
        tvSelectedServer.text = getString(R.string.v7_main_selected_server) + " " + findNameByUrl(sharedPreferences.getString(OverlayService.KEY_FEED_URL, "").toString())


        if (OverlayService.isServiceRunning) {
            btnToggleOverlay.text = getString(R.string.v7_main_stop)
        } else {
            btnToggleOverlay.text = getString(R.string.v7_main_start)
        }

        val LanguageCode: String = "" // LocaleHelper'dan alacaksın

        btnSeeLatencyLogs.setOnClickListener {
            val intent = Intent(this, LatencyLogsActivity::class.java)
            startActivity(intent)
        }

        btnStats.setOnClickListener {
            val intent = Intent(this, StatsActivity::class.java)
            startActivity(intent)
        }

        btnAppSettings.setOnClickListener {
            val intent = Intent(this, v7_AppSettingsActivity::class.java)
            startActivity(intent)
        }

        btnToggleOverlay.setOnClickListener {
            checkOverlayPermission()
        }

        // handleDeepLink(intent)

        /*val savedLatencyOnMainMenuVisibility = sharedPreferences.getInt(OverlayService.KEY_LATENCY_ON_MAIN_MENU_VISIBILITY, 1)

        if (savedLatencyOnMainMenuVisibility == 0) {
            tvMainPingText.visibility = TextView.GONE
        } else {
            tvMainPingText.visibility = TextView.VISIBLE
            pingMeterDemo()
        }*/
    }

    fun checkUpdateInBackground() {
        CoroutineScope(Dispatchers.Main).launch {

            val updateFound = UpdateChecker.isUpdateAvailable(applicationContext)

            // Sonuç ana thread'e döndü.
            if (updateFound)
            {
                val intent = Intent(this@v7_MainActivity, v7_VersionCheck::class.java)
                startActivity(intent)
            }
            else { }
        }
    }

    /*private fun handleDeepLink(intent: Intent?) {
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
*/
   /* private fun DeepLink_StartOverlay_DefaultSettings() {
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
*/
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
                    getString(R.string.v7_toast_error_permission_overlay),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun toggleOverlayService() {
        val serviceIntent = Intent(this, OverlayService::class.java)
        val isServiceRunning = OverlayService.isServiceRunning
        var textStatus = false

        if (isServiceRunning) {
            stopService(serviceIntent)
            btnToggleOverlay.text = getString(R.string.v7_main_start)
            textStatus = false
        } else {
            startService(serviceIntent)
            btnToggleOverlay.text = getString(R.string.v7_main_stop)
            textStatus = true
        }

        tvConnectionInfo.text = if (textStatus) getString(R.string.v7_main_status_working) else getString(R.string.v7_main_status_not_working)

        // Bu kısım, overlay durumunu QSTile'a senkronize eder.
       // QSTileActivity.requestTileUpdate(this, !isServiceRunning)
    }


    /*private fun pingMeterDemo() {
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
                        if (latency compareTo 0) {
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
*/
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
                if (ping != -1L) {
                    ping.compareTo(lowestPing)
                    lowestPing = ping
                    bestServerUrl = url
                }
            }
        }
        return bestServerUrl
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

object UpdateChecker {

    // LOG TAG'i
    private const val TAG = "UpdateChecker"

    /**
     * 🔥 GitHub'a bağlanır, en son versiyon kodunu çeker ve
     * yüklü versiyon kodu ile karşılaştırır.
     *
     * @param context Context objesi (versionCode'u almak için gerekli).
     * @return Yeni bir güncelleme varsa true, yoksa false döner.
     */
    suspend fun isUpdateAvailable(context: Context): Boolean = withContext(Dispatchers.IO) {
        // Uygulamanın getAppVersionDetails fonksiyonunu burada kullanacağız (aşağıda ekli)
        val currentVersionCode = getAppVersionDetails(context).versionCode
        Log.d(TAG, "Current Version Code: $currentVersionCode")

        try {
            // RetrofitClient'ı kullanarak JSON'ı çek
            val remoteInfo = RetrofitClient.rawJsonService.getRemoteVersionInfo()
            val remoteVersionCode = remoteInfo.latestVersionCode

            Log.d(TAG, "Remote Version Code: $remoteVersionCode")

            // Eğer uzak versiyon, yüklü versiyondan büyükse, güncelleme var demektir.
            return@withContext remoteVersionCode > currentVersionCode

        } catch (e: Exception) {
            // Ağ hatası, JSON parse hatası vb.
            //Log.e(TAG, "Güncelleme bilgisi çekilemedi, hata: ${e.message}")
            // Hata durumunda güncelleme var dememek en güvenlisidir.
            return@withContext false
        }
    }

    /**
     * Uygulamanın versiyon detaylarını döndüren yardımcı fonksiyon.
     * (Bu fonksiyonu Activity'den buraya taşımamız veya ortak bir yere koymamız lazım).
     * NOT: getAppVersionDetails fonksiyonunu buraya taşıdım.
     */
    private fun getAppVersionDetails(context: Context): AppVersionInfo {
        // ... Mevcut Activity'deki getAppVersionDetails fonksiyonunun içeriği buraya gelecek
        // (Sana gönderdiğin kodda zaten vardı, onu buraya taşıdık)

        // **Kopyala-Yapıştır Kısmı (V7_VersionCheck sınıfından):**
        try {
            val packageName = context.packageName
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            val versionCode: Int
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                versionCode = packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                versionCode = packageInfo.versionCode
            }
            val versionName = packageInfo.versionName.toString()
            return AppVersionInfo(versionCode, versionName)
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return AppVersionInfo(0, "Unknown")
        }
    }

    // AppVersionInfo Data Class'ı (Ortak bir yere koy)
    data class AppVersionInfo(val versionCode: Int, val versionName: String)
}