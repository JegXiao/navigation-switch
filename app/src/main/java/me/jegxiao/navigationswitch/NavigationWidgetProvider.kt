package me.jegxiao.navigationswitch

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast

/**
 * 1x1 home screen widget: one tap toggles 3-button <-> gesture navigation.
 *
 * When the app cannot toggle yet (missing adb permission, or Honor without Shizuku), the tap
 * opens [MainActivity] at the corresponding setup step.
 */
class NavigationWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE) {
            toggle(context)
            updateAll(context)
        } else {
            super.onReceive(context, intent)
        }
    }

    private fun toggle(context: Context) {
        when (val result = NavModeManager.switchMode(context)) {
            is SwitchResult.Success -> Toast.makeText(
                context,
                if (result.mode == NavMode.GESTURE) R.string.toast_switched_gesture
                else R.string.toast_switched_three,
                Toast.LENGTH_SHORT
            ).show()

            SwitchResult.NoPermission, SwitchResult.NeedsShizuku, SwitchResult.Unsupported ->
                openApp(context)

            is SwitchResult.Failed ->
                Toast.makeText(context, result.reason, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openApp(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_NEED_PERMISSION, true)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, R.string.toast_need_permission, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ACTION_TOGGLE = "me.jegxiao.navigationswitch.action.TOGGLE"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, NavigationWidgetProvider::class.java)
            )
            ids.forEach { render(context, manager, it) }
        }

        private fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val supported = NavModeManager.isSupported()
            val canToggle = NavModeManager.canToggle(context)
            val mode = NavModeManager.currentMode(context)
            val gesture = mode == NavMode.GESTURE

            val iconRes = if (canToggle) {
                if (gesture) R.drawable.ic_nav_gesture else R.drawable.ic_nav_three
            } else {
                R.drawable.ic_nav_switch
            }
            val labelRes = when {
                !supported -> R.string.widget_unsupported
                !canToggle -> R.string.widget_grant
                else -> NavModeManager.modeShortNameRes(mode)
            }

            val views = RemoteViews(context.packageName, R.layout.widget_navigation)
            views.setImageViewResource(R.id.widget_icon, iconRes)
            views.setTextViewText(R.id.widget_label, context.getString(labelRes))
            views.setOnClickPendingIntent(
                R.id.widget_root,
                if (canToggle) toggleIntent(context) else setupIntent(context)
            )
            manager.updateAppWidget(widgetId, views)
        }

        private fun toggleIntent(context: Context): PendingIntent {
            val intent = Intent(context, NavigationWidgetProvider::class.java)
                .setAction(ACTION_TOGGLE)
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun setupIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_NEED_PERMISSION, true)
            }
            return PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}