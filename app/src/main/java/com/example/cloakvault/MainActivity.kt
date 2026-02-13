package com.example.cloakvault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var intruderCapture: IntruderCapture
    private var failedAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize Intruder Trap (Silent Camera)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        } else {
            initIntruderSystem() // <--- Now this will work because the function exists below!
        }

        // 2. Setup Biometrics
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = createBiometricPrompt(executor)

        findViewById<Button>(R.id.btn_auth).setOnClickListener {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("ACCESS REQUESTED")
                .setSubtitle("Confirm Identity")
                .setNegativeButtonText("CANCEL")
                .build()
            biometricPrompt.authenticate(promptInfo)
        }

        // 3. Setup Manual Override (Duress Code)
        findViewById<Button>(R.id.btn_duress).setOnClickListener {
            showPasscodeDialog()
        }
    }

    // --- THIS IS THE MISSING FUNCTION YOU NEEDED ---
    private fun initIntruderSystem() {
        val previewView = findViewById<PreviewView>(R.id.hidden_preview)
        // Connects to the IntruderCapture.kt file we created earlier
        intruderCapture = IntruderCapture(this, this, previewView)
    }
    // ----------------------------------------------

    private fun createBiometricPrompt(executor: Executor): BiometricPrompt {
        return BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                failedAttempts = 0
                openVault(isDuress = false)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                failedAttempts++
                // TRIGGER TRAP: Take silent selfie on failure
                if (failedAttempts >= 1) {
                    // Check if camera is ready before trying to snap
                    if (::intruderCapture.isInitialized) {
                        intruderCapture.captureIntruder()
                        Toast.makeText(applicationContext, "AUTH_FAIL: INCIDENT LOGGED", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun showPasscodeDialog() {
        val input = EditText(this)
        input.hint = "ENTER SECURITY CODE"
        input.textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER

        AlertDialog.Builder(this)
            .setTitle("MANUAL OVERRIDE")
            .setView(input)
            .setPositiveButton("UNLOCK") { _, _ ->
                val code = input.text.toString()
                when (code) {
                    "1234" -> openVault(isDuress = false) // REAL PASSWORD
                    "666" -> openVault(isDuress = true)   // PANIC PASSWORD
                    else -> {
                        Toast.makeText(this, "INVALID CODE", Toast.LENGTH_SHORT).show()
                        if (::intruderCapture.isInitialized) {
                            intruderCapture.captureIntruder() // Wrong password = Selfie
                        }
                    }
                }
            }
            .show()
    }

    private fun openVault(isDuress: Boolean) {
        val intent = Intent(this, VaultActivity::class.java)
        intent.putExtra("DURESS_MODE", isDuress)
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initIntruderSystem()
        }
    }
}