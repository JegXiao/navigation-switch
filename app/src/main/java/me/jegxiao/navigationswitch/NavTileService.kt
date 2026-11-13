package me.jegxiao.navigationswitch

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/** Quick Settings tile: one tap toggles 3-button <-> gesture navigation. */
class NavTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        when (val result = NavModeManager.switchMode(this)) {
            is SwitchResult.Success -> {
                updateTile()
                NavigationWidgetProvider.updateAll(this)
            }

            is SwitchResult.Failed ->
                Toast.makeText(this, result.reason, Toast.LENGTH_SHORT).show()

            else -> openApp()
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val supported = NavModeManager.isSupported()
        val canToggle = NavModeManager.canToggle(this)
        val honor = NavModeManager.isHonorFamily()
        val mode = NavModeManager.currentMode(this)
        val gesture = mode == NavMode.GESTURE

        tile.state = if (canToggle && gesture) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.contentDescription = getString(R.string.tile_content_desc)
        tile.icon = Icon.createWithResource(
            this,
            if (canToggle) {
                if (gesture) R.drawable.ic_nav_gesture else R.drawable.ic_nav_three
            } else {
                R.drawable.ic_nav_switch
            }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !supported -> getString(R.string.unsupported_title)
                !canToggle && honor -> getString(R.string.tile_need_shizuku)
                !canToggle -> getString(R.string.widget_grant)
                else -> getString(NavModeManager.modeNameRes(mode))
            }
        }
        tile.updateTile()
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_NEED_PERMISSION, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}