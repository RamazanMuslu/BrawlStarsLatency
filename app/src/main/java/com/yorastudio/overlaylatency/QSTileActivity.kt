package com.yorastudio.overlaylatency

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.core.content.edit

class QSTileActivity : TileService() {

    private val PREF_TILE_STATE = "tile_state"

    // Başka bir sınıftan (mesela MainActivity'den) çağrılacak statik fonksiyon.
    companion object {
        fun requestTileUpdate(context: Context, newState: Boolean) {
            val prefs = context.getSharedPreferences("TilePrefs", Context.MODE_PRIVATE)
            prefs.edit { putBoolean("tile_state", newState) }

            // Tile'ın durumunu güncellemesi için sisteme sinyal gönderir.
            TileService.requestListeningState(context, ComponentName(context, QSTileActivity::class.java))
        }
    }

    override fun onClick() {
        super.onClick()

        // Overlay izni kontrolü (izin yoksa ayarlar ekranına yönlendirir)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + applicationContext.packageName)
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Bu kısım, cihazın Android sürümüne göre değişebilir. Eğer hata alırsan bu satırı ikiye böl:
            // startActivity(intent)
            // collapse()
            startActivity(intent)
        } else {
            val prefs = applicationContext.getSharedPreferences("TilePrefs", Context.MODE_PRIVATE)
            val currentState = prefs.getBoolean(PREF_TILE_STATE, false)
            val newState = !currentState

            prefs.edit().putBoolean(PREF_TILE_STATE, newState).apply()

            onTileStateChanged(newState)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val prefs = applicationContext.getSharedPreferences("TilePrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(PREF_TILE_STATE, false)

        qsTile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }

    // Hem koddan hem de basıldığında çağrılacak asıl fonksiyon
    private fun onTileStateChanged(isEnabled: Boolean) {
        toggleOverlayService(isEnabled)

        if (isEnabled) {
            qsTile.label = getString(R.string.qs_button_active)
        } else {
            qsTile.label = getString(R.string.qs_button_default)
        }
    }

    private fun toggleOverlayService(enable: Boolean) {
        val serviceIntent = Intent(this, OverlayService::class.java)

        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            // Toast.makeText(this, "Overlay aktif edildi", Toast.LENGTH_SHORT).show()
        } else {
            stopService(serviceIntent)
            // Toast.makeText(this, "Overlay devre dışı bırakıldı", Toast.LENGTH_SHORT).show()
        }
    }
}