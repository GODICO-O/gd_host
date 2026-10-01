package com.godico.devcore.launcher

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerLibraries: Spinner
    private lateinit var btnRefresh: Button
    private lateinit var txtStatus: TextView

    // Dapatkan folder media resmi app: /sdcard/Android/media/com.godico.devcore.launcher/
    private val baseMediaDir: File?
        get() = externalMediaDirs.firstOrNull()

    private val libsDir: File
        get() = File(baseMediaDir, "libs")

    private val assetsDir: File
        get() = File(baseMediaDir, "assets")

    private val logsDir: File
        get() = File(baseMediaDir, "logs")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinnerLibraries = findViewById(R.id.spinnerLibraries)
        btnRefresh = findViewById(R.id.btnRefresh)
        txtStatus = findViewById(R.id.txtStatus)

        // Inisialisasi folder workspace
        initWorkspaceFolders()

        btnRefresh.setOnClickListener {
            scanLibraries()
        }
    }

    override fun onResume() {
        super.onResume()
        scanLibraries()
    }

    /**
     * Membuat folder libs, assets, dan logs di Android/media/com.godico.devcore.launcher/
     */
    private fun initWorkspaceFolders() {
        try {
            if (baseMediaDir != null) {
                if (!libsDir.exists()) libsDir.mkdirs()
                if (!assetsDir.exists()) assetsDir.mkdirs()
                if (!logsDir.exists()) logsDir.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Memindai file .so di folder libs/
     */
    private fun scanLibraries() {
        initWorkspaceFolders()

        if (baseMediaDir == null || !libsDir.exists()) {
            txtStatus.text = "Status: Gagal mengakses direktori media!"
            return
        }

        val soFiles = libsDir.listFiles { _, name -> name.endsWith(".so", ignoreCase = true) }

        if (soFiles.isNullOrEmpty()) {
            val emptyList = listOf("Tidak ada file .so ditemukan")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, emptyList)
            spinnerLibraries.adapter = adapter
            txtStatus.text = "Status: Workspace Siap (libs kosong)\nPath: ${libsDir.absolutePath}"
        } else {
            val fileNames = soFiles.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fileNames)
            spinnerLibraries.adapter = adapter
            txtStatus.text = "Status: Ditemukan ${soFiles.size} library .so\nPath: ${libsDir.absolutePath}"
        }
    }
}
