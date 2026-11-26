package com.androids.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Reinicia el servicio de desbloqueo después de encender el cel o actualizar la app. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            context.startForegroundService(Intent(context, UnlockService::class.java))
        } catch (e: Exception) {
            // Si el sistema lo bloquea, se reinicia al abrir el launcher.
        }
    }
}
