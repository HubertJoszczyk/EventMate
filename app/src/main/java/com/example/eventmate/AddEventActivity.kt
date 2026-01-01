package com.example.eventmate

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class AddEventActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private var categoriesList: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.add_event)
        
        db = AppDatabase.getDatabase(this)

        val rootLayout = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etTitle = findViewById<EditText>(R.id.EventTitle)
        val etLocation = findViewById<EditText>(R.id.EventLocation)
        val etDate = findViewById<EditText>(R.id.DayOfMeet)
        val etTime = findViewById<EditText>(R.id.TimeOfMeet)
        val spinnerCategory = findViewById<Spinner>(R.id.CategorySpinner)
        val btnConfirm = findViewById<Button>(R.id.confirm_event)

        // Załadowanie kategorii do Spinnera
        lifecycleScope.launch {
            categoriesList = withContext(Dispatchers.IO) {
                db.categoryDao().getAll()
            }
            
            // Jeśli baza jest pusta, dodaj przykładowe kategorie (dla testu)
            if (categoriesList.isEmpty()) {
                withContext(Dispatchers.IO) {
                    db.categoryDao().insert(Category(name = "Praca"))
                    db.categoryDao().insert(Category(name = "Rozrywka"))
                    db.categoryDao().insert(Category(name = "Inne"))
                    categoriesList = db.categoryDao().getAll()
                }
            }

            val adapter = ArrayAdapter(
                this@AddEventActivity,
                android.R.layout.simple_spinner_item,
                categoriesList.map { it.name }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter
        }

        btnConfirm.setOnClickListener {
            val title = etTitle.text.toString()
            val location = etLocation.text.toString()
            val dateStr = etDate.text.toString()
            val timeStr = etTime.text.toString()

            if (title.isEmpty()) {
                Toast.makeText(this, "Podaj nazwę wydarzenia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Parsowanie daty i czasu na Long
            val timestamp = try {
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = sdf.parse("$dateStr $timeStr")
                date?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            // Pobranie ID wybranej kategorii
            val selectedCategoryIndex = spinnerCategory.selectedItemPosition
            val selectedCategoryId = if (categoriesList.isNotEmpty()) {
                categoriesList[selectedCategoryIndex].categoryId
            } else null

            val newEvent = Event(
                title = title,
                date = timestamp,
                location = location,
                categoryId = selectedCategoryId
            )

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        db.eventDao().insertEvent(newEvent)
                    }
                    Toast.makeText(this@AddEventActivity, "Event dodany!", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Log.e("DB_TEST", "Błąd zapisu: ${e.message}")
                    Toast.makeText(this@AddEventActivity, "Błąd zapisu", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}