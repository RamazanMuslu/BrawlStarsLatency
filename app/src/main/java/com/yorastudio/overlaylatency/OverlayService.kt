package com.yorastudio.overlaylatency

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.ImageView // Resim göstermek için
import java.net.URL
import java.net.Socket // TCP Socket için
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import com.yorastudio.overlaylatency.AppVersionInfo

// YENİ EKLENDİ: Dil ayarı için gerekli import'lar
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import android.view.Menu
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.net.InetAddress
import java.net.NetworkInterface
import okhttp3.OkHttpClient
import okhttp3.Request

class OverlayService : Service() {

    // Ekran yöneticisi, overlay'i ekleyip çıkarmak için kullanacağız
    private var windowManager: WindowManager? = null
    // Overlay olarak göstereceğimiz görünüm (yani overlay_layout.xml'deki her şey)
    private var overlayView: View? = null
    // overlay_layout.xml içindeki TextView'imiz
    private var textView: TextView? = null
    // overlay_layout.xml içindeki ImageView'imiz
    private var imageView: ImageView? = null
    private var btnOverlayClose: TextView? = null
    private var btnOverlayOpenAppSettings: TextView? = null
    private var btnOverlayMenu: ImageView? = null
    private var overlayMenuArea: LinearLayout? = null
    // YENİ TANIMLAMA: overlay_layout.xml içindeki ana LinearLayout
    private var overlayBackground: LinearLayout? = null
    // Pencere yöneticisi için parametreler (konum, boyut, tür vs.)
    private var params: WindowManager.LayoutParams? = null

