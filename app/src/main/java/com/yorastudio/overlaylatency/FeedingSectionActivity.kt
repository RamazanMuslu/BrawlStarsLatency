package com.yorastudio.overlaylatency

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences
import android.widget.ImageView

class FeedingSectionActivity : AppCompatActivity() {

    /***private lateinit var etFeedUrl: EditText
    private lateinit var btnSaveFeedUrl: Button
    private lateinit var btnClearFeedUrl: Button**/

    private val OVERLAY_PERMISSION_REQUEST_CODE = 101

    private lateinit var tvCurrentFeedUrl: TextView
    private lateinit var tvLatencyRefreshingTime: TextView

    private lateinit var btnServer_EUGE1: Button
    private lateinit var btnServer_EUGE2: Button
    private lateinit var btnServer_AP_INDIA: Button
    private lateinit var btnServer_AP_SINGAPORE: Button
    private lateinit var btnServer_AP_HONGKONG: Button
    private lateinit var btnServer_AP_JAPAN: Button
    private lateinit var btnServer_EU_ITALY: Button
    private lateinit var btnServer_EU_IRELAND: Button
    private lateinit var btnServer_AP_SYDNEY: Button
    private lateinit var btnServer_EU_FINLAND: Button
    private lateinit var btnServer_NA_VIRGINIA: Button
    private lateinit var btnServer_ME_BAHRAIN: Button
    private lateinit var btnServer_NA_LOSANGELES: Button
    private lateinit var btnServer_NA_OREGON: Button
    private lateinit var btnServer_NA_DALLAS: Button
    private lateinit var btnServer_NA_MIAMI: Button
    private lateinit var btnServer_SA_PERU: Button
    private lateinit var btnServer_SA_CHILE: Button
    private lateinit var btnServer_SA_BRASIL: Button


    private lateinit var seekBarLatencyRefreshingTime: SeekBar
    private lateinit var btnLatencyRefreshingTimeSetDefault: Button

    // SharedPreferences'ı kolayca erişmek için lazy init ile tanımlıyoruz
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", Context.MODE_PRIVATE)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feeding_section) // Kendi layout'umuzu yüklüyoruz

        // XML'deki elemanları kodda buluyoruz
        btnServer_EUGE1 = findViewById(R.id.btnSelectServer_EU_GE1)
        btnServer_EUGE2 = findViewById(R.id.btnSelectServer_EU_GE2)
        btnServer_AP_INDIA = findViewById(R.id.btnSelectServer_AP_INDIA)
        btnServer_AP_SINGAPORE = findViewById(R.id.btnSelectServer_AP_SINGAPORE)
        btnServer_AP_HONGKONG = findViewById(R.id.btnSelectServer_AP_HONGKONG)
        btnServer_AP_JAPAN = findViewById(R.id.btnSelectServer_AP_JAPAN)
        btnServer_EU_ITALY = findViewById(R.id.btnSelectServer_EU_ITALY)
        btnServer_EU_IRELAND = findViewById(R.id.btnSelectServer_EU_IRELAND)
        btnServer_AP_SYDNEY = findViewById(R.id.btnSelectServer_AP_SYDNEY)
        btnServer_EU_FINLAND = findViewById(R.id.btnSelectServer_EU_FINLAND)
        btnServer_NA_VIRGINIA = findViewById(R.id.btnSelectServer_NA_VIRGINIA)
        btnServer_ME_BAHRAIN = findViewById(R.id.btnSelectServer_ME_BAHRAIN)
        btnServer_NA_LOSANGELES = findViewById(R.id.btnSelectServer_NA_LOSANGELES)
        btnServer_NA_OREGON = findViewById(R.id.btnSelectServer_NA_OREGON)
        btnServer_NA_DALLAS = findViewById(R.id.btnSelectServer_NA_DALLAS)
        btnServer_NA_MIAMI = findViewById(R.id.btnSelectServer_NA_MIAMI)
        btnServer_SA_PERU = findViewById(R.id.btnSelectServer_SA_PERU)
        btnServer_SA_CHILE = findViewById(R.id.btnSelectServer_SA_CHILE)
        btnServer_SA_BRASIL = findViewById(R.id.btnSelectServer_SA_BRASIL1)


        /***etFeedUrl = findViewById(R.id.etFeedUrl)
        btnSaveFeedUrl = findViewById(R.id.btnSaveFeedUrl)
        btnClearFeedUrl = findViewById(R.id.btnClearFeedUrl)**/
        tvCurrentFeedUrl = findViewById(R.id.tvCurrentFeedUrl)
        tvLatencyRefreshingTime = findViewById(R.id.tvLatencyRefreshingTime)
        seekBarLatencyRefreshingTime = findViewById(R.id.seekBarLatencyRefreshingTime)
        btnLatencyRefreshingTimeSetDefault = findViewById(R.id.btnLatencyRefreshingTimeSetDefault)

        val btnFeedingSection_BackMain: ImageView = findViewById(R.id.btnFeedingSection_BackMain)

