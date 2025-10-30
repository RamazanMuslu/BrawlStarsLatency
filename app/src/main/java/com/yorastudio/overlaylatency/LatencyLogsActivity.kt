package com.yorastudio.overlaylatency

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class LatencyLogsActivity : AppCompatActivity() {

    private val sharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", Context.MODE_PRIVATE)
    }

    private lateinit var mainContent: ConstraintLayout
    private lateinit var sideMenu: LinearLayout
    private lateinit var menuButton: ImageView
    private lateinit var closeMenuButton: ImageView
    private lateinit var backButton: ImageView
    private lateinit var overlayView: View

    private lateinit var lowestPing: TextView
    private lateinit var highestPing: TextView
    private lateinit var averagePing: TextView

    private lateinit var ipFiltersContainer: LinearLayout
    private lateinit var recordsContainer: LinearLayout
    private lateinit var deleteAllLogsButton: Button
    private lateinit var btnExtractLogs: Button
    private var currentSelectedIpButton: Button? = null

    // LatencyLogsActivity.kt dosyasını aç ve aşağıdaki override fonksiyonunu ekle

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleHelper.onAttach(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latency_logs)

        initViews()
        setClickListeners()
        RefreshBody()
    }

    private fun initViews() {
        mainContent = findViewById(R.id.main_content)
        sideMenu = findViewById(R.id.side_menu)
        menuButton = findViewById(R.id.menu_button)
        closeMenuButton = findViewById(R.id.close_menu_button)
        backButton = findViewById(R.id.back_button)
        overlayView = findViewById(R.id.overlay_view)

        lowestPing = findViewById(R.id.lowest_ms)
        highestPing = findViewById(R.id.highest_ms)
        averagePing = findViewById(R.id.average_ms)

        ipFiltersContainer = findViewById(R.id.filters_container)
        recordsContainer = findViewById(R.id.records_container)
        deleteAllLogsButton = findViewById(R.id.btn_delete_all_logs)

        btnExtractLogs = findViewById(R.id.btn_extract_logs)
    }

    private fun setClickListeners() {
        menuButton.setOnClickListener {
            toggleMenu(true)
        }

        closeMenuButton.setOnClickListener {
            toggleMenu(false)
        }

        backButton.setOnClickListener {
            finish()
        }

        overlayView.setOnClickListener {
            toggleMenu(false)
        }

        deleteAllLogsButton.setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.remove(OverlayService.KEY_LATENCY_LOG)
            editor.apply()
            RefreshBody()
            Toast.makeText(this, getString(R.string.toast_latency_logs_deleted), Toast.LENGTH_SHORT).show()
            toggleMenu(false)
        }

        btnExtractLogs.setOnClickListener {
            generateAndDownloadJson()
        }
    }

    private fun toggleMenu(open: Boolean) {
        val animation: Animation
        if (open) {
            sideMenu.visibility = View.VISIBLE
            animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_left)
            overlayView.visibility = View.VISIBLE
            overlayView.animate().alpha(0.5f).setDuration(300).start()
        } else {
            animation = AnimationUtils.loadAnimation(this, R.anim.slide_out_to_left)
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    sideMenu.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            overlayView.animate().alpha(0f).setDuration(300).withEndAction {
                overlayView.visibility = View.GONE
            }.start()
        }
        sideMenu.startAnimation(animation)
    }

    private fun RefreshBody(filterIp: String? = null) {
        setUpVars()
        clearViews()
        FindIPs(filterIp)

        val jsonString = sharedPreferences.getString(OverlayService.KEY_LATENCY_LOG, null)
        var indexer = 0

        if (jsonString != null) {
            val jsonArray = JSONArray(jsonString)
            for (i in jsonArray.length() - 1 downTo 0) {
                val obj = jsonArray.getJSONObject(i)
                val latency = obj.getLong("Latency")
                val date = obj.getString("Date")
                val ip = obj.getString("IP")

                if (filterIp == null || ip == filterIp) {
                    indexer++
                    if (indexer <= 100) {
                        addLatencyLogView(latency, date, ip, indexer)
                    }
                }
            }
        }
    }

    private fun clearViews() {
        ipFiltersContainer.removeAllViews()
        recordsContainer.removeAllViews()
    }

    private fun FindIPs(selectedIp: String? = null) {
        val uniqueIps = mutableSetOf<String>()
        val jsonString = sharedPreferences.getString(OverlayService.KEY_LATENCY_LOG, null)

        if (jsonString != null) {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val ip = obj.getString("IP")
                if (ip != "IP_DONT_GET_ERROR") {
                    uniqueIps.add(ip)
                }
            }
        }

        // "Tümü" butonu ekleme ve seçili olmasını kontrol etme
        val allButton = addIPAddressRecord(getString(R.string.latency_logs_filters_ip_addresses_all))
        if (selectedIp == null) {
            updateFilterButtonColors(allButton)
        }

        // Dinamik IP butonları ekleme
        for (ip in uniqueIps.toList()) {
            val ipButton = addIPAddressRecord(ip)
            if (ip == selectedIp) {
                updateFilterButtonColors(ipButton)
            }
        }
    }

    private fun addIPAddressRecord(ipAddress: String) : Button {
        val btn = Button(this).apply {
            text = ipAddress
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#424242")) // Default renk
            setPadding(16, 8, 16, 8)
            setOnClickListener {
                if (ipAddress == getString(R.string.latency_logs_filters_ip_addresses_all)) {
                    RefreshBody()
                } else {
                    RefreshBody(ipAddress)
                }
                toggleMenu(false) // Filtre seçilince menüyü kapat
            }
        }
        ipFiltersContainer.addView(btn)
        return btn
    }

    private fun updateFilterButtonColors(selectedBtn: Button) {
        // Önceki seçili butonu normal renge döndür
        currentSelectedIpButton?.let {
            it.setBackgroundColor(Color.parseColor("#424242"))
        }
        // Yeni butonu mor yap ve kaydet
        selectedBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500))
        currentSelectedIpButton = selectedBtn
    }

    private fun setUpVars() {
        val lowestPingVal = sharedPreferences.getLong(OverlayService.KEY_LATENCY_LOW, 0)
        val highestPingVal = sharedPreferences.getLong(OverlayService.KEY_LATENCY_HIGH, 0)
        val averagePingVal = (highestPingVal + lowestPingVal) / 2

        lowestPing.text = "${getString(R.string.latency_logs_lowest)}: ${lowestPingVal}ms"
        highestPing.text = "${getString(R.string.latency_logs_highest)}: ${highestPingVal}ms"
        averagePing.text = "${getString(R.string.latency_logs_avarage)}: ${averagePingVal}ms"
    }

    private fun clearLatencyLogViews() {
        recordsContainer.removeAllViews()
    }

    private fun addLatencyLogView(latency: Long, date: String, ip: String, id: Int) {
        // Bu kısım aynı kalıyor, log kayıtlarını dinamik olarak oluşturuyor
        val logLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (150 * resources.displayMetrics.density).toInt()
            ).apply {
                setMargins(0, 0, 0, (16 * resources.displayMetrics.density).toInt())
            }
            setBackgroundColor(Color.parseColor("#424242"))
            setPadding(16, 16, 16, 16)
        }

        val idAndTitleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleView = TextView(this).apply {
            text = getPingTitle(latency)
            setTextColor(Color.WHITE)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            )
        }

        val idView = TextView(this).apply {
            text = "#$id"
            setTextColor(Color.WHITE)
            textSize = 16f
            setBackgroundColor(Color.parseColor("#FF6200EE"))
            setPadding(8, 4, 8, 4)
        }

        val pingView = TextView(this).apply {
            text = "${getString(R.string.latency_logs_ping)} ${latency}ms"
            setTextColor(Color.WHITE)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
        }
        val ipView = TextView(this).apply {
            text = "${getString(R.string.latency_logs_ip)} $ip"
            setTextColor(Color.WHITE)
            textSize = 16f
        }
        val dateView = TextView(this).apply {
            text = "${getString(R.string.latency_logs_date)} $date"
            setTextColor(Color.WHITE)
            textSize = 16f
        }

        idAndTitleLayout.addView(titleView)
        idAndTitleLayout.addView(idView)
        logLayout.addView(idAndTitleLayout)
        logLayout.addView(pingView)
        logLayout.addView(ipView)
        logLayout.addView(dateView)

        recordsContainer.addView(logLayout)
    }

    private fun getPingTitle(latency: Long): String {
        return when (latency) {
            sharedPreferences.getLong(OverlayService.KEY_LATENCY_LOW, 0) -> getString(R.string.latency_logs_lowest)
            sharedPreferences.getLong(OverlayService.KEY_LATENCY_HIGH, 0) -> getString(R.string.latency_logs_highest)
            else -> getString(R.string.latency_logs_avarage)
        }
    }

    private fun generateAndDownloadJson() {
        // 1. DİNAMİK JSON İÇERİĞİNİ HAZIRLA
        // Burası senin uygulamanın anlık durumuna göre ayarlanacak yer.
        val jsonContent = createDynamicJson()

        // 2. DOSYA YOLUNU VE ADINI BELİRLE
        // getExternalCacheDir(), uygulamanın harici önbellek dizinini verir.
        // Bu dizin uygulaman silindiğinde temizlenir.
        val cacheDir = externalCacheDir ?: run {
            Toast.makeText(this, "Önbellek dizinine erişilemiyor, amk.", Toast.LENGTH_LONG).show()
            return
        }

        // Dosya adı, örneğin: settings_20251030.json
        val fileName = "settings_${System.currentTimeMillis()}.json"
        val file = File(cacheDir, fileName)

        // 3. JSON'I DOSYAYA YAZ
        try {
            FileOutputStream(file).use { outputStream ->
                outputStream.write(jsonContent.toByteArray())
                outputStream.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Dosya yazma hatası oldu, aq.", Toast.LENGTH_LONG).show()
            return
        }

        // 4. DOSYAYI PAYLAŞIM VEYA İNDİRME İÇİN SUN
        // FileProvider kullanmak, Android'de güvenli dosya paylaşımı için ŞARTTIR.
        val fileUri: Uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            // Bu kısım senin uygulamanın package name'i olmalı (örn: com.yorastudio.overlaylatency.fileprovider)
            // Manifest dosyasında tanımlanmalıdır.
            "${packageName}.fileprovider",
            file
        )

        // Paylaşım Intent'i oluşturuluyor
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "application/json" // JSON MIME tipi
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Dosyayı okuma izni ver
        }

        // Kullanıcının indirme uygulamasını veya paylaşım menüsünü aç
        startActivity(Intent.createChooser(shareIntent, "JSON dosyasını kaydet veya paylaş"))

        Toast.makeText(this, "JSON hazırlandı ve indirme menüsü açıldı.", Toast.LENGTH_SHORT).show()
    }

    /**
     * Uygulama ayarlarını bir JSON string'ine dönüştürür.
     */
    private fun createDynamicJson(): String {
        // Senin kendi ayarlarını burada al ve JSON string'i oluştur.
        val jsonString = sharedPreferences.getString(OverlayService.KEY_LATENCY_LOG, null)
        return jsonString.toString()
    }
}