    // Metni sürüklerken başlangıç konumlarını tutmak için
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    // Ayarları kaydetmek ve okumak için SharedPreferences kullanacağız.
    // Bu sayede uygulama kapansa bile ayarlarımız (metin boyutu, renk, besleme linki) kalıcı olur.
    private val sharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", Context.MODE_PRIVATE)
    }

    // Besleme linkinden veri çekmek için zamanlanmış görevler yöneticisi
    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    // Zamanlanmış görevin referansı, durdurmak için kullanacağız
    private var feedUpdateFuture: ScheduledFuture<*>? = null
    // UI güncelmelerini ana iş parçacığında yapmak için Handler
    private val handler = Handler(Looper.getMainLooper())

    // YENİ EKLENDİ: Dil ayarına göre güncellenmiş kaynaklar objesi
    private lateinit var localeAwareResources: Resources

    private var LastLatency: Long = 999;

    private var MenuVisibility: Boolean = false

    // Servisin çalışıp çalışmadığını dışarıdan kontrol etmek için statik değişken
    companion object {
        var isServiceRunning = false

        // Ayar anahtarları (SharedPreferences için kullanacağız)
        const val KEY_FEED_URL = "feed_url"
        const val KEY_TEXT_SIZE = "text_size"
        const val KEY_TEXT_COLOR = "text_color"
        const val KEY_FONT_FAMILY = "font_family" // İleride font ayarı eklersek kullanırız
        const val KEY_OVERLAY_X = "overlay_x" // Overlay'in son x konumu
        const val KEY_OVERLAY_Y = "overlay_y" // Overlay'in son y konumu
        // const val KEY_PING_INTERVAL = "ping_interval" // Ping aralığını belirlemek için
        // KALDIRILDI: KEY_REFRESHING_TIME, KEY_PING_INTERVAL ile aynı işi görüyor
        const val KEY_REFRESHING_TIME = "refreshing_time"
        // YENİ EKLENDİ: Uygulama dili için anahtar
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_LATENCY_IMAGE_VISIBILITY = "latency_image_visibility"
        const val KEY_LATENCY_TEXT_VISIBILITY = "latency_text_visibility"
        const val KEY_LATENCY_CLOSE_VISIBILITY = "latency_close_visibility"
        const val KEY_LATENCY_ON_MAIN_MENU_VISIBILITY = "latency_on_main_menu_visibility"
        const val KEY_LATENCY_HIGH = "latency_high"
        const val KEY_LATENCY_LOW = "latency_low"
        const val KEY_LATENCY_LOG = "latency_log_upgraded"
        const val KEY_LATENCY_LOG_OLD = "latency_log"
        const val KEY_USAGE_TIME = "usage_time"
        const val KEY_USAGE_DATA = "usage_data"
        const val KEY_USAGE_DATA_MAIN_MENU = "usage_data_on_main_menu"
        const val KEY_PING_COUNT = "ping_count"
        const val KEY_PING_COUNT_MAIN_MENU = "ping_count_on_main_menu"
        const val KEY_BACKGROUND_SCALE = "background_scale" // YENİ ANAHTAR!
    }

    // Servis bağlandığında çağrılır, biz kullanmayacağımız için null döndürüyoruz.
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
    }

    // Servis ilk oluşturulduğunda çağrılır
    override fun onCreate() {
        // YENİ EKLENDİ: Dili ayarla ve bu dile uygun bir Context'in kaynaklarını al
        // LocaleHelper'dan kaydedilen dili alıyoruz.
        val lang = LocaleHelper.getLanguage(this)
        val contextWithLocale = LocaleHelper.setLocale(this, lang) // Yeni bir Context oluştur
        localeAwareResources = contextWithLocale.resources // Bu Context'in kaynaklarını kullanacağız

        super.onCreate()

        isServiceRunning = true // Servis çalışıyor olarak işaretle
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // overlay_layout.xml dosyamızı bir View nesnesine dönüştürüyoruz
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        // TextView'imizi ID'siyle buluyoruz
        textView = overlayView?.findViewById(R.id.overlayTextView)
        // ImageView'imizi ID'siyle buluyoruz
        imageView = overlayView?.findViewById(R.id.overlayImageView)
        btnOverlayMenu = overlayView?.findViewById(R.id.btnOverlayMenu)
        overlayMenuArea = overlayView?.findViewById(R.id.overlayMenuArea)
        btnOverlayClose = overlayView?.findViewById(R.id.btnOverlayClose)
        btnOverlayOpenAppSettings = overlayView?.findViewById(R.id.btnOverlayOpenAppSettings)
        // YENİ: overlayBackgorund'u bul ve bağla
        overlayBackground = overlayView?.findViewById(R.id.overlayBackgorund)

        // Kaydedilmiş ayarları (boyut, renk, konum) yüklüyoruz ve uyguluyoruz
        applySavedSettings()

        btnOverlayClose?.setOnClickListener {
            val serviceIntent = Intent(this, OverlayService::class.java)
            stopService(serviceIntent)
            Toast.makeText(this, getString(R.string.toast_overlay_text_hidden), Toast.LENGTH_SHORT).show()

            QSTileActivity.requestTileUpdate(this, false)
        }

        btnOverlayOpenAppSettings?.setOnClickListener {

        }

        btnOverlayMenu?.setOnClickListener {
            if (MenuVisibility) {
                MenuVisibility = false
            }
            else {
                MenuVisibility = true
            }

            ControlMenu()
        }

        // Pencere parametrelerini ayarlıyoruz. Bu kısım önemli!
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android Oreo (8.0) ve üzeri için yeni tip
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            // Eski Android sürümleri için eski tip
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, // Genişlik içeriği kadar
            WindowManager.LayoutParams.WRAP_CONTENT, // Yükseklik içeriği kadar
            layoutFlag, // Pencere türü (overlay olması için)
            // Bu bayraklar önemli:
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or // Dokunmatik olayları diğer uygulamalara gönderir
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // Pencereye odaklanılmaz
            PixelFormat.TRANSLUCENT // Arka planı şeffaf yapar
        ).apply {
            gravity = Gravity.TOP or Gravity.START // Konumlandırma sol üstten başlasın
            // Kaydedilmiş konumları yükle, yoksa varsayılan olarak 100,100 olsun
            x = sharedPreferences.getInt(KEY_OVERLAY_X, 100)
            y = sharedPreferences.getInt(KEY_OVERLAY_Y, 100)
        }

        // Oluşturduğumuz görünümü pencere yöneticisine ekliyoruz, yani ekranda gösteriyoruz!
        windowManager?.addView(overlayView, params)

        // Overlay metnini sürüklemek için dokunmatik olayları dinliyoruz
        overlayView?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Parmağı ekrana ilk koyduğumuzda:
                        // Metnin o anki konumunu kaydet
                        initialX = params!!.x
                        initialY = params!!.y
                        // Parmağın ekrandaki başlangıç konumunu kaydet
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true // Olayı tükettik, başkası işlemesin
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Parmak ekranda hareket ettiğinde:
                        // Metnin yeni X ve Y konumunu hesapla
                        params!!.x = initialX + (event.rawX - initialTouchX).toInt()
                        params!!.y = initialY + (event.rawY - initialTouchY).toInt()
                        // Pencereyi yeni konuma güncelle
                        windowManager?.updateViewLayout(overlayView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        // Parmak ekrandan kalktığında:
                        // Son konumu kaydet ki uygulama kapansa bile hatırlasın
                        sharedPreferences.edit().apply {
                            putInt(KEY_OVERLAY_X, params!!.x)
                            putInt(KEY_OVERLAY_Y, params!!.y)
                            apply() // Değişiklikleri uygula
                        }
                        return true
                    }
                }
                return false
            }
        })

        // Besleme güncelleme işlemini başlat
        startFeedUpdate()
    }

    // Servis her başlatıldığında çağrılır (startService ile)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Servis öldürülürse sistemin onu tekrar başlatmaya çalışmasını sağlarız.
        // Böylece overlay metni hep ekranda kalmaya devam eder.
        return START_STICKY
    }

    private fun ControlMenu() {
        if (MenuVisibility) {
            overlayMenuArea?.visibility = LinearLayout.VISIBLE
        }
        else {
            overlayMenuArea?.visibility = LinearLayout.GONE
        }
    }

    // Servis yok edildiğinde (stopService ile) çağrılır
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false // Servisi durdu olarak işaretle
        // Overlay görünümünü ekrandan kaldır
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
        }
        // Zamanlanmış görevi durdur
        feedUpdateFuture?.cancel(true)
        scheduledExecutorService.shutdown() // Executor'ı kapat
    }

    // Kaydedilmiş ayarları yükle ve TextView'e uygula
    private fun applySavedSettings() {
        // SharedPreferences'tan metin boyutunu oku, yoksa varsayılan 20f olsun
        val textSize = sharedPreferences.getFloat(KEY_TEXT_SIZE, 20f)
        // SharedPreferences'tan metin rengini oku, yoksa varsayılan beyaz olsun
        val textColor = sharedPreferences.getInt(KEY_TEXT_COLOR, Color.WHITE)
        // SharedPreferences'tan besleme URL'sini oku.
        val EUCentral2 = "https://dynamodb.eu-central-2.amazonaws.com"
        val initialUrlFromPrefs = sharedPreferences.getString(KEY_FEED_URL, EUCentral2)
        // Latency Image and Text Visibility
        val latencyImageVisibility = sharedPreferences.getInt(KEY_LATENCY_IMAGE_VISIBILITY, 1)
        val latencyTextVisibility = sharedPreferences.getInt(KEY_LATENCY_TEXT_VISIBILITY, 1)
        val latencyCloseBtnVisibility = sharedPreferences.getInt(KEY_LATENCY_CLOSE_VISIBILITY, 1)

        // YENİ: Arka Plan Ölçeği (Background Scale) ayarını oku. Varsayılan 50%
        val backgroundScale = sharedPreferences.getInt(KEY_BACKGROUND_SCALE, 50)

        // YENİ: Arka Plan Ölçeğini Uygula
        // 0-100 değerini 0.5f - 1.5f aralığına çeviriyoruz.
        val scaleFactor = 0.5f + (backgroundScale.toFloat() / 100f) * 1.0f

        overlayBackground?.scaleX = scaleFactor
        overlayBackground?.scaleY = scaleFactor

        if (latencyCloseBtnVisibility == 0) {
            btnOverlayMenu?.visibility = ImageView.GONE
        }
        else {
            btnOverlayMenu?.visibility = ImageView.VISIBLE
        }

        if (latencyTextVisibility == 0) {
            textView?.visibility = TextView.GONE
        }
        else {
            textView?.visibility = TextView.VISIBLE
        }

        if (latencyImageVisibility == 0) {
            imageView?.visibility = ImageView.GONE
        }
        else {
            imageView?.visibility = ImageView.VISIBLE
        }

        textView?.textSize = textSize // Metin boyutunu ayarla
        textView?.setTextColor(textColor) // Metin rengini ayarla

        // GÜNCELLENDİ: Metni localeAwareResources üzerinden çekiyoruz
        if (initialUrlFromPrefs.isNullOrEmpty()) {
            textView?.text = localeAwareResources.getString(R.string.overlay_enter_url_hint)
        } else {
            textView?.text = getString(R.string.overlay_ping_text) + " " + LastLatency
        }
    }

    // Besleme linkinden veri çekme işlemini başlatan fonksiyon (Ping Aralığı ve Dil Desteği ile)
    private fun startFeedUpdate() {
        val feedUrl = sharedPreferences.getString(KEY_FEED_URL, null)
        // Ping aralığını SharedPreferences'tan oku (varsayılan 500ms)
        val pingInterval = sharedPreferences.getLong(KEY_REFRESHING_TIME, 1000L)

        feedUpdateFuture?.cancel(true) // Mevcut görevi iptal et

        if (!feedUrl.isNullOrEmpty()) {
            val targetHost: String
            val targetPort: Int

            try {
                val urlObj = URL(feedUrl)
                targetHost = urlObj.host
                // Port belirtilmemişse veya HTTPS ise 443, değilse 80 varsayılan portu kullan
                targetPort = if (urlObj.port != -1) urlObj.port else if (urlObj.protocol == "https") 443 else 80
            } catch (e: Exception) {
                // GÜNCELLENDİ: Metni localeAwareResources üzerinden çekiyoruz
                handler.post { textView?.text = localeAwareResources.getString(R.string.overlay_invalid_url) }
                return // Fonksiyondan çık
            }

            feedUpdateFuture = scheduledExecutorService.scheduleWithFixedDelay({
                val latency = measureTcpLatency(targetHost.trim(), targetPort) // TCP Ping ölç

                handler.post {
                    if (latency >= 0) {
                        // GÜNCELLENDİ: Metni localeAwareResources üzerinden çekiyoruz
                        LastLatency = latency
                        textView?.text = getString(R.string.overlay_ping_text) + " " + latency
                        latencyAlgorithm(latency)
                        addLogWithPublicIp(latency)
                        addUsingTime(pingInterval)
                    } else if (latency == -1L) {
                        // GÜNCELLENDİ: Metni localeAwareResources üzerinden çekiyoruz
                        textView?.text = localeAwareResources.getString(R.string.overlay_ping_error_connection)
                        changeLatencyIcon(0)
                    } else {
                        // GÜNCELLENDİ: Metni localeAwareResources üzerinden çekiyoruz
                        textView?.text = localeAwareResources.getString(R.string.overlay_ping_failed)
                        changeLatencyIcon(0)
                    }
                }
            }, 0, pingInterval, TimeUnit.MILLISECONDS) // Ping aralığını kullan

        } else {
            // GÜNCELLENDİ: Metni localeAwareResources üzerinden çekiyoruz
            handler.post {
                textView?.text = localeAwareResources.getString(R.string.overlay_enter_url_hint)
                changeLatencyIcon(0)
            }
        }
    }

    private fun addUsingTime(milliseconds: Long) {
        var currentUsageTime = sharedPreferences.getLong(KEY_USAGE_TIME, 0)
        var currentUsageData = sharedPreferences.getLong(KEY_USAGE_DATA, 0)
        var currentPingCount = sharedPreferences.getLong(KEY_PING_COUNT, 0)

        var newUsageTime = currentUsageTime + milliseconds
        var newUsageData = currentUsageData + 60
        var newPingCount = currentPingCount + 1

        sharedPreferences.edit().apply {
            putLong(KEY_USAGE_DATA, newUsageData)
            putLong(KEY_USAGE_TIME, newUsageTime)
            putLong(KEY_PING_COUNT, newPingCount)
        }.apply()
    }

    private fun latencyAlgorithm(latency: Long) {
        if (latency <= 60) {
            changeLatencyIcon(5)
        }
        else if (latency <= 100) {
            changeLatencyIcon(4)
        }
        else if (latency <= 150) {
            changeLatencyIcon(3)
        }
        else if (latency <= 200) {
            changeLatencyIcon(2)
        }
        else if (latency >= 201) {
            changeLatencyIcon(1)
        }
        else {
            changeLatencyIcon(0)
        }
    }

    private fun changeLatencyIcon(status: Int) {

        val resourceID: Int

        if (status == 0) {
            resourceID = R.mipmap.ic_latency_icon_0_error_foreground
        }
        else if (status == 1) {
            resourceID = R.mipmap.ic_latency_icon_1_red_foreground
        }
        else if (status == 2) {
            resourceID = R.mipmap.ic_latency_icon_2_yellow_foreground
        }
        else if (status == 3) {
            resourceID = R.mipmap.ic_latency_icon_3_yellow_foreground
        }
        else if (status == 4) {
            resourceID = R.mipmap.ic_latency_icon_4_green_foreground
        }
        else if (status == 5) {
            resourceID = R.mipmap.ic_latency_icon_5_green_foreground
        }
        else {
            resourceID = R.mipmap.ic_latency_icon_0_error_foreground
        }

        imageView?.setImageResource(resourceID)
    }

    // TCP Socket Ping ölçümü yapar
    private fun measureTcpLatency(host: String, port: Int): Long {
        var latency = -1L // Varsayılan hata değeri
        var socket: Socket? = null // Socket nesnesi

        try {
            val startTime = System.currentTimeMillis() // Bağlantı kurmadan önceki zamanı kaydet
            socket = Socket(host, port) // Host ve porta bağlantı kurmayı dene
            val endTime = System.currentTimeMillis()   // Bağlantı kurulduktan sonraki zamanı kaydet

            latency = endTime - startTime // Gecikmeyi hesapla

        } catch (e: Exception) {
            // Bağlantı hatası (host bulunamadı, port kapalı, zaman aşımı vb.)
            latency = -1L // Genel hata durumunu belirtmek için
        } finally {
            try {
                socket?.close() // Socket'i kapatmayı unutma!
            } catch (e: Exception) {
                // Kapatma hatası
            }
        }
        return latency
    }

    // YENİ EKLENDİ: Belirli bir dil koduyla yeni bir Context oluşturan fonksiyon
    // Bu fonksiyon, LocaleHelper'daki setLocale ile aynı mantığı taşır.
    private fun createLocaleContext(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return context.createConfigurationContext(config)
    }

    fun addLogWithPublicIp(latency: Long) {
        getPublicIpAdress { ip ->
            val ipAddress = ip ?: "IP_DONT_GET_ERROR"

            // Burada UI thread ise sharedPreferences.edit() vs direkt kullanılabilir.
            // Eğer UI thread dışında ise handler veya coroutine ile geçiş gerekebilir.
            addLog(latency, ipAddress)
        }
    }

    private fun addLog(latency: Long, ip: String) {
        val sharedPrefEditor = sharedPreferences.edit()

        var highPing = sharedPreferences.getLong(KEY_LATENCY_HIGH, Long.MIN_VALUE)
        var lowPing = sharedPreferences.getLong(KEY_LATENCY_LOW, Long.MAX_VALUE)
        var pingTitle = "normal"

        if (latency > highPing) {
            highPing = latency
            pingTitle = "highest"
            sharedPrefEditor.putLong(KEY_LATENCY_HIGH, highPing)
        }

        if (latency < lowPing) {
            lowPing = latency
            pingTitle = "lowest"
            sharedPrefEditor.putLong(KEY_LATENCY_LOW, lowPing)
        }

        val jsonString = sharedPreferences.getString(KEY_LATENCY_LOG, null)
        val jsonArray = if (jsonString != null) JSONArray(jsonString) else JSONArray()

        val currentDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        jsonArray.put(LogRecord(latency, currentDateTime, ip, pingTitle))

        sharedPrefEditor.putString(KEY_LATENCY_LOG, jsonArray.toString())
        sharedPrefEditor.apply()
    }

    private fun LogRecord(latency: Long, date: String, ip: String, pingTitle: String): JSONObject {
        return JSONObject().apply {
            put("Latency", latency)
            put("Date", date)
            put("IP", ip)
            put("PingTitle", pingTitle)
        }
    }

    fun getPublicIpAdress(callback: (String?) -> Unit) {
        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url("https://api64.ipify.org?format=text").build()
                val response = client.newCall(request).execute()
                val ip = response.body?.string()
                callback(ip)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is InetAddress) {
                        val ip = addr.hostAddress
                        if (ip.contains(".")) return ip // IPv4
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}