package baf.bierandfriends.eu.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import baf.bierandfriends.eu.data.models.Event
import baf.bierandfriends.eu.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventsAdapter(private val items: List<Event>) :
    RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    inner class EventViewHolder(val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = items[position]
        holder.binding.eventTitle.text = event.name
        holder.binding.eventDescription.text = event.description

        val dateText = event.date?.let {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
                .format(Date(it.seconds * 1000))
        } ?: "Kein Datum"
        holder.binding.eventDate.text = dateText
    }

    override fun getItemCount(): Int = items.size
    }