        // Mevcut kaydedilmiş URL'yi göster
        loadAndDisplayCurrentUrl()

        btnFeedingSection_BackMain.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java) // Bu satırı aktif et
            startActivity(intent)
        }

        btnServer_EUGE1.setOnClickListener{
            ChangeServer("EU/Germany-1")
        }

        btnServer_EUGE2.setOnClickListener{
            ChangeServer("EU/Germany-2")
        }

        btnServer_AP_INDIA.setOnClickListener{
            ChangeServer("AP/India")
        }

        btnServer_AP_SINGAPORE.setOnClickListener{
            ChangeServer("AP/Singapore")
        }

        btnServer_AP_HONGKONG.setOnClickListener{
            ChangeServer("AP/HongKong")
        }

        btnServer_AP_JAPAN.setOnClickListener{
            ChangeServer("AP/Japan")
        }

        btnServer_EU_ITALY.setOnClickListener{
            ChangeServer("EU/Italy")
        }

        btnServer_EU_IRELAND.setOnClickListener{
            ChangeServer("EU/Ireland")
        }

        btnServer_AP_SYDNEY.setOnClickListener{
            ChangeServer("AP/Sydney")
        }

        btnServer_EU_FINLAND.setOnClickListener{
            ChangeServer("EU/Finland")
        }

        btnServer_NA_VIRGINIA.setOnClickListener{
            ChangeServer("NA/Virginia")
        }

        btnServer_ME_BAHRAIN.setOnClickListener{
            ChangeServer("ME/Bahrain")
        }

        btnServer_NA_LOSANGELES.setOnClickListener{
            ChangeServer("NA/LosAngeles")
        }

        btnServer_NA_OREGON.setOnClickListener{
            ChangeServer("NA/Oregon")
        }

        btnServer_NA_DALLAS.setOnClickListener{
            ChangeServer("NA/Dallas")
        }

        btnServer_NA_MIAMI.setOnClickListener{
            ChangeServer("NA/Miami")
        }

        btnServer_SA_PERU.setOnClickListener{
            ChangeServer("SA/Peru")
        }

        btnServer_SA_CHILE.setOnClickListener{
            ChangeServer("SA/Chile")
        }

        btnServer_SA_BRASIL.setOnClickListener{
            ChangeServer("SA/Brasil-1")
        }

        btnLatencyRefreshingTimeSetDefault.setOnClickListener{
            sharedPreferences.edit().putLong(OverlayService.KEY_REFRESHING_TIME, 1000).apply()
            Toast.makeText(this, getString(R.string.toast_refreshing_time_set_default), Toast.LENGTH_SHORT).show()
            loadAndDisplayCurrentUrl() // Ekranı güncelle
            restartOverlayService() // Overlay servisini yeniden başlat ki yeni URL'yi çeksin
        }

        seekBarLatencyRefreshingTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // SeekBar kaydırıldıkça anlık boyutu göster
                tvLatencyRefreshingTime.text = getString(R.string.feeding_section_refreshing_time_current) + " ${progress}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Kaydırmaya başlandığında
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sharedPreferences.edit().putLong(OverlayService.KEY_REFRESHING_TIME, seekBarLatencyRefreshingTime.progress.toLong()).apply()
            }
        })

        val serverNameFromDeepLink = intent.getStringExtra("SERVER_NAME_FROM_DEEPLINK")
        if (!serverNameFromDeepLink.isNullOrEmpty()) {
            ChangeServerDeepLink(serverNameFromDeepLink)
        }
    }

    private fun ChangeServerDeepLink(serverName: String) {
        val feedUrl = ServerAddressDeepLink(serverName) //"https://dynamodb.eu-central-2.amazonaws.com"
        sharedPreferences.edit().putString(OverlayService.KEY_FEED_URL, feedUrl).apply()
        Toast.makeText(this, getString(R.string.toast_server_changed) + " ${feedUrl}", Toast.LENGTH_SHORT).show()
        loadAndDisplayCurrentUrl() // Ekranı güncelle
        restartOverlayService() // Overlay servisini yeniden başlat ki yeni URL'yi çeksin
    }

    private fun ChangeServer(serverName: String) {
        val feedUrl = ServerAddress(serverName) //"https://dynamodb.eu-central-2.amazonaws.com"
        sharedPreferences.edit().putString(OverlayService.KEY_FEED_URL, feedUrl).apply()
        Toast.makeText(this, getString(R.string.toast_server_changed) + " ${feedUrl}", Toast.LENGTH_SHORT).show()
        loadAndDisplayCurrentUrl() // Ekranı güncelle
        restartOverlayService() // Overlay servisini yeniden başlat ki yeni URL'yi çeksin
    }

    public fun ServerAddressDeepLink(serverName: String) : String {
        return when (serverName) {
            "ap_india" -> "https://dynamodb.ap-south-1.amazonaws.com"
            "ap_singapore" -> "https://dynamodb.ap-southeast-1.amazonaws.com"
            "ap_hongkong" -> "https://dynamodb.ap-east-1.amazonaws.com"
            "ap_japan" -> "https://dynamodb.ap-northeast-1.amazonaws.com"
            "eu_italy" -> "https://dynamodb.eu-south-1.amazonaws.com"
            "eu_ireland" -> "https://dynamodb.eu-west-1.amazonaws.com" // Güncellendi
            "ap_sydney" -> "https://dynamodb.ap-southeast-2.amazonaws.com" // Güncellendi
            "eu_finland" -> "https://dynamodb.eu-north-1.amazonaws.com" // Güncellendi
            "na_virginia" -> "https://dynamodb.us-east-1.amazonaws.com" // Güncellendi
            "me_bahrain" -> "https://dynamodb.me-south-1.amazonaws.com" // Güncellendi
            "na_losangeles" -> "https://dynamodb.us-west-2.amazonaws.com" // Güncellendi
            "na_oregon" -> "https://dynamodb.us-west-2.amazonaws.com" // Güncellendi
            "na_dallas" -> "https://dynamodb.us-east-1.amazonaws.com" // Güncellendi
            "na_miami" -> "https://dynamodb.us-east-1.amazonaws.com" // Güncellendi
            "sa_peru" -> "https://dynamodb.us-east-1.amazonaws.com" // Güncellendi
            "sa_chile" -> "https://dynamodb.us-east-1.amazonaws.com" // Güncellendi
            "sa_brasil_1" -> "https://dynamodb.sa-east-1.amazonaws.com" // Güncellendi (Brasil-1 -> brasil_1)
            "eu_germany_1" -> "https://dynamodb.eu-central-1.amazonaws.com" // Güncellendi
            "eu_germany_2" -> "https://dynamodb.eu-central-2.amazonaws.com" // Güncellendi
            else -> ""
        }
    }

    private fun ServerAddress(serverName: String) : String {
        return when (serverName) {
            "AP/India" -> "https://dynamodb.ap-south-1.amazonaws.com"
            "AP/Singapore" -> "https://dynamodb.ap-southeast-1.amazonaws.com"
            "AP/HongKong" -> "https://dynamodb.ap-east-1.amazonaws.com"
            "AP/Japan" -> "https://dynamodb.ap-northeast-1.amazonaws.com"
            "EU/Italy" -> "https://dynamodb.eu-south-1.amazonaws.com"
            "EU/Ireland" -> "https://dynamodb.eu-west-1.amazonaws.com"
            "AP/Sydney" -> "https://dynamodb.ap-southeast-2.amazonaws.com"
            "EU/Finland" -> "https://dynamodb.eu-north-1.amazonaws.com"
            "NA/Virginia" -> "https://dynamodb.us-east-1.amazonaws.com"
            "ME/Bahrain" -> "https://dynamodb.me-south-1.amazonaws.com"
            "NA/LosAngeles" -> "https://dynamodb.us-west-2.amazonaws.com"
            "NA/Oregon" -> "https://dynamodb.us-west-2.amazonaws.com"
            "NA/Dallas" -> "https://dynamodb.us-east-1.amazonaws.com"
            "NA/Miami" -> "https://dynamodb.us-east-1.amazonaws.com"
            "SA/Peru" -> "https://dynamodb.us-east-1.amazonaws.com"
            "SA/Chile" -> "https://dynamodb.us-east-1.amazonaws.com"
            "SA/Brasil-1" -> "https://dynamodb.sa-east-1.amazonaws.com"
            "EU/Germany-1" -> "https://dynamodb.eu-central-1.amazonaws.com"
            "EU/Germany-2" -> "https://dynamodb.eu-central-2.amazonaws.com"
            else -> ""
        }
    }

    // Kaydedilmiş URL'yi yükleyip ekranda gösteren fonksiyon
    private fun loadAndDisplayCurrentUrl() {
        val currentUrl = sharedPreferences.getString(OverlayService.KEY_FEED_URL, ServerAddress("EU/Germany-2"))
        val currentRefreshingTime = sharedPreferences.getLong(OverlayService.KEY_REFRESHING_TIME, 1000)

        tvCurrentFeedUrl.text = getString(R.string.feeding_section_current_server) + " ${currentUrl}"
        tvLatencyRefreshingTime.text = getString(R.string.feeding_section_refreshing_time_current) + " ${currentRefreshingTime}"
        seekBarLatencyRefreshingTime.progress = currentRefreshingTime.toInt()
    }

    // Overlay servisini durdurup yeniden başlatan fonksiyon
    // Bu sayede OverlayService, SharedPreferences'taki güncel URL'yi okuyup işlemeye başlar.
    private fun restartOverlayService() {
        val serviceIntent = Intent(this, OverlayService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Eğer izin verilmemişse, kullanıcıyı telefonun izin ayarlarına gönderiyoruz.
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName") // Bizim uygulamanın ayarlarına gitsin diye.
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE) // İzni istedik.
        }
        else {
            if (OverlayService.isServiceRunning) {
                stopService(serviceIntent)
            }
            // Servisi başlat (bu, onCreate'ini tekrar tetikleyecektir)
            startService(serviceIntent)
        }
    }
}