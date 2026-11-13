package me.jegxiao.navigationswitch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NEED_PERMISSION = "extra_need_permission"
    }

    private lateinit var scrollView: ScrollView
    private lateinit var modeIcon: ImageView
    private lateinit var modeTitle: TextView
    private lateinit var modeDesc: TextView
    private lateinit var switchButton: MaterialButton
    private lateinit var permissionCard: MaterialCardView
    private lateinit var adbCommand: TextView
    private lateinit var copyButton: MaterialButton
    private lateinit var recheckButton: MaterialButton
    private lateinit var rootButton: MaterialButton
    private lateinit var shizukuCard: MaterialCardView
    private lateinit var shizukuStatus: TextView
    private lateinit var openShizukuButton: MaterialButton
    private lateinit var grantShizukuButton: MaterialButton

    private var switching = false

    private val modeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refreshUi()
        }
    }

    private val binderReceivedListener = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            refreshUi()
        }
    }

    private val binderDeadListener = object : Shizuku.OnBinderDeadListener {
        override fun onBinderDead() {
            refreshUi()
        }
    }

    private val permissionResultListener = object : Shizuku.OnRequestPermissionResultListener {
        override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
            if (requestCode != ShizukuHelper.PERMISSION_REQUEST_CODE) return
            refreshUi()
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                toast(R.string.toast_shizuku_granted)
                NavigationWidgetProvider.updateAll(this@MainActivity)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupEdgeToEdge()

        adbCommand.text = getString(R.string.adb_command, packageName)
        copyButton.setOnClickListener { copyAdbCommand() }
        recheckButton.setOnClickListener {
            refreshUi()
            toast(if (NavModeManager.canWriteSecureSettings(this)) R.string.toast_granted else R.string.toast_still_missing)
        }
        rootButton.setOnClickListener { requestRootGrant() }
        openShizukuButton.setOnClickListener { openShizuku() }
        grantShizukuButton.setOnClickListener { grantShizukuPermission() }
        switchButton.setOnClickListener { performSwitch() }

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        refreshUi()
        if (intent.getBooleanExtra(EXTRA_NEED_PERMISSION, false) && !NavModeManager.canToggle(this)) {
            if (NavModeManager.isHonorFamily()) scrollToShizuku() else scrollToPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        contentResolver.registerContentObserver(NavModeManager.settingUri(), false, modeObserver)
        refreshUi()
        NavigationWidgetProvider.updateAll(this)
    }

    override fun onPause() {
        contentResolver.unregisterContentObserver(modeObserver)
        super.onPause()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        super.onDestroy()
    }

    private fun bindViews() {
        scrollView = findViewById(R.id.root_scroll)
        modeIcon = findViewById(R.id.mode_icon)
        modeTitle = findViewById(R.id.mode_title)
        modeDesc = findViewById(R.id.mode_desc)
        switchButton = findViewById(R.id.switch_button)
        permissionCard = findViewById(R.id.permission_card)
        adbCommand = findViewById(R.id.adb_command)
        copyButton = findViewById(R.id.copy_button)
        recheckButton = findViewById(R.id.recheck_button)
        rootButton = findViewById(R.id.root_button)
        shizukuCard = findViewById(R.id.shizuku_card)
        shizukuStatus = findViewById(R.id.shizuku_status)
        openShizukuButton = findViewById(R.id.open_shizuku_button)
        grantShizukuButton = findViewById(R.id.grant_shizuku_button)
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Below API 27 the navigation bar cannot use dark icons; keep a dark bar there.
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            window.navigationBarColor = Color.BLACK
        }

        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }

        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun refreshUi() {
        val supported = NavModeManager.isSupported()
        val honor = NavModeManager.isHonorFamily()
        val mode = NavModeManager.currentMode(this)
        val gesture = mode == NavMode.GESTURE

        shizukuCard.isVisible = supported && honor
        permissionCard.isVisible = supported && !honor && !NavModeManager.canWriteSecureSettings(this)

        switchButton.isVisible = supported
        if (!switching) switchButton.isEnabled = supported

        if (supported) {
            modeIcon.setImageResource(
                if (gesture) R.drawable.ic_nav_gesture else R.drawable.ic_nav_three
            )
            modeTitle.setText(NavModeManager.modeNameRes(mode))
            modeDesc.setText(
                when (mode) {
                    NavMode.GESTURE -> R.string.mode_gesture_desc
                    NavMode.TWO_BUTTON -> R.string.mode_two_desc
                    NavMode.THREE_BUTTON -> R.string.mode_three_desc
                }
            )
            switchButton.setText(if (gesture) R.string.switch_to_three else R.string.switch_to_gesture)
        } else {
            modeIcon.setImageResource(R.drawable.ic_nav_switch)
            modeTitle.setText(R.string.unsupported_title)
            modeDesc.setText(R.string.unsupported_desc)
        }

        if (supported && honor) {
            val alive = ShizukuHelper.isBinderAlive()
            val granted = ShizukuHelper.isPermissionGranted()
            shizukuStatus.setText(
                when {
                    granted -> R.string.shizuku_status_granted
                    alive -> R.string.shizuku_status_running
                    else -> R.string.shizuku_status_not_running
                }
            )
            grantShizukuButton.isEnabled = alive && !granted
            grantShizukuButton.setText(
                if (granted) R.string.shizuku_granted else R.string.shizuku_grant
            )
        }
    }

    private fun performSwitch() {
        if (switching) return
        switching = true
        switchButton.isEnabled = false

        Thread {
            val result = NavModeManager.switchMode(applicationContext)
            runOnUiThread {
                switching = false
                switchButton.isEnabled = NavModeManager.isSupported()
                handleSwitchResult(result)
            }
        }.start()
    }

    private fun handleSwitchResult(result: SwitchResult) {
        when (result) {
            is SwitchResult.Success -> {
                refreshUi()
                NavigationWidgetProvider.updateAll(this)
                toast(
                    if (result.mode == NavMode.GESTURE) R.string.toast_switched_gesture
                    else R.string.toast_switched_three
                )
            }

            SwitchResult.NoPermission -> {
                refreshUi()
                toast(R.string.toast_need_permission)
                scrollToPermission()
            }

            SwitchResult.NeedsShizuku -> {
                refreshUi()
                toast(R.string.toast_need_shizuku)
                scrollToShizuku()
            }

            SwitchResult.Unsupported -> toast(R.string.toast_unsupported)

            is SwitchResult.Failed -> toast(getString(R.string.toast_failed, result.reason))
        }
    }

    private fun requestRootGrant() {
        rootButton.isEnabled = false
        toast(R.string.toast_root_trying)
        Thread {
            val granted = RootShell.grantSecureSettings(applicationContext)
            runOnUiThread {
                rootButton.isEnabled = true
                refreshUi()
                NavigationWidgetProvider.updateAll(this)
                toast(if (granted) R.string.toast_root_granted else R.string.toast_root_failed)
            }
        }.start()
    }

    private fun openShizuku() {
        val launch = packageManager.getLaunchIntentForPackage(ShizukuHelper.PACKAGE_NAME)
        if (launch != null) {
            startActivity(launch)
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/")))
        } catch (_: Exception) {
            toast(R.string.toast_shizuku_not_installed)
        }
    }

    private fun grantShizukuPermission() {
        if (ShizukuHelper.isPermissionGranted()) {
            toast(R.string.toast_shizuku_granted)
            return
        }
        if (!ShizukuHelper.isBinderAlive()) {
            toast(R.string.toast_shizuku_not_running)
            refreshUi()
            return
        }
        ShizukuHelper.requestPermission()
    }

    private fun copyAdbCommand() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), adbCommand.text))
        toast(R.string.toast_copied)
    }

    private fun scrollToPermission() {
        scrollView.post { scrollView.smoothScrollTo(0, permissionCard.top) }
    }

    private fun scrollToShizuku() {
        scrollView.post { scrollView.smoothScrollTo(0, shizukuCard.top) }
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    private fun toast(text: CharSequence) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}