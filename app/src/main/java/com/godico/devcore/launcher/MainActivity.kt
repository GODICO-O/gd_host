package com.godico.devcore.launcher

import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var listViewLibraries: ListView
    private lateinit var btnRefresh: Button
    private lateinit var btnMoveUp: Button
    private lateinit var btnMoveDown: Button
    private lateinit var btnExecute: Button
    private lateinit var txtStatus: TextView

    private val libraryList = mutableListOf<LibraryItem>()
    private lateinit var adapter: LibraryAdapter

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

        listViewLibraries = findViewById(R.id.listViewLibraries)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnMoveUp = findViewById(R.id.btnMoveUp)
        btnMoveDown = findViewById(R.id.btnMoveDown)
        btnExecute = findViewById(R.id.btnExecute)
        txtStatus = findViewById(R.id.txtStatus)

        adapter = LibraryAdapter(this, libraryList)
        listViewLibraries.adapter = adapter

        initWorkspaceFolders()

        listViewLibraries.setOnItemClickListener { _, _, position, _ ->
            adapter.selectedPosition = position
            adapter.notifyDataSetChanged()
        }

        btnRefresh.setOnClickListener {
            scanLibraries()
        }

        btnMoveUp.setOnClickListener {
            if (adapter.selectedPosition != -1) {
                adapter.moveUp(adapter.selectedPosition)
            } else {
                txtStatus.text = "Status: Pilih/sentuh salah satu baris file dulu untuk dipindahkan!"
            }
        }

        btnMoveDown.setOnClickListener {
            if (adapter.selectedPosition != -1) {
                adapter.moveDown(adapter.selectedPosition)
            } else {
                txtStatus.text = "Status: Pilih/sentuh salah satu baris file dulu untuk dipindahkan!"
            }
        }

        btnExecute.setOnClickListener {
            executeSelectedLibrariesInOrder()
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

        libraryList.clear()
        adapter.clearQueue()

        if (!soFiles.isNullOrEmpty()) {
            soFiles.sortedBy { it.name }.forEach { file ->
                libraryList.add(LibraryItem(file.name))
            }
            txtStatus.text = "Status: Ditemukan ${soFiles.size} library .so\nPath: ${libsDir.absolutePath}"
        } else {
            txtStatus.text = "Status: Workspace Siap (libs kosong)\nPath: ${libsDir.absolutePath}"
        }

        adapter.notifyDataSetChanged()
    }

    private fun executeSelectedLibrariesInOrder() {
        val selectedQueue = adapter.selectedQueue

        if (selectedQueue.isEmpty()) {
            txtStatus.text = "Status ERROR: Belum ada file .so yang dicentang!"
            return
        }

        val logStringBuilder = StringBuilder()
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        logStringBuilder.append("=== EXECUTION RUN AT $timeStamp ===\n")

        txtStatus.text = "Status: Menyalin ${selectedQueue.size} file ke internal storage..."

        // 1. Copy SEMUA file .so di libsDir ke filesDir (termasuk dependency implisit)
        val allSoInLibs = libsDir.listFiles { _, name -> name.endsWith(".so", ignoreCase = true) }
        allSoInLibs?.forEach { source ->
            val target = File(filesDir, source.name)
            copyFile(source, target)
            target.setExecutable(true, false)
            target.setReadable(true, false)
        }

        // 2. Load satu per satu sesuai urutan antrean yang dipilih user (#1, #2, dst)
        var successCount = 0
        val totalCount = selectedQueue.size

        for ((index, item) in selectedQueue.withIndex()) {
            val targetSoFile = File(filesDir, item.fileName)
            val stepInfo = "[${index + 1}/$totalCount] Loading ${item.fileName}..."
            
            try {
                System.load(targetSoFile.absolutePath)
                successCount++
                logStringBuilder.append("SUCCESS: $stepInfo\n")
            } catch (e: Throwable) {
                val errorMsg = "FAILED: $stepInfo -> ${e.message}\n"
                logStringBuilder.append(errorMsg)
                
                writeLog(logStringBuilder.toString())
                txtStatus.text = "Status EKSEKUSI TERHENTI di (#${index + 1}):\n" +
                        "Failed file: ${item.fileName}\n" +
                        "Error: ${e.localizedMessage ?: e.message}"
                return
            }
        }

        logStringBuilder.append("RESULT: $successCount/$totalCount libraries loaded successfully.\n\n")
        writeLog(logStringBuilder.toString())

        val loadedNames = selectedQueue.joinToString("\n") { " -> ${it.fileName}" }
        txtStatus.text = "Status: BERHASIL MEMUAT SEMUA! ($successCount/$totalCount)\n" +
                "Urutan Load:\n$loadedNames\n" +
                "Log: ${File(logsDir, "launcher.log").absolutePath}"
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
