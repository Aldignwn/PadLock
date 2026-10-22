package com.aldi.padlock

import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/** Tile Quick Settings (Android 7+) sebagai tombol cadangan untuk kunci/buka sentuhan. */
class LockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        sync()
    }

    override fun onClick() {
        super.onClick()
        val service = LockService.instance
        if (service == null) {
            Toast.makeText(
                applicationContext,
                getString(R.string.toast_enable_service),
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivityAndCollapse(intent)
        } else {
            service.toggle()
            sync()
        }
    }

    private fun sync() {
        val tile = qsTile ?: return
        tile.state = if (LockService.isLocked()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.updateTile()
    }
}
