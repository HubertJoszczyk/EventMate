package com.example.eventmate

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddEventActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private var categoriesList: List<Category> = emptyList()
    
    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var voiceNotePath: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    private lateinit var ivPhotoPreview: ImageView
    private lateinit var tvVoiceStatus: TextView
    
    private val calendar = Calendar.getInstance()

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.CAMERA] != true || permissions[Manifest.permission.RECORD_AUDIO] != true) {
            Toast.makeText(this, "Wymagane uprawnienia do multimediów", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            photoUri?.let { uri ->
                ivPhotoPreview.visibility = View.VISIBLE
                ivPhotoPreview.setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.add_event)
        
        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString("photoPath")
            voiceNotePath = savedInstanceState.getString("voicePath")
            val uriString = savedInstanceState.getString("photoUri")
            if (uriString != null) photoUri = Uri.parse(uriString)
        }

        db = AppDatabase.getDatabase(this)
        checkPermissions()

        val etTitle = findViewById<EditText>(R.id.EventTitle)
        val etLocation = findViewById<EditText>(R.id.EventLocation)
        val etDate = findViewById<EditText>(R.id.DayOfMeet)
        val etTime = findViewById<EditText>(R.id.TimeOfMeet)
        val spinnerCategory = findViewById<Spinner>(R.id.CategorySpinner)
        val btnConfirm = findViewById<Button>(R.id.confirm_event)
        val btnTakePhoto = findViewById<Button>(R.id.btnTakePhoto)
        val btnRecordVoice = findViewById<Button>(R.id.btnRecordVoice)
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview)
        tvVoiceStatus = findViewById(R.id.tvVoiceStatus)

        // Ustawienie początkowych wartości daty i godziny
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        etDate.setText(dateFormat.format(calendar.time))
        etTime.setText(timeFormat.format(calendar.time))

        // Blokujemy wpisywanie z klawiatury
        etDate.isFocusable = false
        etDate.isClickable = true
        etTime.isFocusable = false
        etTime.isClickable = true

        etDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                etDate.setText(dateFormat.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        etTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                etTime.setText(timeFormat.format(calendar.time))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        photoUri?.let {
            ivPhotoPreview.visibility = View.VISIBLE
            ivPhotoPreview.setImageURI(it)
        }
        if (voiceNotePath != null) tvVoiceStatus.visibility = View.VISIBLE

        lifecycleScope.launch {
            categoriesList = withContext(Dispatchers.IO) { db.categoryDao().getAll() }
            if (categoriesList.isEmpty()) {
                withContext(Dispatchers.IO) {
                    db.categoryDao().insert(Category(name = "Praca"))
                    db.categoryDao().insert(Category(name = "Rozrywka"))
                    db.categoryDao().insert(Category(name = "Inne"))
                    categoriesList = db.categoryDao().getAll()
                }
            }
            spinnerCategory.adapter = ArrayAdapter(this@AddEventActivity, android.R.layout.simple_spinner_item, categoriesList.map { it.name }).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        btnTakePhoto.setOnClickListener { dispatchTakePictureIntent() }
        btnRecordVoice.setOnClickListener {
            if (!isRecording) startRecording() else stopRecording()
            btnRecordVoice.text = if (isRecording) "Zatrzymaj" else "Nagraj"
        }

        btnConfirm.setOnClickListener {
            val title = etTitle.text.toString()
            val dateStr = etDate.text.toString()
            val timeStr = etTime.text.toString()

            if (title.isEmpty()) {
                Toast.makeText(this, "Podaj nazwę wydarzenia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Parsujemy tekst z pól, aby mieć pewność co do daty
            val timestamp = try {
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                sdf.parse("$dateStr $timeStr")?.time ?: calendar.timeInMillis
            } catch (e: Exception) {
                calendar.timeInMillis
            }

            val selectedCategoryId = if (categoriesList.isNotEmpty()) categoriesList[spinnerCategory.selectedItemPosition].categoryId else null

            val newEvent = Event(
                title = title,
                date = timestamp,
                location = etLocation.text.toString(),
                categoryId = selectedCategoryId,
                photoUri = currentPhotoPath,
                voiceNotePath = voiceNotePath
            )

            lifecycleScope.launch {
                withContext(Dispatchers.IO) { db.eventDao().insertEvent(newEvent) }
                Toast.makeText(this@AddEventActivity, "Event dodany!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("photoPath", currentPhotoPath)
        outState.putString("voicePath", voiceNotePath)
        outState.putString("photoUri", photoUri?.toString())
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun dispatchTakePictureIntent() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) { null }

        photoFile?.also {
            val authority = "${packageName}.fileprovider"
            photoUri = FileProvider.getUriForFile(this, authority, it)
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            takePhotoLauncher.launch(takePictureIntent)
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun startRecording() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val audioFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AUDIO_${timeStamp}.3gp")
        voiceNotePath = audioFile.absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(voiceNotePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
                start()
                isRecording = true
            } catch (e: IOException) { Log.e("AUDIO", "prepare() failed") }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        tvVoiceStatus.visibility = View.VISIBLE
    }
}