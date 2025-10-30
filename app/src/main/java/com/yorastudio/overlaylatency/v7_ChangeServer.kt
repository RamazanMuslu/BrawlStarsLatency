package com.yorastudio.overlaylatency

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL

// =========================================================================================
// 1. DATA CLASS (Server)
// =========================================================================================

data class Server(
    val id: Int,
    val name: String,
    val url: String,
    var isSelected: Boolean = false
)

// =========================================================================================
// 2. ACTIVITY SINIFI (v7_ChangeServer.kt)
// =========================================================================================

class v7_ChangeServer : AppCompatActivity() {

    private lateinit var btnRefreshServers: TextView

    // --- PING CACHE: Geri dönüşümlü View'larda gereksiz ping atmamak için ---
    private val pingCache = mutableMapOf<String, Long>()

    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", Context.MODE_PRIVATE)
    }

    private val serverList: List<Server> by lazy {
        loadServersFromAssets(this)
    }

    // ====================== Lifecycle Metotları ======================

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selectserver_version7) // Senin XML adın

        // XML elemanlarını bul
        val btnBack: ImageView = findViewById(R.id.btn_back_from_server)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_servers)
        btnRefreshServers = findViewById(R.id.text_server_status) // Refresh butonu

        // RecyclerView'ı kur
        setupServerRecyclerView(recyclerView)

        // Buton İşlemleri
        btnBack.setOnClickListener { finish() }

        // Yenileme Butonu
        btnRefreshServers.setOnClickListener {
            pingCache.clear() // Cache'i temizle
            setupServerRecyclerView(recyclerView) // Listeyi yeniden kurarak pingleri yenile
            Toast.makeText(this, "Ping değerleri güncelleniyor, aq!", Toast.LENGTH_SHORT).show()
        }
    }

    // ====================== Sunucu Seçim ve Liste Fonksiyonları ======================

    private fun setupServerRecyclerView(recyclerView: RecyclerView) {
        // SharedPreferences'tan KEY_FEED_URL yerine doğrudan String key kullanıyorum
        val currentUrl = sharedPreferences.getString(OverlayService.KEY_FEED_URL, "")

        val listForAdapter = serverList.map { it.copy(isSelected = it.url == currentUrl) }

        val adapter = ServerAdapter(
            serverList = listForAdapter,
            onItemClicked = { selectedServerName -> changeServer(selectedServerName, recyclerView) },
            pingScope = lifecycleScope,
            pingFunction = ::pingServer,
            statusFunction = ::getStatusText,
            pingCache = pingCache // Cache'i gönder
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun changeServer(serverName: String, recyclerView: RecyclerView) {
        val feedUrl = serverList.find { it.name == serverName }?.url

        if (!feedUrl.isNullOrEmpty()) {
            sharedPreferences.edit().putString(OverlayService.KEY_FEED_URL, feedUrl).apply()

            Toast.makeText(this, getString(R.string.toast_server_changed) + " ${serverName}", Toast.LENGTH_SHORT).show()

            // NOT: restartOverlayService() fonksiyonunu Activity içinde tanımladığını varsayıyoruz.
            // restartOverlayService()

            setupServerRecyclerView(recyclerView) // UI'ı güncelle
        } else {
            Toast.makeText(this, "Sunucu URL'si bulunamadı: $serverName", Toast.LENGTH_SHORT).show()
        }
    }

    // --- PING, STATUS ve JSON YÜKLEME Fonksiyonları ---

    private fun loadServersFromAssets(context: Context): List<Server> {
        val jsonString: String = try {
            context.assets.open("servers.json").bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return emptyList()
        }
        val listServerType = object : TypeToken<List<Server>>() {}.type
        return Gson().fromJson(jsonString, listServerType)
    }

    fun pingServer(url: String): Long {
        val feedUrl = url.trim()

        if (feedUrl.isEmpty()) return -2L // Boş URL

        val targetHost: String
        val targetPort: Int

        try {
            val urlObj = URL(feedUrl)
            targetHost = urlObj.host
            targetPort = if (urlObj.port != -1) urlObj.port else if (urlObj.protocol == "https") 443 else 80
        } catch (e: Exception) {
            return -3L // URL ayrıştırma hatası
        }

        return measureTcpLatency(targetHost, targetPort)
    }

    private fun measureTcpLatency(host: String, port: Int): Long {
        val timeoutMs = 2000L // 2 saniye zaman aşımı
        var latency = -1L
        var socket: Socket? = null

        try {
            val startTime = System.currentTimeMillis()

            socket = Socket()
            socket.soTimeout = timeoutMs.toInt()

            socket.connect(InetSocketAddress(host, port), timeoutMs.toInt())

            val endTime = System.currentTimeMillis()
            latency = endTime - startTime

        } catch (e: ConnectException) {
            latency = -1L // Bağlantı reddedildi
        } catch (e: SocketTimeoutException) {
            latency = -2L // Zaman aşımı
        } catch (e: Exception) {
            latency = -3L // Diğer hatalar
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) { /* İgnore */ }
        }
        return latency
    }

    fun getStatusText(pingMs: Long): String {
        return when {
            pingMs == -1L || pingMs == -2L || pingMs == -3L -> "Erişilemiyor"
            pingMs <= 50 -> "Stabil / Düşük Yük"
            pingMs <= 150 -> "Orta Yük"
            pingMs <= 300 -> "Yüksek Yük"
            else -> "Çok Yüksek Yük"
        }
    }

    // NOT: Bu fonksiyonu tanımlamayı unutma!
    private fun restartOverlayService() {
        // OverlayService'i durdurup yeniden başlatan kod burada olmalı.
        // val serviceIntent = Intent(this, OverlayService::class.java)
        // stopService(serviceIntent); startService(serviceIntent)
    }
}

