package com.yorastudio.overlaylatency

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.Visibility
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import android.content.Intent
import android.net.Uri

// **********************************************
// 🔥 JSON VERSİYON KONTROLÜ İÇİN YENİ SINIFLAR
// **********************************************

// 1. GitHub'daki versions.json dosyasının yapısı
// Örn: {"latest_version_code": 102, "latest_version_name": "6.40.1", "update_url": "..."}
data class RemoteVersionInfo(
    @SerializedName("latest_version_code")
    val latestVersionCode: Int, // Int olarak çekip direkt karşılaştıracağız
    @SerializedName("latest_version_name")
    val latestVersionName: String,
    @SerializedName("update_url")
    val updateUrl: String
)

// 2. GitHub Raw Content Servis Arayüzü (Interface)
interface RawJsonService {
    // GitHub Raw URL'in BASE_URL'den sonraki yolu:
    // BASE_URL = https://raw.githubusercontent.com/
    // Tam URL: https://raw.githubusercontent.com/RamazanMuslu/BrawlStarsLatency/main/versions.json
    @GET("RamazanMuslu/BrawlStarsLatency/main/versions.json")
    suspend fun getRemoteVersionInfo(): RemoteVersionInfo
}

// 3. Retrofit İstemcisi
object RetrofitClient {
    // Raw content URL'in tabanı
    private const val BASE_URL = "https://raw.githubusercontent.com/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Servis örneği
    val rawJsonService: RawJsonService by lazy {
        retrofit.create(RawJsonService::class.java)
    }
}

// **********************************************
// 🔥 ANA AKTİVİTE KODU
// **********************************************

class v7_VersionCheck : AppCompatActivity() {

    // Kullanılacak XML elementleri
    private lateinit var tvVersionInfo: TextView
    private lateinit var tvUpdateStatus: TextView // Güncelleme durumu için yeni TextView (XML'e eklenmeli!)
    private lateinit var btnDownloadUpdate: Button

    // SharedPreferences'ı kolayca erişmek için lazy init ile tanımlıyoruz
    private val sharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", MODE_PRIVATE)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_updatechecker_version7) // Kendi layout'umuzu yüklüyoruz

        // XML'deki elemanları kodda buluyoruz
        val btnAboutApp_BackMain: ImageView = findViewById(R.id.btn_back_from_update)
        val btnCheck: Button = findViewById(R.id.btn_check_for_updates_large)
        tvVersionInfo = findViewById(R.id.text_detail_status_message)
        btnDownloadUpdate = findViewById(R.id.btn_update_app)
        tvUpdateStatus = findViewById(R.id.tv_update_status)

        loadAndApplySettings()
        // 🔥 Versiyon kontrolünü burada başlatıyoruz
        checkAppVersion()

        btnCheck.setOnClickListener {
            checkAppVersion()
        }

        btnAboutApp_BackMain.setOnClickListener{
            finish()
        }
    }

    private fun loadAndApplySettings() {
        val currentVersionName = getAppVersionDetails(this).versionName
        // Uygulamanın güncel sürümünü göster
        tvVersionInfo.text = getString(R.string.v7_about_app_version) + " " + currentVersionName
        // Kontrol başlamadan önce bekleme mesajı
        // (R.string.v7_checking_update gibi bir string tanımla)
        tvUpdateStatus.text = "Güncelleme Kontrol Ediliyor..."
    }

    /**
     * GitHub'daki JSON dosyasından en son versionCode'u çeker ve uygulamanınkiyle karşılaştırır.
     */
    private fun checkAppVersion() {
        btnDownloadUpdate.visibility = Button.GONE
        val currentVersionCode = getAppVersionDetails(this).versionCode
        val currentVersionName = getAppVersionDetails(this).versionName

        // Ağ işlemleri için arka plan thread'i (UI donmasın diye)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // JSON dosyasını çek
                val remoteInfo = RetrofitClient.rawJsonService.getRemoteVersionInfo()
                val remoteVersionCode = remoteInfo.latestVersionCode
                val remoteVersionName = remoteInfo.latestVersionName

                Log.d("VersionCheck", "Remote Code: $remoteVersionCode, Current Code: $currentVersionCode")

                withContext(Dispatchers.Main) {
                    if (remoteVersionCode > currentVersionCode) {
                        tvUpdateStatus.setTextColor(Color.parseColor("#FFC107")) // Yeni Sürüm Rengi
                        tvUpdateStatus.text = getString(R.string.v7_check_for_update_new_version_avalible) + "($remoteVersionName)"
                        btnDownloadUpdate.visibility = Button.VISIBLE
                        btnDownloadUpdate.setOnClickListener {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(remoteVersionName))
                        }


                    } else if (remoteVersionCode == currentVersionCode) {
                        tvUpdateStatus.setTextColor(Color.parseColor("#4CAF50")) // Güncel Sürüm Rengi
                        tvUpdateStatus.text = getString(R.string.v7_check_for_update_up_to_date)
                    }
                    else if (remoteVersionCode < currentVersionCode) {
                        tvUpdateStatus.setTextColor(Color.parseColor("#4CAF50")) // Güncel Sürüm Rengi
                        tvUpdateStatus.text = getString(R.string.v7_check_for_update_development_environment_enable)
                    }
                    else {
                        tvUpdateStatus.setTextColor(Color.RED) // Güncel Sürüm Rengi
                        tvUpdateStatus.text = getString(R.string.v7_check_for_update_an_error_occurred)
                    }
                }
            } catch (e: Exception) {
                // Hata yakalama
                Log.e("VersionCheck", "JSON dosyasından sürüm çekilemedi: ${e.message}")
                withContext(Dispatchers.Main) {
                    tvUpdateStatus.setTextColor(Color.RED)
                    tvUpdateStatus.text = getString(R.string.v7_check_for_update_an_error_occurred)
                }
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
            // AppVersionInfo'nun var olduğunu varsayarak döndürüyoruz.
            return AppVersionInfo(versionCode, versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            // AppVersionInfo'nun var olduğunu varsayarak döndürüyoruz.
            return AppVersionInfo(0, "Unknown")
        }
    }
}