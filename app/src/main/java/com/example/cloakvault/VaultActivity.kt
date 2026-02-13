package com.example.cloakvault

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class VaultActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val cryptoManager = CryptoManager()
    private var adapter: FirestoreRecyclerAdapter<SecretNote, NoteViewHolder>? = null

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private var isDuressMode = false
    private var currentCollection = "secrets"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        val headerText = findViewById<TextView>(R.id.header)
        val menuIcon = findViewById<ImageView>(R.id.menu_icon)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add)

        isDuressMode = intent.getBooleanExtra("DURESS_MODE", false)

        if (isDuressMode) {
            currentCollection = "public_decoy"
            headerText.text = "> GUEST_MODE_ACTIVE"
            headerText.setTextColor(getColor(R.color.error_red))
            Toast.makeText(this, "⚠️ RESTRICTED ACCESS MODE", Toast.LENGTH_LONG).show()
        } else {
            headerText.text = "> SECURE_STORAGE_ACTIVE"
        }

        setupRecyclerView()

        menuIcon.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)

            when (menuItem.itemId) {
                R.id.nav_secrets -> { /* Do nothing */ }
                R.id.nav_photos -> {
                    val intent = Intent(this, PhotoVaultActivity::class.java)
                    intent.putExtra("DURESS_MODE", isDuressMode)
                    startActivity(intent)
                }
                R.id.nav_intruders -> {
                    if (isDuressMode) {
                        Toast.makeText(this, "ACCESS DENIED: ADMIN ONLY", Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(this, IntruderGalleryActivity::class.java)
                        startActivity(intent)
                    }
                }
                R.id.nav_logout -> {
                    secureLogout()
                }
            }
            true
        }

        fabAdd.setOnClickListener {
            showAddDialog()
        }

        // Inside VaultActivity.kt onCreate()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // Shows a message instead of instantly closing the app!
                    Toast.makeText(this@VaultActivity, "Use Sidebar to Lock System", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val query = db.collection(currentCollection).orderBy("timestamp", Query.Direction.DESCENDING)
        val options = FirestoreRecyclerOptions.Builder<SecretNote>().setQuery(query, SecretNote::class.java).build()

        adapter = object : FirestoreRecyclerAdapter<SecretNote, NoteViewHolder>(options) {
            override fun onBindViewHolder(holder: NoteViewHolder, position: Int, model: SecretNote) {
                holder.bind(model)
                holder.itemView.setOnClickListener {
                    val decryptedText = cryptoManager.decrypt(model.encryptedData)
                    if (decryptedText != null) {
                        showDecryptedDialog(model.title, decryptedText)
                    } else {
                        Toast.makeText(this@VaultActivity, "DECRYPTION ERROR", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
                view.setBackgroundColor(getColor(R.color.card_surface))
                return NoteViewHolder(view)
            }
        }
        recyclerView.adapter = adapter
    }

    private fun showAddDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (isDuressMode) "ADD PUBLIC NOTE" else "NEW SECRET ENTRY")
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val inputTitle = EditText(this).apply { hint = "TITLE" }
        val inputSecret = EditText(this).apply { hint = "SECRET DATA" }
        layout.addView(inputTitle)
        layout.addView(inputSecret)
        builder.setView(layout)
        builder.setPositiveButton("ENCRYPT & SAVE") { _, _ ->
            val title = inputTitle.text.toString()
            val secret = inputSecret.text.toString()
            if (title.isNotEmpty() && secret.isNotEmpty()) {
                val encrypted = cryptoManager.encrypt(secret)
                if (encrypted != null) {
                    val note = SecretNote(title, encrypted, System.currentTimeMillis())
                    db.collection(currentCollection).add(note)
                }
            }
        }
        builder.show()
    }

    private fun showDecryptedDialog(title: String, clearText: String) {
        AlertDialog.Builder(this)
            .setTitle("DECRYPTED: $title")
            .setMessage(clearText)
            .setPositiveButton("CLOSE", null)
            .show()
    }

    private fun secureLogout() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(android.R.id.text1)
        private val statusView: TextView = itemView.findViewById(android.R.id.text2)
        fun bind(note: SecretNote) {
            titleView.text = note.title
            titleView.setTextColor(itemView.resources.getColor(R.color.neon_cyan, null))
            titleView.typeface = Typeface.MONOSPACE
            statusView.text = "ENCRYPTED: AES-256 [LOCKED]"
            statusView.setTextColor(itemView.resources.getColor(R.color.text_dim, null))
        }
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