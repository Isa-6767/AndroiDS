package com.androids.launcher

import android.app.Activity
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

/** Pantalla de arranque "AndroiDS" que se muestra al desbloquear el teléfono. */
class SplashActivity : Activity() {

    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(StartupView(this) { closeSplash() })
        hideBars()
        playChime()
    }

    private fun closeSplash() {
        if (finished) return
        finished = true
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    @Suppress("DEPRECATION")
    private fun hideBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    /** Un "ding" corto y propio (respeta el modo silencio porque usa el canal de notificaciones). */
    private fun playChime() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 700)
        } catch (e: Exception) {
            // Sin sonido, no pasa nada.
        }
    }
}
