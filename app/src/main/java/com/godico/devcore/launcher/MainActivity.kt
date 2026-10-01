package com.godico.devcore.launcher

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerLibraries: Spinner
    private lateinit var btnRefresh: Button
    private lateinit var btnExecute: Button
    private lateinit var txtStatus: TextView

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
        btnExecute = findViewById(R.id.btnExecute)
        txtStatus = findViewById(R.id.txtStatus)

        initWorkspaceFolders()

        btnRefresh.setOnClickListener {
            scanLibraries()
        }

        btnExecute.setOnClickListener {
            executeSelectedLibrary()
        }
    }

    override fun onResume() {
        super.onResume()
        scanLibraries()
    }

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

    private fun executeSelectedLibrary() {
        val selectedItem = spinnerLibraries.selectedItem?.toString()

        if (selectedItem.isNullOrEmpty() || selectedItem == "Tidak ada file .so ditemukan") {
            txtStatus.text = "Status ERROR: Tidak ada file .so yang dipilih!"
            return
        }

        val targetSoFile = File(libsDir, selectedItem)

        if (!targetSoFile.exists()) {
            txtStatus.text = "Status ERROR: File ${targetSoFile.name} tidak ditemukan!"
            return
        }

        txtStatus.text = "Status: Memuat ${targetSoFile.name}..."

        try {
            // Load native .so ke memori JVM
            System.load(targetSoFile.absolutePath)

            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logMsg = "[$timeStamp] SUCCESS: Loaded ${targetSoFile.name}\n"
            writeLog(logMsg)

            txtStatus.text = "Status: BERHASIL MENGEKSEKUSI!\n" +
                    "Library: ${targetSoFile.name}\n" +
                    "Log disimpan di: ${File(logsDir, "launcher.log").absolutePath}"

        } catch (e: Throwable) {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val errorMsg = "[$timeStamp] ERROR: Failed to load ${targetSoFile.name} -> ${e.message}\n"
            writeLog(errorMsg)

            txtStatus.text = "Status EKSEKUSI GAGAL:\n" +
                    "Error: ${e.localizedMessage ?: e.message}\n" +
                    "File: ${targetSoFile.name}"
        }
    }

    private fun writeLog(message: String) {
        try {
            val logFile = File(logsDir, "launcher.log")
            logFile.appendText(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
