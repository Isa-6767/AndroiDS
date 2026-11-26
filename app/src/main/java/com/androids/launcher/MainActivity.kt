package com.androids.launcher

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppEntry(
    val label: String,
    val pkg: String,
    val cls: String,
    val icon: Drawable
)

class MainActivity : Activity() {

    private lateinit var clock: TextView
    private lateinit var dateView: TextView
    private val adapter = AppAdapter()
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            val now = Date()
            clock.text = DateFormat.getTimeFormat(this@MainActivity).format(now)
            val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM")
            dateView.text = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clock = findViewById(R.id.clock)
        dateView = findViewById(R.id.date)

        // Pantalla de arriba: toque = repetir el arranque, toque largo = revisar permisos.
        val top = findViewById<View>(R.id.topScreen)
        top.setOnClickListener { startActivity(Intent(this, SplashActivity::class.java)) }
        top.setOnLongClickListener {
            setup(force = true)
            true
        }

        val grid = findViewById<GridView>(R.id.grid)
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, position, _ ->
            val app = adapter.items[position]
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(app.pkg, app.cls)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir ${app.label}", Toast.LENGTH_SHORT).show()
            }
        }
        grid.setOnItemLongClickListener { _, _, position, _ ->
            val app = adapter.items[position]
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}"))
            )
            true
        }

        setup(force = false)
    }

    override fun onResume() {
        super.onResume()
        loadApps()
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        setup(force = false)
    }

    /** Arranca el servicio que escucha el desbloqueo y pide los permisos necesarios. */
    private fun setup(force: Boolean) {
        try {
            startForegroundService(Intent(this, UnlockService::class.java))
        } catch (e: Exception) {
            // Si el sistema no lo permite ahora, se reintenta la próxima vez que abras el launcher.
        }

        val prefs = getSharedPreferences("androids", MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            (force || !prefs.getBoolean("asked_notif", false))
        ) {
            prefs.edit().putBoolean("asked_notif", true).apply()
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            return
        }

        if (!Settings.canDrawOverlays(this) &&
            (force || !prefs.getBoolean("asked_overlay", false))
        ) {
            prefs.edit().putBoolean("asked_overlay", true).apply()
            Toast.makeText(
                this,
                "Activa \"Mostrar sobre otras apps\" para el arranque al desbloquear",
                Toast.LENGTH_LONG
            ).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = pm.queryIntentActivities(query, 0)
                .filter { it.activityInfo.packageName != packageName }
                .map {
                    AppEntry(
                        label = it.loadLabel(pm).toString(),
                        pkg = it.activityInfo.packageName,
                        cls = it.activityInfo.name,
                        icon = it.loadIcon(pm)
                    )
                }
                .sortedBy { it.label.lowercase() }
            runOnUiThread { adapter.submit(list) }
        }.start()
    }

    inner class AppAdapter : BaseAdapter() {
        var items: List<AppEntry> = emptyList()
            private set

        fun submit(list: List<AppEntry>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            val app = items[position]
            view.findViewById<ImageView>(R.id.icon).setImageDrawable(app.icon)
            view.findViewById<TextView>(R.id.label).text = app.label
            return view
        }
    }
}
