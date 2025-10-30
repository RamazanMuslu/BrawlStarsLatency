package com.yorastudio.overlaylatency

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.nio.charset.Charset

//
// TÜM SINIFLAR VE FONKSİYONLAR TEK DOSYADA
//

// 1. Veri Sınıfları (Data Classes)
data class Contributor(
    val contribution: String,
    val name: String,
    val contact: String,
    val website: String
)

// 2. RecyclerView Adapter ve ViewHolder
class ContributorAdapter(private val contributors: List<Contributor>) :
    RecyclerView.Adapter<ContributorAdapter.ContributorViewHolder>() {

    class ContributorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.text_contributor_name)
        val detailTextView: TextView = itemView.findViewById(R.id.text_contributor_detail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContributorViewHolder {
        // R.layout.item_contributor, senin item layout dosyanın adı olmalı!
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contributor, parent, false)
        return ContributorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContributorViewHolder, position: Int) {
        val contributor = contributors[position]
        holder.nameTextView.text = contributor.name
        holder.detailTextView.text = contributor.contribution

        // Tıklama olayını ekleyelim (Web sitesine gitme)
        holder.itemView.setOnClickListener {
            if (contributor.website.isNotBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contributor.website))
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    // Tıklama hatası olursa bir şey yapabilirsin.
                }
            }
        }
    }

    override fun getItemCount() = contributors.size
}

// 3. Ana Activity Sınıfı
class v7_ContributorsMenu : AppCompatActivity() {

    private var selectedColor: Int = Color.WHITE
    // Varsayıyorum ki layout'ta versiyon bilgisini gösteren bir TextView var.
    // Eğer yoksa, tvVersionInfo ile ilgili satırları kaldır.
    private lateinit var tvVersionInfo: TextView
    private lateinit var recyclerViewContributors: RecyclerView
    private lateinit var btnAboutApp_BackMain: ImageView

    // SharedPreferences'ı kolayca erişmek için lazy init ile tanımlıyoruz
    private val sharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", MODE_PRIVATE)
    }

    override fun attachBaseContext(newBase: Context?) {
        // LocaleHelper sınıfının var olduğunu varsayıyoruz.
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credits_version7)

        // XML'deki elemanları kodda buluyoruz
        btnAboutApp_BackMain = findViewById(R.id.btn_back_from_credits)
        recyclerViewContributors = findViewById(R.id.recycler_view_team_credits)

        // Eğer layout'ta bir TextView ile versiyon bilgisini göstermiyorsan, bu satırı YORUM SATIRI YAP/SİL.
        // tvVersionInfo = findViewById(R.id.tv_version_info)

        loadAndApplySettings()
        setupContributors()

        btnAboutApp_BackMain.setOnClickListener{
            finish()
        }
    }

    private fun loadAndApplySettings() {
        // Eğer tvVersionInfo layout'ta yoksa veya bulunamazsa burası hata verir.
        // if (::tvVersionInfo.isInitialized) {
        //    tvVersionInfo.text = getString(R.string.v7_about_app_version) + " " + getAppVersionDetails(this).versionName
        // }
    }

    private fun setupContributors() {
        // 1. JSON'dan destekçi listesini çek
        val contributors = getContributorsList(this)

        // 2. Adapter'ı oluştur ve RecyclerView'a ata
        val adapter = ContributorAdapter(contributors)
        recyclerViewContributors.adapter = adapter
    }

    // JSON dosyasını okuma fonksiyonu
    private fun getContributorsList(context: Context): List<Contributor> {
        val jsonString: String
        try {
            // assets/contributors.json dosyasını aç ve oku
            val inputStream = context.assets.open("contributors.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            jsonString = String(buffer, Charset.forName("UTF-8"))
        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        }

        // Gson kullanarak JSON string'i Contributor listesine dönüştür
        return try {
            val listType = object : TypeToken<List<Contributor>>() {}.type
            Gson().fromJson(jsonString, listType)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    // Uygulama versiyon bilgisini çeken fonksiyon
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