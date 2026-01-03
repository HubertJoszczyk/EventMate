package com.example.eventmate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShowEventListActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var adapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.show_event_list)
        
        db = AppDatabase.getDatabase(this)
        
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewEvents)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Przekazujemy funkcję kliknięcia do adaptera
        adapter = EventAdapter { event ->
            val intent = Intent(this, EventDetailsActivity::class.java)
            intent.putExtra("EVENT_ID", event.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        val mainView = findViewById<View>(R.id.textViewTitle)
        ViewCompat.setOnApplyWindowInsetsListener(mainView.parent as View) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lifecycleScope.launch {
            db.eventDao().getAllEvents().collectLatest { events ->
                adapter.submitList(events)
            }
        }
    }

    class EventAdapter(private val onItemClick: (Event) -> Unit) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {
        private var eventsList: List<Event> = emptyList()

        fun submitList(newList: List<Event>) {
            eventsList = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
            return EventViewHolder(view, onItemClick)
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            holder.bind(eventsList[position])
        }

        override fun getItemCount(): Int = eventsList.size

        class EventViewHolder(itemView: View, private val onItemClick: (Event) -> Unit) : RecyclerView.ViewHolder(itemView) {
            private val tvTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
            private val tvDate: TextView = itemView.findViewById(R.id.tvEventDate)
            private val tvLocation: TextView = itemView.findViewById(R.id.tvEventLocation)

            fun bind(event: Event) {
                tvTitle.text = event.title
                tvLocation.text = event.location
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                tvDate.text = sdf.format(Date(event.date))
                
                itemView.setOnClickListener { onItemClick(event) }
            }
        }
    }
}