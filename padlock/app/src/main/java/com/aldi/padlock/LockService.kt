package com.aldi.padlock

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.service.quicksettings.TileService
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Service aksesibilitas yang:
 *  1. memasang overlay layar penuh yang menelan semua sentuhan,
 *  2. mendengarkan tombol gamepad (Start + Select ditahan 2 detik) untuk toggle kunci,
 *  3. otomatis membuka kunci saat gamepad terputus.
 *
 * Event tombol/joystick gamepad tidak lewat jendela overlay, jadi tetap sampai ke aplikasi.
 */
class LockService : AccessibilityService(), InputManager.InputDeviceListener {

    companion object {
        @Volatile
        var instance: LockService? = null

        /** Lama Start+Select harus ditahan untuk toggle. */
        private const val HOLD_MS = 2000L

        /** Kombinasi tombol; ubah di sini jika gamepad kamu memakai kode lain. */
        private const val KEY_A = KeyEvent.KEYCODE_BUTTON_START
        private const val KEY_B = KeyEvent.KEYCODE_BUTTON_SELECT

        fun isLocked(): Boolean = instance?.locked == true
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private lateinit var im: InputManager
    private var overlay: View? = null
    private var aDown = false
    private var bDown = false

    val locked: Boolean get() = overlay != null

    private val comboRunnable = Runnable { toggle() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        im = getSystemService(Context.INPUT_SERVICE) as InputManager
        im.registerInputDeviceListener(this, handler)
        refreshTile()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // tidak dipakai
    }

    override fun onInterrupt() {
        // tidak dipakai
    }

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
        if (::im.isInitialized) im.unregisterInputDeviceListener(this)
        instance = null
        refreshTile()
        return super.onUnbind(intent)
    }

    // ---------------------------------------------------------------- tombol gamepad

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (code != KEY_A && code != KEY_B) return false

        val down = event.action == KeyEvent.ACTION_DOWN
        // abaikan auto-repeat saat tombol ditahan
        if (down && event.repeatCount > 0) return false

        if (code == KEY_A) aDown = down else bDown = down

        handler.removeCallbacks(comboRunnable)
        if (aDown && bDown) handler.postDelayed(comboRunnable, HOLD_MS)

        // false = event tetap diteruskan ke aplikasi
        return false
    }

    // ---------------------------------------------------------------- lock / unlock

    fun toggle() {
        if (locked) unlock(getString(R.string.toast_unlocked)) else lock()
    }

    private fun lock() {
        if (!hasGamepad()) {
            toast(getString(R.string.toast_no_pad))
            return
        }
        showOverlay()
        feedback()
        toast(getString(R.string.toast_locked))
        refreshTile()
    }

    private fun unlock(message: String) {
        removeOverlay()
        feedback()
        toast(message)
        refreshTile()
    }

    private fun showOverlay() {
        if (overlay != null) return
        val view = View(this)
        view.setOnTouchListener { _, _ -> true } // telan semua sentuhan

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        wm.addView(view, lp)
        overlay = view
    }

    private fun removeOverlay() {
        val v = overlay ?: return
        try {
            wm.removeView(v)
        } catch (_: Exception) {
        }
        overlay = null
    }

    // ---------------------------------------------------------------- deteksi gamepad

    private fun hasGamepad(): Boolean {
        for (id in InputDevice.getDeviceIds()) {
            val d = InputDevice.getDevice(id) ?: continue
            if (d.isVirtual) continue
            val s = d.sources
            val isPad = (s and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (s and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if (isPad) return true
        }
        return false
    }

    override fun onInputDeviceAdded(deviceId: Int) {}

    override fun onInputDeviceChanged(deviceId: Int) {}

    override fun onInputDeviceRemoved(deviceId: Int) {
        aDown = false
        bDown = false
        handler.removeCallbacks(comboRunnable)
        // beri jeda singkat supaya daftar device sudah diperbarui sistem
        handler.postDelayed({
            if (locked && !hasGamepad()) unlock(getString(R.string.toast_pad_lost))
        }, 300)
    }

    // ---------------------------------------------------------------- util

    private fun feedback() {
        try {
            @Suppress("DEPRECATION")
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(80)
        } catch (_: Exception) {
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    private fun refreshTile() {
        try {
            TileService.requestListeningState(
                this, ComponentName(this, LockTileService::class.java)
            )
        } catch (_: Exception) {
        }
    }
}
