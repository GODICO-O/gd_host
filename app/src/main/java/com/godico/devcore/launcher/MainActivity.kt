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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerLibraries: Spinner
    private lateinit var btnRefresh: Button
    private lateinit var txtStatus: TextView

    private val PERMISSION_REQUEST_CODE = 1001

    private val libsDir: File by lazy {
        File(getExternalFilesDir(null)?.parentFile?.parentFile, "media/com.godico.devcore.launcher/libs").apply {
            if (!exists()) mkdirs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinnerLibraries = findViewById(R.id.spinnerLibraries)
        btnRefresh = findViewById(R.id.btnRefresh)
        txtStatus = findViewById(R.id.txtStatus)

        // Cek dan minta izin penyimpanan langsung saat aplikasi pertama kali dibuka
        checkAndRequestPermissions()

        btnRefresh.setOnClickListener {
            if (hasStoragePermission()) {
                scanLibraries()
            } else {
                checkAndRequestPermissions()
            }
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

    private fun checkAndRequestPermissions() {
        if (hasStoragePermission()) {
            scanLibraries()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        addCategory("android.intent.category.DEFAULT")
                        data = Uri.parse("package:$packageName")
                    }
                    startActivityForResult(intent, PERMISSION_REQUEST_CODE)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, PERMISSION_REQUEST_CODE)
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
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                scanLibraries()
            } else {
                txtStatus.text = "Status: Izin penyimpanan ditolak!\nHarap berikan izin file untuk melanjutkan."
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasStoragePermission()) {
                scanLibraries()
            } else {
                txtStatus.text = "Status: Izin All Files Access ditolak!"
            }
        }
    }

    private fun scanLibraries() {
        if (!libsDir.exists()) {
            libsDir.mkdirs()
        }

        val soFiles = libsDir.listFiles { _, name -> name.endsWith(".so", ignoreCase = true) }

        if (soFiles.isNullOrEmpty()) {
            val emptyList = listOf("Tidak ada file .so ditemukan")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, emptyList)
            spinnerLibraries.adapter = adapter
            txtStatus.text = "Status: Folder libs/ kosong!\nPath: ${libsDir.absolutePath}"
        } else {
            val fileNames = soFiles.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fileNames)
            spinnerLibraries.adapter = adapter
            txtStatus.text = "Status: Ditemukan ${soFiles.size} library .so\nLocation: ${libsDir.absolutePath}"
        }
    }
}
