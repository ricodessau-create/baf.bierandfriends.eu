package baf.bierandfriends.eu.ui.tickets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import baf.bierandfriends.eu.databinding.ItemTicketMessageBinding

class TicketMessageAdapter(
    private val messages: List<Map<String, Any>>
) : RecyclerView.Adapter<TicketMessageAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTicketMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTicketMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.binding.msgAuthor.text = msg["authorName"] as? String ?: "Unbekannt"
        holder.binding.msgText.text = msg["text"] as? String ?: ""
    }

    override fun getItemCount() = messages.size
}
