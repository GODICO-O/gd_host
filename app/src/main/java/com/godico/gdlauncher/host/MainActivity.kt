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

        // Target path langsung di folder privat aplikasi (Bypass SELinux/Permission)
        val privateCoreFile = File(filesDir, "libgd_launcher_core.so")

        val statusText = TextView(this).apply {
            text = "Status: Core belum di-load\nTarget Path: ${privateCoreFile.absolutePath}"
            textSize = 15f
        }

        val btnLoad = Button(this).apply {
            text = "LOAD / RELOAD CORE (.SO)"
            setOnClickListener {
                if (!privateCoreFile.exists()) {
                    Toast.makeText(context, "File .so tidak ada di internal files!", Toast.LENGTH_LONG).show()
                    statusText.text = "Error: File tidak ditemukan di:\n${privateCoreFile.absolutePath}"
                    return@setOnClickListener
                }

                try {
                    // Langsung eksekusi dari internal privat storage aplikasi
                    System.load(privateCoreFile.absolutePath)

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
}
