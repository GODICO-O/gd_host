package com.godico.devcore.launcher

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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

        val sourceSoFile = File(libsDir, selectedItem)

        if (!sourceSoFile.exists()) {
            txtStatus.text = "Status ERROR: File ${sourceSoFile.name} tidak ditemukan!"
            return
        }

        txtStatus.text = "Status: Menyiapkan ${sourceSoFile.name} ke internal storage..."

        try {
            // 1. Salin file .so dari media SDCard ke folder internal app privat (melewati Linker Namespace restriction)
            val internalSoFile = File(filesDir, sourceSoFile.name)
            copyFile(sourceSoFile, internalSoFile)

            // Pastikan file executable
            internalSoFile.setExecutable(true, false)
            internalSoFile.setReadable(true, false)

            txtStatus.text = "Status: Memuat native library..."

            // 2. Load dari internal privat storage
            System.load(internalSoFile.absolutePath)

            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logMsg = "[$timeStamp] SUCCESS: Loaded ${sourceSoFile.name} from internal storage\n"
            writeLog(logMsg)

            txtStatus.text = "Status: BERHASIL MENGEKSEKUSI!\n" +
                    "Library: ${sourceSoFile.name}\n" +
                    "Loaded Path: ${internalSoFile.absolutePath}\n" +
                    "Log: ${File(logsDir, "launcher.log").absolutePath}"

        } catch (e: Throwable) {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val errorMsg = "[$timeStamp] ERROR: Failed to load ${sourceSoFile.name} -> ${e.message}\n"
            writeLog(errorMsg)

            txtStatus.text = "Status EKSEKUSI GAGAL:\n" +
                    "Error: ${e.localizedMessage ?: e.message}\n" +
                    "File: ${sourceSoFile.name}"
        }
    }

    private fun copyFile(source: File, target: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
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
