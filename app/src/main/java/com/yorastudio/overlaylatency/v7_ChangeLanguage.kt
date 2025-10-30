package com.yorastudio.overlaylatency

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

// Dil verilerini tutan Data Class (Daha önce konuşmuştuk)
data class Language(
    val name: String,        // Kullanıcıya gösterilecek (Örn: "Türkçe")
    val code: String,        // Android kodu (Örn: "tr")
    var isSelected: Boolean = false
)

class v7_ChangeLanguage : AppCompatActivity() {

    // SharedPreferences'ı kullanmak için tanımlama
    private val sharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", Context.MODE_PRIVATE)
    }

    // Uygulamanın desteklediği DİLLERİN LİSTESİ (Koddan çektik)
    private val availableLanguages = listOf(
        Language(name = "English", code = "en"),
        Language(name = "Türkçe", code = "tr"),
        Language(name = "Deutsch", code = "de"),
        Language(name = "Français", code = "fr"),
        Language(name = "中文", code = "zh"),
        Language(name = "Русский", code = "ru")
    )

    // --- Lifecycle Metotları ---

    override fun attachBaseContext(newBase: Context?) {
        // Activity ilk oluşturulurken Context'i kaydedilen dile göre ayarla
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Eğer tek bir layout kullanılıyorsa, onu buraya yüklemelisin.
        // Ancak RecyclerView kullandığın XML'e göre ilerliyorum.
        setContentView(R.layout.activity_changelanguage_version7)

        // XML elemanlarını bul
        val btnBack: ImageView = findViewById(R.id.btn_back_from_language) // XML'deki ID'yi varsayıyorum
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_languages_full)

        // Mevcut seçili dili SharedPreferences'tan çek
        val currentLangCode = LocaleHelper.getLanguage(this)

        // 1. Diller listesini hazırla: Current Lang Code'u bul ve işaretle
        val listForAdapter = availableLanguages.map { lang ->
            lang.copy(isSelected = lang.code == currentLangCode)
        }

        // 2. RecyclerView ve Adapter'ı Kur
        val adapter = LanguageAdapter(listForAdapter) { selectedCode ->
            // Tıklama Olayı (Callback)
            setNewLocale(selectedCode, restartService = true)
        }

        // Layout Manager'ı XML'de tanımlamış olsan bile, kodda da set etmek bazen iyidir.
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 3. Geri Butonu
        btnBack.setOnClickListener {
            finish() // Activity'yi kapat ve bir önceki ekrana dön
        }
    }

    // --- Yardımcı Fonksiyonlar ---

    /**
     * Yeni bir dil ayarlar, SharedPreferences'a kaydeder ve uygulamayı yeniden başlatır.
     */
    private fun setNewLocale(languageCode: String, viewToast: Boolean = true, restartService: Boolean = false) {

        // Zaten aynı dildeysek, gereksiz işlem yapma
        if (LocaleHelper.getLanguage(this) == languageCode) {
            return
        }

        // 1. Yeni dili kaydet (LocaleHelper senin için SharedPreferences'ı günceller)
        LocaleHelper.setLanguage(this, languageCode)

        // 2. Kullanıcıya bilgi ver
        if (viewToast) {
            Toast.makeText(this, getString(R.string.toast_langauge_changed), Toast.LENGTH_SHORT).show()
        }

        // 3. Activity'yi yeniden başlat
        // Uygulamayı tamamen yeniden başlatan Intent'i kullanmak en güvenli yoldur.
        val intent = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Mevcut (eski dildeki) Activity'yi kapat.

        // 4. İsteğe bağlı olarak servisi yeniden başlat
        if (restartService) {
            restartOverlayService()
        }
    }

    private fun restartOverlayService() {
        // OverlayService.isServiceRunning değişkenini doğru bir şekilde yönettiğini varsayıyorum.
        val serviceIntent = Intent(this, OverlayService::class.java)
        // Eğer servis çalışıyorsa önce durdur, sonra yeniden başlat
        if (OverlayService.isServiceRunning) {
            stopService(serviceIntent)
            startService(serviceIntent)
        }
    }

    // --- Dil Adapter'ın Activity/Fragment İçinde Olması Gereken Basitleştirilmiş Hali ---
    // NOT: Bu sınıfı ayrı bir dosyada tutman daha iyidir!

    inner class LanguageAdapter(
        private val languageList: List<Language>,
        private val onItemClicked: (languageCode: String) -> Unit
    ) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

        inner class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameTextView: TextView = view.findViewById(R.id.text_language_name)
            val selectedIndicator: ImageView = view.findViewById(R.id.image_selected_indicator)
            val container: View = view.findViewById(R.id.language_item_container) // Tıklama konteynırı
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LanguageViewHolder {
            val view = layoutInflater.inflate(R.layout.item_language, parent, false) // Senin Item XML'in
            return LanguageViewHolder(view)
        }

        override fun getItemCount(): Int = languageList.size

        override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
            val language = languageList[position]

            holder.nameTextView.text = language.name

            // Tik işaretini göster/gizle
            holder.selectedIndicator.visibility =
                if (language.isSelected) View.VISIBLE else View.GONE

            // Tıklama Olayı
            holder.container.setOnClickListener {
                onItemClicked(language.code) // Activity'deki setNewLocale'ı çağırır.
            }
        }
    }
}