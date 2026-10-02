package com.godico.devcore.launcher

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
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
    private lateinit var chkPassiveDependencies: CheckBox
    private lateinit var btnExecute: Button
    private lateinit var txtStatus: TextView

    private val libraryList = mutableListOf<LibraryItem>()
    private lateinit var adapter: LibraryAdapter

    private val baseMediaDir: File?
        get() = externalMediaDirs.firstOrNull()

    private val libsDir: File
        get() = File(baseMediaDir, "libs")

    private val logsDir: File
        get() = File(baseMediaDir, "logs")

    private val saveDir: File
        get() = File(filesDir, "save")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listViewLibraries = findViewById(R.id.listViewLibraries)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnMoveUp = findViewById(R.id.btnMoveUp)
        btnMoveDown = findViewById(R.id.btnMoveDown)
        chkPassiveDependencies = findViewById(R.id.chkPassiveDependencies)
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
            }
        }

        btnMoveDown.setOnClickListener {
            if (adapter.selectedPosition != -1) {
                adapter.moveDown(adapter.selectedPosition)
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
                if (!logsDir.exists()) logsDir.mkdirs()
            }
            if (!saveDir.exists()) saveDir.mkdirs()
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
        logStringBuilder.append("=== HOOKED EXECUTION RUN AT $timeStamp ===\n")

        txtStatus.text = "Status: Salin file .so ke cache internal (Bypass Namespace Restriction)..."

        // 1. Salin SEMUA file .so ke cacheDir privat untuk bypass namespace restrictions
        val allSoInLibs = libsDir.listFiles { _, name -> name.endsWith(".so", ignoreCase = true) }
        allSoInLibs?.forEach { source ->
            val target = File(cacheDir, source.name)
            copyFile(source, target)
            target.setExecutable(true, false)
            target.setReadable(true, false)
        }

        // 2. Eksekusi Berurutan Sesuai Selection Queue (#1, #2, #3, dst)
        var successCount = 0
        val totalCount = selectedQueue.size

        for ((index, item) in selectedQueue.withIndex()) {
            val targetSoFile = File(cacheDir, item.fileName)
            val stepInfo = "[${index + 1}/$totalCount] System.load: ${item.fileName}"

            try {
                // Memuat Native Shared Object
                System.load(targetSoFile.absolutePath)
                successCount++
                logStringBuilder.append("SUCCESS: $stepInfo\n")

                // JIKA COCOS2DCPP BARUSAN DI-LOAD -> JALANKAN GOT HOOK ENGINE!
                if (item.fileName.contains("cocos2dcpp", ignoreCase = true)) {
                    val isHooked = LauncherFix.initHookEngine(saveDir.absolutePath)
                    if (isHooked) {
                        logStringBuilder.append("HOOK STATUS: Native GOT fopen/rename Redirect Applied Successfully -> ${saveDir.absolutePath}\n")
                    } else {
                        logStringBuilder.append("HOOK STATUS: GOT Hooking Warning (Address base 0).\n")
                    }
                }

            } catch (e: Throwable) {
                val errorMsg = "FAILED: $stepInfo -> ${e.localizedMessage ?: e.message}\n"
                logStringBuilder.append(errorMsg)
                writeLog(logStringBuilder.toString())

                txtStatus.text = "Status EKSEKUSI TERHENTI di (#${index + 1}):\n" +
                        "Failed file: ${item.fileName}\n" +
                        "Error: ${e.localizedMessage ?: e.message}"
                return
            }
        }

        logStringBuilder.append("RESULT: $successCount/$totalCount libraries loaded & hooked successfully.\n\n")
        writeLog(logStringBuilder.toString())

        val loadedNames = selectedQueue.joinToString("\n") { " -> ${it.fileName}" }
        txtStatus.text = "Status: EKSEKUSI DENGAN HOOKING BERHASIL! ($successCount/$totalCount)\n" +
                "Urutan Load:\n$loadedNames\n" +
                "Save Redirect: ${saveDir.absolutePath}"
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
