package com.godico.gdlauncher.host

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    private external fun gd_core_init(): Int
    private external fun gd_core_get_version(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Izin All Files Access (Android 11+) saat app dibuka
        checkAndRequestStoragePermission()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 120, 60, 60)
        }

        val statusText = TextView(this).apply {
            text = "Status: Core belum di-load\nPath External: /sdcard/GDLauncher/core/libgd_launcher_core.so"
            textSize = 16f
        }

        val btnLoad = Button(this).apply {
            text = "LOAD / RELOAD CORE (.SO)"
            setOnClickListener {
                val sdCardPath = "/sdcard/GDLauncher/core/libgd_launcher_core.so"
                val externalFile = File(sdCardPath)

                if (!externalFile.exists()) {
                    Toast.makeText(context, "File tidak ada di $sdCardPath", Toast.LENGTH_LONG).show()
                    statusText.text = "Error: File .so tidak ditemukan di SD Card!"
                    return@setOnClickListener
                }

                try {
                    // Copy file dari SD Card ke Internal Cache/Files Dir privat aplikasi
                    val internalFile = File(filesDir, "libgd_launcher_core.so")
                    externalFile.copyTo(internalFile, overwrite = true)

                    // Load .so dari internal storage privat (Bypass SELinux NOEXEC Restriction)
                    System.load(internalFile.absolutePath)

                    val initCode = gd_core_init()
                    val version = gd_core_get_version()
                    statusText.text = "Status: CORE SUCCESS!\nVersion: $version\nInit Code: $initCode"
                    Toast.makeText(context, "Core Berhasil Dimuat dari Internal!", Toast.LENGTH_SHORT).show()
                } catch (e: Throwable) {
                    e.printStackTrace()
                    statusText.text = "Gagal Load Core:\n${e.message}"
                }
            }
        }

        layout.addView(statusText)
        layout.addView(btnLoad)
        setContentView(layout)
    }

    private fun checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