// =========================================================================================
// 3. ADAPTER SINIFI (ServerAdapter.kt)
// =========================================================================================

class ServerAdapter(
    private val serverList: List<Server>,
    private val onItemClicked: (serverName: String) -> Unit,
    private val pingScope: CoroutineScope,
    private val pingFunction: (String) -> Long,
    private val statusFunction: (Long) -> String,
    private val pingCache: MutableMap<String, Long> // Cache
) : RecyclerView.Adapter<ServerAdapter.ServerViewHolder>() {

    inner class ServerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view.findViewById(R.id.server_item_container)
        val locationTextView: TextView = view.findViewById(R.id.text_server_location)
        val statusTextView: TextView = view.findViewById(R.id.text_server_status_text)
        val pingTextView: TextView = view.findViewById(R.id.text_server_ping)
        val selectedIndicator: ImageView = view.findViewById(R.id.image_server_selected_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_server, parent, false)
        return ServerViewHolder(view)
    }

    override fun getItemCount(): Int = serverList.size

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val server = serverList[position]

        // 1. STATİK ALANLARI HER ZAMAN GÜNCELLE
        holder.locationTextView.text = server.name // Sunucu adı artık kaybolmayacak.
        holder.container.setOnClickListener { onItemClicked(server.name) }

        // 2. TİK İŞARETİNİN GÖRÜNÜRLÜĞÜNÜ YÖNET (Ping HER ZAMAN GÖRÜNÜR)
        // Ping'i asla gizlemiyoruz!
        holder.selectedIndicator.visibility =
            if (server.isSelected) View.VISIBLE else View.GONE

        // 3. PING ve CACHE MANTIĞI (Aynı kalır)
        val cachedPing = pingCache[server.url]

        // Yükleniyor durumları
        holder.pingTextView.text = "..."
        holder.statusTextView.text = "Kontrol Ediliyor..."

        if (cachedPing != null) {
            // CACHE HIT
            val pingText = if (cachedPing >= 0) "${cachedPing}ms" else "N/A"

            holder.pingTextView.text = pingText
            holder.statusTextView.text = statusFunction(cachedPing)

        } else {
            // PING AT
            pingScope.launch {
                val pingResult = withContext(Dispatchers.IO) { pingFunction(server.url) }
                pingCache[server.url] = pingResult

                withContext(Dispatchers.Main) {
                    if (holder.adapterPosition == position) {
                        val pingText = if (pingResult >= 0) "${pingResult}ms" else "N/A"

                        holder.pingTextView.text = pingText
                        holder.statusTextView.text = statusFunction(pingResult)
                    }
                }
            }
        }
    }
}