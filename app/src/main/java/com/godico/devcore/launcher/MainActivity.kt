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

    // Mendapatkan folder: /sdcard/Android/media/com.godico.devcore.launcher/libs
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
            if (hasStoragePermission()) {
                scanLibraries()
            } else {
                requestStoragePermission()
            }
        }

        // Jalankan pengecekan pertama kali tanpa memicu looping
        checkPermissionsAndInit()
    }

    override fun onResume() {
        super.onResume()
        // Cukup coba scan jika izin SUDAH diberikan, JANGAN panggil intent request permission di sini
        if (hasStoragePermission()) {
            scanLibraries()
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkPermissionsAndInit() {
        if (hasStoragePermission()) {
            scanLibraries()
        } else {
            txtStatus.text = "Status: Butuh izin penyimpanan.\nTekan tombol SCAN untuk mengizinkan."
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    addCategory("android.intent.category.DEFAULT")
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
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

    private fun scanLibraries() {
        // Buat folder media/com.godico.devcore.launcher/libs jika belum ada
        if (!libsDir.exists()) {
            libsDir.mkdirs()
        }

        val soFiles = libsDir.listFiles { _, name -> name.endsWith(".so", ignoreCase = true) }

        if (soFiles.isNullOrEmpty()) {
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
    }
}
