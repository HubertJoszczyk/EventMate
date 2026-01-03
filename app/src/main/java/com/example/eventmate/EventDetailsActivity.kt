package com.example.eventmate

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventDetailsActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.event_details)
        
        db = AppDatabase.getDatabase(this)
        
        val eventId = intent.getIntExtra("EVENT_ID", -1)
        
        val tvTitle = findViewById<TextView>(R.id.tvDetailsTitle)
        val tvDate = findViewById<TextView>(R.id.tvDetailsDate)
        val tvLocation = findViewById<TextView>(R.id.tvDetailsLocation)
        val tvCategory = findViewById<TextView>(R.id.tvDetailsCategory)
        val ivPhoto = findViewById<ImageView>(R.id.ivDetailsPhoto)
        val layoutVoice = findViewById<LinearLayout>(R.id.layoutVoiceNote)
        val btnPlay = findViewById<Button>(R.id.btnPlayVoice)
        val btnBack = findViewById<Button>(R.id.btnBack)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnBack.setOnClickListener { finish() }

        if (eventId != -1) {
            lifecycleScope.launch {
                val event = withContext(Dispatchers.IO) { db.eventDao().getEventById(eventId) }
                
                event?.let { e ->
                    tvTitle.text = e.title
                    tvLocation.text = e.location
                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    tvDate.text = sdf.format(Date(e.date))
                    
                    // Obsługa zdjęcia
                    if (!e.photoUri.isNullOrEmpty()) {
                        val imgFile = File(e.photoUri)
                        if (imgFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                            ivPhoto.setImageBitmap(bitmap)
                            ivPhoto.visibility = View.VISIBLE
                        }
                    }

                    // Obsługa audio
                    if (!e.voiceNotePath.isNullOrEmpty()) {
                        layoutVoice.visibility = View.VISIBLE
                        btnPlay.setOnClickListener { playAudio(e.voiceNotePath) }
                    }

                    // Obsługa kategorii
                    if (e.categoryId != null) {
                        val category = withContext(Dispatchers.IO) {
                            db.categoryDao().getAll().find { it.categoryId == e.categoryId }
                        }
                        tvCategory.text = category?.name ?: "Brak"
                    } else {
                        tvCategory.text = "Brak"
                    }
                }
            }
        }
    }

    private fun playAudio(path: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
            }
            Toast.makeText(this, "Odtwarzanie...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Błąd odtwarzania", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}