package com.example.cloakvault

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class IntruderCapture(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {

    private var imageCapture: ImageCapture? = null

    init {
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview (Hidden 1x1 pixel)
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            // Select Front Camera (Selfie)
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("IntruderCapture", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun captureIntruder() {
        val imageCapture = imageCapture ?: return

        // Create timestamped file
        val filename = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"

        val photoFile = File(context.externalCacheDir, filename)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("IntruderCapture", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Upload to Firebase immediately
                    uploadToFirebase(photoFile, filename)
                }
            }
        )
    }

    private fun uploadToFirebase(file: File, filename: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("intruders/$filename")
        val uri = android.net.Uri.fromFile(file)

        storageRef.putFile(uri).addOnSuccessListener {
            // Success - Silent log
            Log.d("IntruderCapture", "Intruder logged: $filename")
            file.delete() // Clean up local file
        }.addOnFailureListener {
            Log.e("IntruderCapture", "Upload failed")
        }
    }
}