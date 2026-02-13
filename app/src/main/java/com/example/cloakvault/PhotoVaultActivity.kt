package com.example.cloakvault

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID

class PhotoVaultActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val crypto = CryptoManager()

    private var adapter: FirestoreRecyclerAdapter<SecurePhoto, PhotoViewHolder>? = null
    private var isDuressMode = false
    private var storagePath = "secure_photos"

    // --- 1. GALLERY PICKER ---
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadFromGallery(uri)
        }
    }

    // --- 2. CAMERA CAPTURE ---
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                uploadFromCamera(bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_vault)

        isDuressMode = intent.getBooleanExtra("DURESS_MODE", false)
        val header = findViewById<TextView>(R.id.photo_header)

        if (isDuressMode) {
            storagePath = "public_decoy_photos"
            header.text = "> GUEST_GALLERY"
            header.setTextColor(getColor(R.color.error_red))
        } else {
            storagePath = "secure_photos"
            header.text = "> ENCRYPTED_GALLERY"
        }

        setupRecyclerView()

        findViewById<FloatingActionButton>(R.id.fab_gallery).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<FloatingActionButton>(R.id.fab_camera).setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureLauncher.launch(intent)
        }

        // --- 3. FORCE CORRECT BACK BUTTON BEHAVIOR ---
        // --- FORCE WAKE THE VAULT ---
        // --- HARD RESET BACK BUTTON ---
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 1. Show a toast so you know the button actually worked
                Toast.makeText(this@PhotoVaultActivity, "Reloading Vault...", Toast.LENGTH_SHORT).show()

                // 2. Force a completely fresh start of VaultActivity
                val intent = Intent(this@PhotoVaultActivity, VaultActivity::class.java)
                intent.putExtra("DURESS_MODE", isDuressMode)
                // CLEAR_TASK wipes all old memory and forces a fresh start
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        })
    }

    // --- UPLOAD LOGIC ---
    private fun uploadFromGallery(uri: Uri) {
        Toast.makeText(this, "Encrypting...", Toast.LENGTH_SHORT).show()
        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        if (bytes != null) {
            processAndUpload(bytes)
        }
    }

    private fun uploadFromCamera(bitmap: Bitmap) {
        Toast.makeText(this, "Encrypting...", Toast.LENGTH_SHORT).show()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bytes = stream.toByteArray()

        processAndUpload(bytes)
    }

    private fun processAndUpload(data: ByteArray) {
        val encryptedPair = crypto.encryptToBytes(data)

        if (encryptedPair == null) {
            Toast.makeText(this, "Encryption Failed!", Toast.LENGTH_SHORT).show()
            return
        }

        val iv = encryptedPair.first
        val encryptedBytes = encryptedPair.second
        val filename = UUID.randomUUID().toString() + ".enc"

        val ref = storage.reference.child("$storagePath/$filename")

        ref.putBytes(encryptedBytes).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { uri ->
                val ivString = Base64.encodeToString(iv, Base64.DEFAULT)
                val photo = SecurePhoto(uri.toString(), ivString, filename, System.currentTimeMillis())

                db.collection(storagePath).add(photo)
                    .addOnSuccessListener {
                        Toast.makeText(this, "SECURE SAVE COMPLETE", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Upload Failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // --- DISPLAY LOGIC ---
    private fun setupRecyclerView() {
        val grid = findViewById<RecyclerView>(R.id.photo_grid)
        grid.layoutManager = GridLayoutManager(this, 3)

        val query = db.collection(storagePath)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<SecurePhoto>()
            .setQuery(query, SecurePhoto::class.java)
            .build()

        adapter = object : FirestoreRecyclerAdapter<SecurePhoto, PhotoViewHolder>(options) {
            override fun onBindViewHolder(holder: PhotoViewHolder, position: Int, model: SecurePhoto) {
                val ref = storage.getReferenceFromUrl(model.downloadUrl)

                ref.getBytes(5 * 1024 * 1024).addOnSuccessListener { result ->
                    val encryptedBytes = result as ByteArray
                    val iv = Base64.decode(model.iv, Base64.DEFAULT)
                    val decryptedBytes = crypto.decryptFromBytes(iv, encryptedBytes)

                    if (decryptedBytes != null && decryptedBytes.isNotEmpty()) {
                        val bmp = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
                        holder.image.setImageBitmap(bmp)
                    }
                }.addOnFailureListener {
                    holder.image.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_photo, parent, false)
                return PhotoViewHolder(view)
            }
        }
        grid.adapter = adapter
    }

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image_view)
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }
}