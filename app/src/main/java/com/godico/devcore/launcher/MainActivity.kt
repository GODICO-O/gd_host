package com.godico.devcore.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerLibraries: Spinner
    private lateinit var btnRefresh: Button
    private lateinit var txtStatus: TextView

    private val PERMISSION_REQUEST_CODE = 1001

    // Target: /sdcard/Android/media/com.godico.devcore.launcher/libs
    private val libsDir: File
        get() {
            val externalDir = getExternalFilesDir(null)
            val mediaBase = externalDir?.parentFile?.parentFile
            return File(mediaBase, "media/$packageName/libs")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinnerLibraries = findViewById(R.id.spinnerLibraries)
        btnRefresh = findViewById(R.id.btnRefresh)
        txtStatus = findViewById(R.id.txtStatus)

        btnRefresh.setOnClickListener {
            checkAndRunScan()
        }

        checkAndRunScan()
    }

    override fun onResume() {
        super.onResume()
        // Coba scan otomatis setiap kembali ke aplikasi
        tryScanDirectly()
    }

    private fun checkAndRunScan() {
        if (!tryScanDirectly()) {
            requestStoragePermission()
        }
    }

    /**
     * Mencoba membuat folder & membaca direktori secara langsung.
     * Jika berhasil, artinya izin penyimpanan aman!
     */
    private fun tryScanDirectly(): Boolean {
        return try {
            if (!libsDir.exists()) {
                libsDir.mkdirs()
            }

            val soFiles = libsDir.listFiles { _, name -> name.endsWith(".so", ignoreCase = true) }

            if (soFiles != null) {
                if (soFiles.isEmpty()) {
                    val emptyList = listOf("Tidak ada file .so ditemukan")
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, emptyList)
                    spinnerLibraries.adapter = adapter
                    txtStatus.text = "Status: Folder libs kosong!\nPath: ${libsDir.absolutePath}"
                } else {
                    val fileNames = soFiles.map { it.name }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fileNames)
                    spinnerLibraries.adapter = adapter
                    txtStatus.text = "Status: Ditemukan ${soFiles.size} library .so\nPath: ${libsDir.absolutePath}"
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun requestStoragePermission() {
        txtStatus.text = "Status: Meminta izin penyimpanan...\nKlik SCAN jika pop-up tidak muncul."

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Untuk Android 11+
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            // Untuk Android 10 ke bawah (Pop-up dialog biasa)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            tryScanDirectly()
        }
    }
}
