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

        val statusText = TextView(this).apply {
            text = "Status: Core belum di-load\nPath: /sdcard/GDLauncher/core/libgd_launcher_core.so"
            textSize = 16f
        }

        val btnLoad = Button(this).apply {
            text = "LOAD / RELOAD CORE (.SO)"
            setOnClickListener {
                val corePath = "/sdcard/GDLauncher/core/libgd_launcher_core.so"
                val coreFile = File(corePath)

                if (!coreFile.exists()) {
                    Toast.makeText(context, "File tidak ada di $corePath", Toast.LENGTH_LONG).show()
                    statusText.text = "Error: File .so tidak ditemukan!"
                    return@setOnClickListener
                }

                try {
                    System.load(coreFile.absolutePath)
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
