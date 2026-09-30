package com.godico.gdlauncher.host

import android.app.Activity
import android.os.Bundle
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

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 120, 60, 60)
        }

        // Target path Geode Style: Android/media/<package_name>/
        val mediaFolder = externalMediaDirs.firstOrNull() ?: File(filesDir, "fallback")
        val coreFile = File(mediaFolder, "libgd_launcher_core.so")

        val statusText = TextView(this).apply {
            text = "Status: Core belum di-load\nTarget Path:\n${coreFile.absolutePath}"
            textSize = 14f
        }

        val btnLoad = Button(this).apply {
            text = "LOAD / RELOAD CORE (.SO)"
            setOnClickListener {
                if (!coreFile.exists()) {
                    Toast.makeText(context, "File .so belum ada di folder media!", Toast.LENGTH_LONG).show()
                    statusText.text = "Error: File tidak ditemukan di:\n${coreFile.absolutePath}"
                    return@setOnClickListener
                }

                try {
                    // Copy sebentar dari folder media ke filesDir internal sebelum di-load (opsional untuk bypass SELinux)
                    val internalFile = File(filesDir, "libgd_launcher_core.so")
                    coreFile.copyTo(internalFile, overwrite = true)

                    // Load dari internal filesDir
                    System.load(internalFile.absolutePath)

                    val initCode = gd_core_init()
                    val version = gd_core_get_version()
                    statusText.text = "Status: CORE SUCCESS!\nVersion: $version\nInit Code: $initCode"
                    Toast.makeText(context, "Core Berhasil Dimuat!", Toast.LENGTH_SHORT).show()
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
}
