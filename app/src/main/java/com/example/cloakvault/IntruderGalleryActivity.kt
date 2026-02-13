package com.example.cloakvault

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class IntruderGalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_vault)

        // 1. UI CUSTOMIZATION
        val header = findViewById<TextView>(R.id.photo_header)
        header.text = "> INTRUDER_LOGS"
        header.setTextColor(getColor(R.color.error_red))

        // Hide Add Buttons
        findViewById<View>(R.id.fab_camera).visibility = View.GONE
        findViewById<View>(R.id.fab_gallery).visibility = View.GONE

        // 2. SETUP RECYCLER VIEW
        val grid = findViewById<RecyclerView>(R.id.photo_grid)
        grid.layoutManager = GridLayoutManager(this, 2)

        // 3. FETCH PHOTOS
        val storageRef = FirebaseStorage.getInstance().reference.child("intruders")

        Toast.makeText(this, "Scanning Logs...", Toast.LENGTH_SHORT).show()

        storageRef.listAll()
            .addOnSuccessListener { result ->
                val items = result.items
                if (items.isEmpty()) {
                    Toast.makeText(this, "✅ SYSTEM SECURE: No Intruders", Toast.LENGTH_SHORT).show()
                } else {
                    grid.adapter = IntruderAdapter(items)
                }
            }
            .addOnFailureListener { exception ->
                if (exception.message?.contains("exist", ignoreCase = true) == true) {
                    Toast.makeText(this, "✅ SYSTEM SECURE: No Intruders Yet", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }

        // --- 4. FORCE CORRECT BACK BUTTON BEHAVIOR ---
        // --- FORCE WAKE THE VAULT ---
        // --- HARD RESET BACK BUTTON ---
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@IntruderGalleryActivity, "Reloading Vault...", Toast.LENGTH_SHORT).show()

                val intent = Intent(this@IntruderGalleryActivity, VaultActivity::class.java)
                intent.putExtra("DURESS_MODE", false)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        })
    }

    // --- ADAPTER ---
    inner class IntruderAdapter(private val items: List<StorageReference>) :
        RecyclerView.Adapter<IntruderAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val img: ImageView = view.findViewById(R.id.image_view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            items[position].downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this@IntruderGalleryActivity)
                    .load(uri)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.stat_notify_error)
                    .into(holder.img)
            }.addOnFailureListener {
                holder.img.setImageResource(android.R.drawable.stat_notify_error)
            }
        }

        override fun getItemCount() = items.size
    }
}