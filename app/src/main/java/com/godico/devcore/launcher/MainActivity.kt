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

    // Flag: mencegah infinite loop di onResume
    // true  = baru kembali dari layar Settings permission
    // false = resume biasa (rotate screen, dll)
    private var returningFromSettings = false

    companion object {
        private const val REQ_LEGACY_STORAGE = 1001

        // Sub-folder yang dibuat otomatis setelah izin aktif
        private val REQUIRED_DIRS = listOf("libs", "assets", "logs")
    }

    // Path dasar: /sdcard/Android/media/com.godico.devcore.launcher/
    // Dibangun manual — tidak pakai parentFile chain yang rawan null
    private val appMediaDir: File
        get() = File(
            Environment.getExternalStorageDirectory(),
            "Android/media/$packageName"
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinnerLibraries = findViewById(R.id.spinnerLibraries)
        btnRefresh       = findViewById(R.id.btnRefresh)
        txtStatus        = findViewById(R.id.txtStatus)

        btnRefresh.setOnClickListener { handleScan() }

        // Scan pertama saat app dibuka
        handleScan()
    }

    override fun onResume() {
        super.onResume()
        // Hanya re-scan kalau memang baru balik dari Settings
        // Mencegah infinite loop onResume → Settings → onResume
        if (returningFromSettings) {
            returningFromSettings = false
            handleScan()
        }
    }

    // ─── Entry point utama ───────────────────────────────────────────────────

    private fun handleScan() {
        when {
            hasStoragePermission() -> doScan()
            else                   -> requestStoragePermission()
        }
    }

    // ─── Cek izin berdasarkan versi Android ─────────────────────────────────

    private fun hasStoragePermission(): Boolean {
        return when {
            // Android 11+ (API 30+): butuh MANAGE_ALL_FILES
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                Environment.isExternalStorageManager()

            // Android 6–10: cek READ_EXTERNAL_STORAGE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

            // Android 5 ke bawah: selalu granted
            else -> true
        }
    }

    // ─── Minta izin sesuai versi ─────────────────────────────────────────────

    private fun requestStoragePermission() {
        setStatus("Meminta izin penyimpanan...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: arahkan ke halaman Settings khusus
            // Set flag SEBELUM startActivity agar onResume tahu kita balik dari Settings
            returningFromSettings = true
            try {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            } catch (e: Exception) {
                // Fallback: beberapa ROM (ColorOS, MIUI) tidak support intent spesifik
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (e2: Exception) {
                    // ROM sangat custom — arahkan ke Settings app secara manual
                    returningFromSettings = true
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                    setStatus(
                        "Buka: Izin → File & Media → Izinkan kelola semua file\n" +
                        "Lalu kembali dan tap SCAN"
                    )
                }
            }
        } else {
            // Android 6–10: dialog runtime permission biasa
            // onResume TIDAK dipakai untuk ini — pakai callback onRequestPermissionsResult
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQ_LEGACY_STORAGE
            )
        }
    }

    // ─── Scan & buat folder ──────────────────────────────────────────────────

    private fun doScan() {
        try {
            // 1. Buat semua sub-folder yang dibutuhkan
            val createdDirs   = mutableListOf<String>()
            val existingDirs  = mutableListOf<String>()

            REQUIRED_DIRS.forEach { dirName ->
                val dir = File(appMediaDir, dirName)
                when {
                    dir.exists()   -> existingDirs.add(dirName)
                    dir.mkdirs()   -> createdDirs.add(dirName)
                    else           -> {
                        // mkdirs() gagal — storage belum benar-benar accessible
                        setStatus(
                            "Gagal membuat folder: $dirName\n" +
                            "Path: ${dir.absolutePath}\n\n" +
                            "Pastikan izin 'Kelola semua file' sudah diaktifkan,\n" +
                            "lalu tap SCAN lagi."
                        )
                        return
                    }
                }
            }

            // 2. Baca file .so di folder libs
            val libsDir  = File(appMediaDir, "libs")
            val soFiles  = libsDir
                .listFiles { _, name -> name.endsWith(".so", ignoreCase = true) }
                ?.sortedBy { it.name }
                ?: run {
                    setStatus(
                        "Tidak dapat membaca folder libs.\n" +
                        "Path: ${libsDir.absolutePath}"
                    )
                    return
                }

            // 3. Update Spinner
            val displayList = if (soFiles.isEmpty()) {
                listOf("(Tidak ada file .so)")
            } else {
                soFiles.map { it.name }
            }

            spinnerLibraries.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                displayList
            )

            // 4. Update status detail
            val dirInfo = buildString {
                REQUIRED_DIRS.forEach { name ->
                    val icon = if (name in createdDirs) "✓ dibuat" else "✓ ada"
                    appendLine("  $icon: $name/")
                }
            }

            setStatus(
                "✅ Izin OK\n" +
                "Path: ${appMediaDir.absolutePath}\n\n" +
                "Folder:\n$dirInfo\n" +
                "Library .so: ${soFiles.size} file ditemukan"
            )

        } catch (e: SecurityException) {
            // Masih kena SecurityException walau isExternalStorageManager() = true
            // Ini bug ColorOS terkenal — storage provider belum ready
            setStatus(
                "SecurityException — izin ada tapi storage belum siap.\n" +
                "Coba: restart aplikasi atau cabut-pasang izin di Settings.\n\n" +
                "Detail: ${e.message}"
            )
        } catch (e: Exception) {
            setStatus("Error tidak terduga:\n${e.message}")
        }
    }

    // ─── Helper UI ───────────────────────────────────────────────────────────

    private fun setStatus(msg: String) {
        txtStatus.text = msg
    }

    // ─── Callback permission Android 6–10 ───────────────────────────────────

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LEGACY_STORAGE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doScan()
            } else {
                setStatus(
                    "Izin ditolak.\n" +
                    "Tap SCAN untuk coba lagi atau buka Settings secara manual."
                )
            }
        }
    }
}
