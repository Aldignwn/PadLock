package com.aldi.padlock

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Layar sederhana: status service, tombol ke pengaturan aksesibilitas, tombol tes kunci. */
class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        status = TextView(this).apply { textSize = 16f }
        root.addView(status)

        val settingsBtn = Button(this).apply {
            setText(R.string.btn_settings)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        root.addView(settingsBtn)

        val lockBtn = Button(this).apply {
            setText(R.string.btn_lock)
            setOnClickListener {
                // Hanya mengunci jika service aktif; membuka lagi lewat Start+Select di gamepad.
                val s = LockService.instance
                if (s != null) {
                    if (!s.locked) s.toggle()
                } else {
                    updateStatus()
                }
            }
        }
        root.addView(lockBtn)

        val info = TextView(this).apply {
            setText(R.string.info)
            textSize = 14f
            setPadding(0, pad, 0, 0)
        }
        root.addView(info)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        status.setText(
            if (LockService.instance != null) R.string.status_on else R.string.status_off
        )
    }
}
