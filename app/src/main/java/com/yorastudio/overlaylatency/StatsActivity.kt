package com.yorastudio.overlaylatency

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import java.math.RoundingMode

class StatsActivity : AppCompatActivity() {

    private val sharedPreferences by lazy {
        getSharedPreferences("OverlayPrefs", Context.MODE_PRIVATE)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleHelper.onAttach(it) })
    }

    private lateinit var backButton: ImageView
    private lateinit var tvUsageTime: TextView
    private lateinit var tvDataUsage: TextView
    private lateinit var tvDataUsageOnMainMenu: TextView
    private lateinit var tvDataUsageTotal: TextView
    private lateinit var tvPingCount: TextView
    private lateinit var tvPingCountMainMenu: TextView
    private lateinit var tvPingCountTotal: TextView
    private lateinit var tvLowestPing: TextView
    private lateinit var tvHighestPing: TextView
    private lateinit var tvAveragePing: TextView
    private lateinit var btn_open_latency_logs: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        initViews()
        setClickListeners()
        loadStats()
    }

    private fun initViews() {
        backButton = findViewById(R.id.back_button)
        tvUsageTime = findViewById(R.id.tv_usage_time)
        tvDataUsage = findViewById(R.id.tv_data_usage)
        tvDataUsageOnMainMenu = findViewById(R.id.tv_data_usage_main_menu)
        tvDataUsageTotal = findViewById(R.id.tv_data_usage_total)
        tvPingCount = findViewById(R.id.tv_ping_count)
        tvPingCountMainMenu = findViewById(R.id.tv_ping_count_main_menu)
        tvPingCountTotal = findViewById(R.id.tv_ping_count_total)
        tvLowestPing = findViewById(R.id.tv_lowest_ping)
        tvHighestPing = findViewById(R.id.tv_highest_ping)
        tvAveragePing = findViewById(R.id.tv_average_ping)
        btn_open_latency_logs = findViewById(R.id.btn_open_latency_logs)
    }

    private fun setClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        btn_open_latency_logs.setOnClickListener {
            val intent = Intent(this, LatencyLogsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadStats() {
        // SharedPreferences'tan kaydedilmiş değerleri al
        val usageTimeMillis = sharedPreferences.getLong(OverlayService.KEY_USAGE_TIME, 0L)
        val dataUsageBytesOnOverlay:Long = sharedPreferences.getLong( OverlayService.KEY_USAGE_DATA, 0L)
        val dataUsageBytesOnMainMenu:Long = sharedPreferences.getLong( OverlayService.KEY_USAGE_DATA_MAIN_MENU, 0L)
        val dataUsageBytesTotal = dataUsageBytesOnOverlay + dataUsageBytesOnMainMenu
        val lowestPing = sharedPreferences.getLong(OverlayService.KEY_LATENCY_LOW, 0L)
        val highestPing = sharedPreferences.getLong(OverlayService.KEY_LATENCY_HIGH, 0L)
        val pingCount = sharedPreferences.getLong(OverlayService.KEY_PING_COUNT, 0L)
        val pingCountMainMenu = sharedPreferences.getLong(OverlayService.KEY_PING_COUNT_MAIN_MENU, 0L)
        val pingCountTotal = pingCount + pingCountMainMenu

        // TextView'lere değerleri yazdır
        tvUsageTime.text = formatUsageTime(usageTimeMillis)
        tvDataUsage.text = formatDataUsage(dataUsageBytesOnOverlay)
        tvDataUsageOnMainMenu.text = formatDataUsage(dataUsageBytesOnMainMenu)
        tvDataUsageTotal.text = formatDataUsage(dataUsageBytesTotal)
        tvPingCount.text = pingCount.toString()
        tvPingCountMainMenu.text = pingCountMainMenu.toString()
        tvPingCountTotal.text = pingCountTotal.toString()
        tvLowestPing.text = "${lowestPing}ms"
        tvHighestPing.text = "${highestPing}ms"
        tvAveragePing.text = "${0}ms"
    }

    private fun formatDataUsage(bytes: Long): String {
        if (bytes <= 0) {
            return "0 B"
        }

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var i = 0
        var formattedBytes = bytes.toDouble()

        while (formattedBytes >= 1024 && i < units.size - 1) {
            formattedBytes /= 1024
            i++
        }

        // BigDecimal ile küsüratları daha doğru hesapla
        val decimalValue = BigDecimal(formattedBytes).setScale(2, RoundingMode.HALF_UP)

        return "${decimalValue.toPlainString()} ${units[i]}"
    }

    private fun formatUsageTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val result = StringBuilder()

        if (hours > 0) {
            result.append("$hours ${getString(R.string.stats_usage_time_hours)} ")
        }
        if (minutes > 0) {
            result.append("$minutes ${getString(R.string.stats_usage_time_minutes)} ")
        }
        if (seconds > 0 || (hours == 0L)) {
            // Eğer saat ve dakika sıfırsa saniyeyi göster, yoksa sadece saniyeyi
            result.append("$seconds ${getString(R.string.stats_usage_time_seconds)}")
        }

        return result.toString().trim()
    }
}